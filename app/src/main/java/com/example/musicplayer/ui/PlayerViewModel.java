package com.example.musicplayer.ui;

import android.app.Application;
import android.content.ComponentName;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.example.musicplayer.MusicApp;
import com.example.musicplayer.data.LibraryRepository;
import com.example.musicplayer.data.Prefs;
import com.example.musicplayer.data.TrackEntity;
import com.example.musicplayer.lyrics.LrcParser;
import com.example.musicplayer.lyrics.LyricsRepository;
import com.example.musicplayer.model.LyricLine;
import com.example.musicplayer.model.PlayerUiState;
import com.example.musicplayer.playback.PlaybackService;
import com.example.musicplayer.playback.ShuffleHelper;
import com.example.musicplayer.util.ArtworkLoader;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 播放页 / 曲库页共享的 ViewModel：
 * - 连接 PlaybackService（MediaController）
 * - 维护播放队列与随机重排
 * - 发布播放状态、歌词、封面
 * - 持久化上次播放位置
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerViewModel extends AndroidViewModel {

    private static final String TAG = "PlayerViewModel";

    private static final long TICK_INTERVAL_MS = 200L;
    private static final long POSITION_SAVE_THRESHOLD_MS = 5_000L;
    private static final long RESTART_THRESHOLD_MS = 3_000L;

    private final Application app;
    private final Prefs prefs;
    private final LibraryRepository libraryRepository;
    private final ArtworkLoader artworkLoader;
    private final ExecutorService ioExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<PlayerUiState> state =
            new MutableLiveData<>(PlayerUiState.empty());
    private final MutableLiveData<List<LyricLine>> lyrics =
            new MutableLiveData<List<LyricLine>>(Collections.emptyList());
    private final MutableLiveData<Bitmap> artwork =
            new MutableLiveData<Bitmap>();
    private final MutableLiveData<Boolean> scanning =
            new MutableLiveData<Boolean>(false);
    private final MutableLiveData<String> message =
            new MutableLiveData<String>();

    private final LiveData<List<TrackEntity>> library;

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;

    private List<TrackEntity> libraryTracks = Collections.emptyList();
    private List<MediaItem> queueSource = Collections.emptyList();
    private List<LyricLine> currentLyrics = Collections.emptyList();
    private String currentMediaKey;
    private int lyricRequestId;
    private long lastSavedPosition = -1L;
    private boolean queueInitialized;
    private boolean active;
    private Uri pendingExternalUri;

    private final Observer<List<TrackEntity>> libraryObserver = tracks -> {
        libraryTracks = tracks == null ? Collections.<TrackEntity>emptyList() : tracks;
        rebuildQueueSource();
        if (!queueInitialized && controller != null && controller.getMediaItemCount() == 0) {
            initializeQueue();
        }
        publishState();
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
            publishState();
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            if (mediaItem != null) {
                saveTrackStart(mediaItem);
            }
            publishState();
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Log.w(TAG, "播放出错，服务将自动跳过", error);
            publishState();
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (active) {
                publishState();
            }
            mainHandler.postDelayed(this, TICK_INTERVAL_MS);
        }
    };

    public PlayerViewModel(@NonNull Application application) {
        super(application);
        this.app = application;

        MusicApp musicApp = (MusicApp) application;
        prefs = musicApp.getPrefs();
        libraryRepository = musicApp.getLibraryRepository();
        artworkLoader = musicApp.getArtworkLoader();
        ioExecutor = musicApp.getIoExecutor();

        library = libraryRepository.observeLibrary();
        library.observeForever(libraryObserver);

        connectController();
        mainHandler.post(ticker);
    }

    private void connectController() {
        SessionToken token = new SessionToken(
                app, new ComponentName(app, PlaybackService.class));
        controllerFuture = new MediaController.Builder(app, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                controller.addListener(playerListener);
                // 随机播放由"队列重排"实现，引擎的 shuffle 始终保持关闭
                controller.setShuffleModeEnabled(false);
                controller.setRepeatMode(prefs.getRepeatMode());

                if (pendingExternalUri != null) {
                    Uri uri = pendingExternalUri;
                    pendingExternalUri = null;
                    playExternalUri(uri);
                    return;
                }

                if (controller.getMediaItemCount() > 0) {
                    queueInitialized = true;
                } else if (!queueInitialized) {
                    initializeQueue();
                }
                publishState();
            } catch (Exception e) {
                Log.e(TAG, "连接 PlaybackService 失败", e);
                message.setValue("无法连接播放服务");
            }
        }, ContextCompat.getMainExecutor(app));
    }

    private void initializeQueue() {
        if (controller == null || queueSource.isEmpty()) {
            return;
        }
        queueInitialized = true;

        int startIndex = 0;
        long startPosition = 0L;
        String lastUri = prefs.getLastTrackUri();
        if (lastUri != null) {
            int found = indexOfUri(queueSource, lastUri);
            if (found >= 0) {
                startIndex = found;
                startPosition = Math.max(0L, prefs.getLastPositionMs());
            }
        }

        List<MediaItem> queue = new ArrayList<>(queueSource);
        if (prefs.isShuffleEnabled()) {
            queue = ShuffleHelper.shuffleKeepingIndex(queue, startIndex);
        }

        controller.setMediaItems(queue, startIndex, startPosition);
        controller.setPlayWhenReady(false);
        controller.setRepeatMode(prefs.getRepeatMode());
        controller.prepare();
    }

    private void rebuildQueueSource() {
        List<MediaItem> items = new ArrayList<>(libraryTracks.size());
        for (TrackEntity track : libraryTracks) {
            items.add(toMediaItem(track));
        }
        queueSource = items;
    }

    private static MediaItem toMediaItem(TrackEntity track) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(track.getTitle())
                .setArtist(track.getArtist())
                .setAlbumTitle(track.getAlbum())
                .build();
        return new MediaItem.Builder()
                .setUri(track.getUri())
                .setMediaId(track.getUri())
                .setMediaMetadata(metadata)
                .build();
    }

    // ==================== 供 UI 调用的播放控制 ====================

    public void playTrack(@Nullable TrackEntity track) {
        if (controller == null || track == null) {
            return;
        }
        if (!queueInitialized) {
            rebuildQueueSource();
            initializeQueue();
        }

        int index = controllerIndexOfMediaId(track.getUri());
        if (index < 0) {
            // 队列中还没有这首歌（通常是扫描后新增），用最新曲库重建队列
            rebuildQueueSource();
            int sourceIndex = ShuffleHelper.indexOfMediaId(queueSource, track.getUri());
            if (sourceIndex < 0) {
                return;
            }
            List<MediaItem> queue = new ArrayList<>(queueSource);
            if (prefs.isShuffleEnabled()) {
                queue = ShuffleHelper.shuffleKeepingIndex(queue, sourceIndex);
            }
            controller.setMediaItems(queue, sourceIndex, 0L);
            controller.prepare();
            index = sourceIndex;
        }

        controller.seekTo(index, 0L);
        controller.setPlayWhenReady(true);
        if (controller.getPlaybackState() == Player.STATE_IDLE) {
            controller.prepare();
        }
        publishState();
    }

    /**
     * 播放文件管理器“打开方式”传入的音频 URI。
     * 外部文件不在 Room 曲库中，因此使用单曲队列。
     */
    public void playExternalUri(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        if (controller == null) {
            pendingExternalUri = uri;
            return;
        }

        String title = displayNameOf(uri);
        MediaItem item = new MediaItem.Builder()
                .setUri(uri)
                .setMediaId(uri.toString())
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(title).build())
                .build();

        queueSource = Collections.singletonList(item);
        queueInitialized = true;
        controller.setMediaItems(Collections.singletonList(item));
        controller.setPlayWhenReady(true);
        controller.prepare();
        publishState();
    }

    private String displayNameOf(Uri uri) {
        String name = null;
        try (Cursor cursor = app.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    name = cursor.getString(index);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "读取外部音频文件名失败", e);
        }
        if (name == null || name.trim().isEmpty()) {
            name = uri.getLastPathSegment();
        }
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    public void togglePlayPause() {
        if (controller == null || controller.getMediaItemCount() == 0) {
            return;
        }
        if (controller.getPlaybackState() == Player.STATE_ENDED) {
            controller.seekToDefaultPosition();
        }
        if (controller.getPlayWhenReady()) {
            controller.pause();
        } else {
            controller.play();
        }
        publishState();
    }

    /**
     * 下一首：手动操作优先于单曲循环；队尾顺序播放时暂停并回到本曲开头。
     */
    public void next() {
        if (controller == null) {
            return;
        }
        int count = controller.getMediaItemCount();
        if (count == 0) {
            return;
        }
        int current = controller.getCurrentMediaItemIndex();
        int repeat = controller.getRepeatMode();
        boolean playWhenReady = controller.getPlayWhenReady();

        int target;
        if (current < count - 1) {
            target = current + 1;
        } else if (repeat == Player.REPEAT_MODE_ALL || repeat == Player.REPEAT_MODE_ONE) {
            target = 0;
        } else {
            controller.pause();
            controller.seekTo(current, 0L);
            publishState();
            return;
        }

        controller.seekTo(target, 0L);
        controller.setPlayWhenReady(playWhenReady);
        if (controller.getPlaybackState() == Player.STATE_IDLE) {
            controller.prepare();
        }
        publishState();
    }

    /**
     * 上一首：播放超过 3 秒先回到本曲开头。
     */
    public void previous() {
        if (controller == null) {
            return;
        }
        int count = controller.getMediaItemCount();
        if (count == 0) {
            return;
        }
        if (controller.getCurrentPosition() > RESTART_THRESHOLD_MS) {
            controller.seekTo(0L);
            publishState();
            return;
        }

        int current = controller.getCurrentMediaItemIndex();
        int repeat = controller.getRepeatMode();
        boolean playWhenReady = controller.getPlayWhenReady();
        int target;
        if (current > 0) {
            target = current - 1;
        } else if (repeat == Player.REPEAT_MODE_ALL) {
            target = count - 1;
        } else {
            target = 0;
        }

        controller.seekTo(target, 0L);
        controller.setPlayWhenReady(playWhenReady);
        if (controller.getPlaybackState() == Player.STATE_IDLE) {
            controller.prepare();
        }
        publishState();
    }

    public void seekBy(long deltaMs) {
        if (controller == null) {
            return;
        }
        seekTo(controller.getCurrentPosition() + deltaMs);
    }

    public void seekTo(long positionMs) {
        if (controller == null) {
            return;
        }
        long target = Math.max(0L, positionMs);
        long duration = controller.getDuration();
        if (duration != C.TIME_UNSET && duration > 0L) {
            target = Math.min(target, duration);
        }
        controller.seekTo(target);
        publishState();
    }

    /**
     * 循环模式：顺序 -> 列表循环 -> 单曲循环 -> 顺序。
     */
    public void cycleRepeat() {
        if (controller == null) {
            return;
        }
        int current = controller.getRepeatMode();
        int next;
        if (current == Player.REPEAT_MODE_OFF) {
            next = Player.REPEAT_MODE_ALL;
        } else if (current == Player.REPEAT_MODE_ALL) {
            next = Player.REPEAT_MODE_ONE;
        } else {
            next = Player.REPEAT_MODE_OFF;
        }
        controller.setRepeatMode(next);
        prefs.setRepeatMode(next);
        publishState();
    }

    /**
     * 随机开关：重排队列，当前曲目留在原位置，播放位置保持不变。
     */
    public void toggleShuffle() {
        if (controller == null || queueSource.isEmpty()) {
            return;
        }
        boolean enable = !prefs.isShuffleEnabled();
        long position = controller.getCurrentPosition();
        int currentIndex = controller.getCurrentMediaItemIndex();
        MediaItem currentItem = controller.getCurrentMediaItem();
        boolean playWhenReady = controller.getPlayWhenReady();

        List<MediaItem> newQueue;
        int newIndex;
        if (enable) {
            newQueue = ShuffleHelper.shuffleKeepingIndex(new ArrayList<>(queueSource), currentIndex);
            newIndex = Math.max(0, Math.min(currentIndex, newQueue.size() - 1));
        } else {
            newQueue = new ArrayList<>(queueSource);
            newIndex = currentItem == null
                    ? 0
                    : ShuffleHelper.indexOfMediaId(newQueue, currentItem.mediaId);
            if (newIndex < 0) {
                newIndex = 0;
            }
        }

        controller.setMediaItems(newQueue, newIndex, position);
        controller.setPlayWhenReady(playWhenReady);
        controller.prepare();
        prefs.setShuffleEnabled(enable);
        publishState();
    }

    /** 将用户选择的 .lrc 复制到应用歌词目录，文件名与当前歌曲标题一致。 */
    public void importLyrics(@Nullable Uri source) {
        if (source == null) {
            return;
        }
        MediaItem item = controller == null ? null : controller.getCurrentMediaItem();
        if (item == null) {
            message.setValue("请先播放一首歌曲");
            return;
        }
        MediaMetadata metadata = item.mediaMetadata;
        String title = metadata != null && metadata.title != null
                ? metadata.title.toString().trim() : "";
        if (title.isEmpty()) {
            message.setValue("无法获取歌曲标题");
            return;
        }
        final String fileName = sanitizeFileName(title) + ".lrc";

        ioExecutor.execute(() -> {
            try (InputStream input = app.getContentResolver().openInputStream(source)) {
                if (input == null) {
                    throw new IOException("无法读取所选文件");
                }
                byte[] data = readAllBytes(input);
                if (LyricsRepository.parse(data).isEmpty()) {
                    mainHandler.post(() -> message.setValue("所选文件不是有效的 LRC 歌词"));
                    return;
                }

                File dir = app.getExternalFilesDir("lyrics");
                if (dir == null) {
                    throw new IOException("无法访问歌词目录");
                }
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new IOException("无法创建歌词目录");
                }
                try (FileOutputStream output = new FileOutputStream(new File(dir, fileName))) {
                    output.write(data);
                }

                mainHandler.post(() -> {
                    message.setValue("歌词已导入：" + fileName);
                    loadLyrics(item);
                });
            } catch (Exception e) {
                Log.w(TAG, "导入歌词失败", e);
                String reason = e.getMessage() == null ? "未知错误" : e.getMessage();
                mainHandler.post(() -> message.setValue("导入歌词失败：" + reason));
            }
        });
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            if (output.size() > 2 * 1024 * 1024) {
                throw new IOException("歌词文件超过 2 MB");
            }
        }
        return output.toByteArray();
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public void scanLibrary() {
        if (Boolean.TRUE.equals(scanning.getValue())) {
            return;
        }
        scanning.setValue(true);
        libraryRepository.refresh(app, (count, error) -> mainHandler.post(() -> {
            scanning.setValue(false);
            if (error != null) {
                message.setValue("扫描失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
            } else {
                message.setValue("已扫描 " + count + " 首歌曲");
                if (!queueInitialized) {
                    initializeQueue();
                }
            }
            publishState();
        }));
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            publishState();
        } else {
            savePlaybackSnapshot();
        }
    }

    // ==================== 状态发布 ====================

    private void publishState() {
        if (controller == null) {
            state.setValue(PlayerUiState.empty());
            return;
        }

        MediaItem item = controller.getCurrentMediaItem();
        String uri = uriOf(item);

        if (item == null) {
            if (currentMediaKey != null) {
                currentMediaKey = null;
                currentLyrics = Collections.emptyList();
                lyrics.setValue(currentLyrics);
                artwork.setValue(null);
            }
        } else if (uri != null && !uri.equals(currentMediaKey)) {
            currentMediaKey = uri;
            loadLyrics(item);
            loadArtwork(uri);
        }

        long position = controller.getCurrentPosition();
        long duration = controller.getDuration();
        if (duration == C.TIME_UNSET || duration < 0L) {
            duration = 0L;
        }

        MediaMetadata metadata = item == null ? null : item.mediaMetadata;
        String title = metadata != null && metadata.title != null ? metadata.title.toString() : "";
        String artist = metadata != null && metadata.artist != null ? metadata.artist.toString() : "";
        String album = metadata != null && metadata.albumTitle != null ? metadata.albumTitle.toString() : "";

        PlayerUiState snapshot = new PlayerUiState(
                controller.getPlaybackState(),
                controller.isPlaying(),
                controller.getPlayWhenReady(),
                position,
                duration,
                controller.getRepeatMode(),
                prefs.isShuffleEnabled(),
                item == null ? null : item.mediaId,
                uri,
                title,
                artist,
                album,
                LrcParser.indexAt(currentLyrics, position),
                !currentLyrics.isEmpty()
        );
        state.setValue(snapshot);

        if (uri != null) {
            maybeSavePosition(uri, position);
        }
    }

    private void loadLyrics(MediaItem item) {
        final int requestId = ++lyricRequestId;
        final MediaMetadata metadata = item.mediaMetadata;
        final String title = metadata != null && metadata.title != null ? metadata.title.toString() : "";
        final String artist = metadata != null && metadata.artist != null ? metadata.artist.toString() : "";
        final TrackEntity track = findTrack(item.mediaId);

        ioExecutor.execute(() -> {
            List<LyricLine> parsed = LyricsRepository.load(app, track, title, artist);
            mainHandler.post(() -> {
                if (requestId != lyricRequestId) {
                    return;
                }
                currentLyrics = parsed;
                lyrics.setValue(parsed);
                publishState();
            });
        });
    }

    private void loadArtwork(String uri) {
        final String expected = uri;
        artwork.setValue(null);
        artworkLoader.load(app, Uri.parse(uri), 512, bitmap -> {
            if (expected.equals(currentMediaKey)) {
                artwork.setValue(bitmap);
            }
        });
    }

    private TrackEntity findTrack(String uri) {
        if (uri == null) {
            return null;
        }
        for (TrackEntity track : libraryTracks) {
            if (uri.equals(track.getUri())) {
                return track;
            }
        }
        return null;
    }

    private void saveTrackStart(MediaItem item) {
        String uri = uriOf(item);
        if (uri != null) {
            prefs.setLastTrackUri(uri);
            prefs.setLastPositionMs(0L);
            lastSavedPosition = 0L;
        }
    }

    private void maybeSavePosition(String uri, long position) {
        if (lastSavedPosition < 0L
                || Math.abs(position - lastSavedPosition) >= POSITION_SAVE_THRESHOLD_MS) {
            prefs.setLastTrackUri(uri);
            prefs.setLastPositionMs(position);
            lastSavedPosition = position;
        }
    }

    private void savePlaybackSnapshot() {
        if (controller == null) {
            return;
        }
        MediaItem item = controller.getCurrentMediaItem();
        String uri = uriOf(item);
        if (uri != null) {
            prefs.setLastTrackUri(uri);
        }
        prefs.setLastPositionMs(controller.getCurrentPosition());
    }

    private static String uriOf(@Nullable MediaItem item) {
        if (item == null || item.localConfiguration == null
                || item.localConfiguration.uri == null) {
            return null;
        }
        return item.localConfiguration.uri.toString();
    }

    private int controllerIndexOfMediaId(String mediaId) {
        if (controller == null || mediaId == null) {
            return -1;
        }
        for (int i = 0; i < controller.getMediaItemCount(); i++) {
            MediaItem item = controller.getMediaItemAt(i);
            if (mediaId.equals(item.mediaId)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfUri(List<MediaItem> items, String uri) {
        if (uri == null) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            if (uri.equals(uriOf(items.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    // ==================== 暴露给 UI 的数据 ====================

    public LiveData<PlayerUiState> getState() {
        return state;
    }

    public LiveData<List<TrackEntity>> getLibrary() {
        return library;
    }

    public LiveData<List<LyricLine>> getLyrics() {
        return lyrics;
    }

    public LiveData<Bitmap> getArtwork() {
        return artwork;
    }

    public LiveData<Boolean> getScanning() {
        return scanning;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public void clearMessage() {
        message.setValue(null);
    }

    @Override
    protected void onCleared() {
        mainHandler.removeCallbacksAndMessages(null);
        savePlaybackSnapshot();
        library.removeObserver(libraryObserver);
        if (controller != null) {
            controller.removeListener(playerListener);
        }
        if (controllerFuture != null) {
            try {
                MediaController.releaseFuture(controllerFuture);
            } catch (Exception ignored) {
            }
        }
        controller = null;
    }
}
