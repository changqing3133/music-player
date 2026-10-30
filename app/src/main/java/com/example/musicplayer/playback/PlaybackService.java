package com.example.musicplayer.playback;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.musicplayer.MusicApp;
import com.example.musicplayer.data.Prefs;
import com.example.musicplayer.lyrics.LrcParser;
import com.example.musicplayer.lyrics.LyricsRepository;
import com.example.musicplayer.model.LyricLine;
import com.example.musicplayer.ui.LockLyricActivity;
import com.example.musicplayer.ui.MainActivity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 全应用唯一的播放器宿主：
 * - MediaSessionService 提供前台服务、媒体通知、锁屏/蓝牙/耳机线控
 * - ExoPlayer 只在这里创建，UI 全部通过 MediaController 操作
 * - 悬浮歌词与锁屏歌词在服务内跟随播放进度刷新
 */
@OptIn(markerClass = UnstableApi.class)
public class PlaybackService extends MediaSessionService {

    private static final String TAG = "PlaybackService";

    /** 连续解码失败超过该数量后停止，避免坏文件队列无限重试。 */
    private static final int MAX_CONSECUTIVE_SKIPS = 3;

    /** 媒体键“单击 / 双击”判定窗口；窗口内的第二次按键视为双击（下一首）。 */
    private static final long DOUBLE_TAP_WINDOW_MS = 350L;

    /** 与 PlayerViewModel 保持一致：播放超过 3 秒按“上一首”先回到本曲开头。 */
    private static final long RESTART_THRESHOLD_MS = 3_000L;

    private static final long LYRIC_TICK_MS = 250L;

    public static final String ACTION_SET_FLOATING_LYRICS =
            "com.example.musicplayer.action.SET_FLOATING_LYRICS";
    public static final String ACTION_SET_LOCK_LYRICS =
            "com.example.musicplayer.action.SET_LOCK_LYRICS";
    public static final String ACTION_RELOAD_LYRICS =
            "com.example.musicplayer.action.RELOAD_LYRICS";
    public static final String ACTION_LOCK_LYRIC_SHOWN =
            "com.example.musicplayer.action.LOCK_LYRIC_SHOWN";
    public static final String ACTION_LOCK_LYRIC_CLOSED =
            "com.example.musicplayer.action.LOCK_LYRIC_CLOSED";
    public static final String EXTRA_ENABLED = "enabled";

    private ExoPlayer player;
    private MediaSession mediaSession;
    private int consecutiveSkips;

    private Prefs prefs;
    private ExecutorService ioExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private LyricOverlayController overlay;
    private List<LyricLine> lyrics = Collections.emptyList();
    private int lyricRequestId;
    private int lastOverlayIndex = Integer.MIN_VALUE;
    private boolean lockActivityShowing;

    private long lastPlayPauseTapAt;
    private final Runnable singleTapAction = new Runnable() {
        @Override
        public void run() {
            lastPlayPauseTapAt = 0L;
            togglePlayPause();
        }
    };

    private final Runnable lyricTicker = new Runnable() {
        @Override
        public void run() {
            updateOverlayForPosition(false);
            mainHandler.postDelayed(this, LYRIC_TICK_MS);
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "screen receiver: " + action);
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                maybeShowLockLyrics();
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                // 锁屏歌词页面自己监听解锁事件，这里仅作为兜底清理状态
                lockActivityShowing = false;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "service created");

        MusicApp app = (MusicApp) getApplication();
        prefs = app.getPrefs();
        ioExecutor = app.getIoExecutor();
        overlay = new LyricOverlayController(this, () -> {
            prefs.setFloatingLyricsEnabled(false);
            stopLyricTicker();
        });

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .setSeekParameters(SeekParameters.EXACT)
                .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    consecutiveSkips = 0;
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                reloadLyrics();
                if (prefs.isFloatingLyricsEnabled() && !lockActivityShowing) {
                    showOverlay();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                skipBrokenItem();
            }
        });

        mediaSession = new MediaSession.Builder(this, player)
                .setSessionActivity(buildSessionActivity())
                .setCallback(new SessionCallback())
                .build();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        ContextCompat.registerReceiver(this, screenReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);

        if (prefs.isFloatingLyricsEnabled()
                && player.getMediaItemCount() > 0
                && player.getCurrentMediaItem() != null) {
            showOverlay();
        }
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    /**
     * 拦截自定义控制指令，避免 MediaSessionService 把未知 action 当作
     * “无法解析的启动意图”而立即停止服务。
     */
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && handleOwnAction(intent)) {
            return START_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private boolean handleOwnAction(@NonNull Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return false;
        }
        Log.i(TAG, "own action " + action + " enabled=" + intent.getBooleanExtra(EXTRA_ENABLED, false));
        switch (action) {
            case ACTION_SET_FLOATING_LYRICS:
                setFloatingLyrics(intent.getBooleanExtra(EXTRA_ENABLED, false));
                return true;
            case ACTION_SET_LOCK_LYRICS:
                prefs.setLockLyricsEnabled(intent.getBooleanExtra(EXTRA_ENABLED, false));
                return true;
            case ACTION_RELOAD_LYRICS:
                reloadLyrics();
                return true;
            case ACTION_LOCK_LYRIC_SHOWN:
                lockActivityShowing = true;
                overlay.hide();
                stopLyricTicker();
                return true;
            case ACTION_LOCK_LYRIC_CLOSED:
                lockActivityShowing = false;
                if (prefs.isFloatingLyricsEnabled()) {
                    showOverlay();
                }
                return true;
            default:
                return false;
        }
    }

    // ==================== 悬浮歌词 ====================

    private void setFloatingLyrics(boolean enabled) {
        prefs.setFloatingLyricsEnabled(enabled);
        if (!enabled) {
            stopLyricTicker();
            overlay.hide();
            return;
        }
        if (!overlay.canShow()) {
            prefs.setFloatingLyricsEnabled(false);
            return;
        }
        reloadLyrics();
        showOverlay();
    }

    private void showOverlay() {
        if (overlay == null || player == null || player.getCurrentMediaItem() == null) {
            return;
        }
        overlay.show(overlayTitle());
        lastOverlayIndex = Integer.MIN_VALUE;
        updateOverlayForPosition(true);
        startLyricTicker();
    }

    private void startLyricTicker() {
        mainHandler.removeCallbacks(lyricTicker);
        mainHandler.post(lyricTicker);
    }

    private void stopLyricTicker() {
        mainHandler.removeCallbacks(lyricTicker);
    }

    /**
     * @param force true 时忽略行号是否变化，强制刷新一次文本
     */
    private void updateOverlayForPosition(boolean force) {
        if (overlay == null || !overlay.isVisible() || player == null) {
            return;
        }
        overlay.setTitle(overlayTitle());

        if (lyrics.isEmpty()) {
            overlay.setLines(player.isPlaying() ? "♪ 正在播放" : "♪ 暂无歌词",
                    "把同名 .lrc 放入应用歌词目录，或在播放页导入");
            lastOverlayIndex = Integer.MIN_VALUE;
            return;
        }

        int index = LrcParser.indexAt(lyrics, player.getCurrentPosition());
        if (!force && index == lastOverlayIndex) {
            return;
        }
        lastOverlayIndex = index;

        String current;
        String next;
        if (index < 0) {
            current = "♪";
            next = lyrics.get(0).getText();
        } else {
            current = lyrics.get(index).getText();
            next = index + 1 < lyrics.size() ? lyrics.get(index + 1).getText() : "";
        }
        overlay.setLines(current, next);
    }

    private String overlayTitle() {
        if (player == null) {
            return "本地音乐";
        }
        MediaItem item = player.getCurrentMediaItem();
        MediaMetadata metadata = item == null ? null : item.mediaMetadata;
        String title = metadata != null && metadata.title != null
                ? metadata.title.toString() : "";
        String artist = metadata != null && metadata.artist != null
                ? metadata.artist.toString() : "";
        if (title.isEmpty()) {
            return "本地音乐";
        }
        return artist.isEmpty() ? title : "正在播放 · " + title + " - " + artist;
    }

    // ==================== 歌词加载 ====================

    private void reloadLyrics() {
        MediaItem item = player == null ? null : player.getCurrentMediaItem();
        if (item == null) {
            lyrics = Collections.emptyList();
            return;
        }
        MediaMetadata metadata = item.mediaMetadata;
        final String title = metadata != null && metadata.title != null
                ? metadata.title.toString() : "";
        final String artist = metadata != null && metadata.artist != null
                ? metadata.artist.toString() : "";
        final int requestId = ++lyricRequestId;

        ioExecutor.execute(() -> {
            List<LyricLine> parsed = LyricsRepository.loadByTitle(this, title, artist);
            mainHandler.post(() -> {
                if (requestId != lyricRequestId) {
                    return;
                }
                lyrics = parsed;
                lastOverlayIndex = Integer.MIN_VALUE;
                updateOverlayForPosition(true);
            });
        });
    }

    // ==================== 锁屏歌词 ====================

    private void maybeShowLockLyrics() {
        boolean enabled = prefs != null && prefs.isLockLyricsEnabled();
        boolean canOverlay = Settings.canDrawOverlays(this);
        boolean playing = player != null && player.isPlaying();
        int lyricCount = lyrics == null ? -1 : lyrics.size();
        Log.i(TAG, "maybeShowLockLyrics enabled=" + enabled
                + " canDrawOverlays=" + canOverlay
                + " playing=" + playing
                + " lyrics=" + lyricCount);
        if (!enabled || player == null) {
            return;
        }
        // 后台启动 Activity 的豁免依赖“悬浮窗”权限，未授权时宁可不弹
        if (!canOverlay) {
            return;
        }
        if (!playing || lyrics.isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, LockLyricActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(intent);
            Log.i(TAG, "lock lyric activity requested");
        } catch (Exception e) {
            Log.w(TAG, "无法显示锁屏歌词页面", e);
        }
    }

    // ==================== 媒体按键 ====================

    private final class SessionCallback implements MediaSession.Callback {
        @Override
        public boolean onMediaButtonEvent(MediaSession session,
                                          MediaSession.ControllerInfo controllerInfo,
                                          Intent intent) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return false;
            }
            int code = event.getKeyCode();
            boolean playPause = code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    || code == KeyEvent.KEYCODE_HEADSETHOOK;
            boolean next = code == KeyEvent.KEYCODE_MEDIA_NEXT;
            boolean previous = code == KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            if (!playPause && !next && !previous) {
                return false;
            }

            Log.d(TAG, "media button " + KeyEvent.keyCodeToString(code)
                    + " action=" + event.getAction() + " repeat=" + event.getRepeatCount());

            // 只处理按下沿，抬起沿一并消费，避免 Media3 再执行一次默认动作
            if (event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0) {
                return true;
            }
            if (playPause) {
                handlePlayPauseTap();
            } else if (next) {
                goNext();
            } else {
                goPrevious();
            }
            return true;
        }
    }

    /**
     * 蓝牙耳机/线控常见的单击 = 播放暂停，双击 = 下一首。
     * 双击判定依赖两个连续 PLAY_PAUSE 按键，因此单击动作延迟一个判定窗口执行。
     */
    private void handlePlayPauseTap() {
        long now = SystemClock.elapsedRealtime();
        if (lastPlayPauseTapAt != 0L && now - lastPlayPauseTapAt <= DOUBLE_TAP_WINDOW_MS) {
            lastPlayPauseTapAt = 0L;
            mainHandler.removeCallbacks(singleTapAction);
            goNext();
            return;
        }
        lastPlayPauseTapAt = now;
        mainHandler.postDelayed(singleTapAction, DOUBLE_TAP_WINDOW_MS);
    }

    private void togglePlayPause() {
        if (player == null) {
            return;
        }
        if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekToDefaultPosition();
        }
        if (player.getPlayWhenReady()) {
            player.pause();
        } else {
            player.play();
        }
    }

    /** 与 PlayerViewModel#next 行为一致：队尾顺序播放时暂停并回到本曲开头。 */
    private void goNext() {
        if (player == null) {
            return;
        }
        int count = player.getMediaItemCount();
        if (count == 0) {
            return;
        }
        int current = player.getCurrentMediaItemIndex();
        int repeat = player.getRepeatMode();
        int target;
        if (current < count - 1) {
            target = current + 1;
        } else if (repeat == Player.REPEAT_MODE_ALL || repeat == Player.REPEAT_MODE_ONE) {
            target = 0;
        } else {
            player.pause();
            player.seekTo(current, 0L);
            return;
        }
        player.seekTo(target, 0L);
        if (player.getPlaybackState() == Player.STATE_IDLE) {
            player.prepare();
        }
    }

    /** 与 PlayerViewModel#previous 行为一致：播放超过 3 秒先回到本曲开头。 */
    private void goPrevious() {
        if (player == null) {
            return;
        }
        int count = player.getMediaItemCount();
        if (count == 0) {
            return;
        }
        if (player.getCurrentPosition() > RESTART_THRESHOLD_MS) {
            player.seekTo(0L);
            return;
        }
        int current = player.getCurrentMediaItemIndex();
        int repeat = player.getRepeatMode();
        int target;
        if (current > 0) {
            target = current - 1;
        } else if (repeat == Player.REPEAT_MODE_ALL) {
            target = count - 1;
        } else {
            target = 0;
        }
        player.seekTo(target, 0L);
        if (player.getPlaybackState() == Player.STATE_IDLE) {
            player.prepare();
        }
    }

    private PendingIntent buildSessionActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(this, 0, intent, flags);
    }

    /**
     * 解码失败时自动跳到下一首（含单曲循环场景），连续失败达到上限后停止。
     */
    private void skipBrokenItem() {
        if (player == null || player.getMediaItemCount() == 0) {
            stopSelf();
            return;
        }
        if (++consecutiveSkips > MAX_CONSECUTIVE_SKIPS) {
            player.pause();
            player.stop();
            return;
        }
        int count = player.getMediaItemCount();
        int next = (player.getCurrentMediaItemIndex() + 1) % count;
        player.seekToDefaultPosition(next);
        player.prepare();
        player.play();
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        // 正在播放则保留服务；暂停/空闲状态下被划掉任务则结束服务。
        if (player == null
                || player.getPlaybackState() == Player.STATE_IDLE
                || !player.getPlayWhenReady()) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) {
            // 未注册成功时忽略
        }
        if (overlay != null) {
            overlay.hide();
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
