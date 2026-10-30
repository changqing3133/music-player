package com.example.musicplayer.ui;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.example.musicplayer.MusicApp;
import com.example.musicplayer.R;
import com.example.musicplayer.databinding.ActivityLockLyricBinding;
import com.example.musicplayer.lyrics.LrcParser;
import com.example.musicplayer.lyrics.LyricsRepository;
import com.example.musicplayer.model.LyricLine;
import com.example.musicplayer.playback.PlaybackService;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 锁屏歌词页：屏幕熄灭时由 {@link PlaybackService} 拉起，覆盖在锁屏之上。
 * 上滑请求解锁，或点击右上角 × 仅关闭歌词页（保持锁定）。
 */
@OptIn(markerClass = UnstableApi.class)
public class LockLyricActivity extends AppCompatActivity {

    private static final long TICK_MS = 250L;

    private ActivityLockLyricBinding binding;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private MediaController controller;
    private ListenableFuture<MediaController> controllerFuture;
    private ExecutorService ioExecutor;

    private List<LyricLine> lyrics = Collections.emptyList();
    private String mediaKey;
    private int lineIndex = Integer.MIN_VALUE;
    private int lyricRequestId;
    private float swipeStartY;

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                finish();
            }
        }
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
            refreshTrack();
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            renderLine();
            mainHandler.postDelayed(this, TICK_MS);
        }
    };

    private static final String TAG = "LockLyricActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        showOverKeyguard();

        binding = ActivityLockLyricBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.lockClose.setOnClickListener(v -> finish());

        ioExecutor = ((MusicApp) getApplication()).getIoExecutor();

        ContextCompat.registerReceiver(this, unlockReceiver,
                new IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED);

        connectController();
    }

    private void showOverKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(false);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    /**
     * 歌词页覆盖在锁屏之上时，系统锁屏的“上滑解锁”手势会被页面挡住，
     * 因此这里自己识别上滑并请求系统解除锁屏（有密码时弹出验证）。
     */
    private void handleUnlockGesture(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                swipeStartY = event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                float delta = event.getRawY() - swipeStartY;
                if (delta < -dp(72)) {
                    requestUnlock();
                }
                break;
            default:
                break;
        }
    }

    private void requestUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this,
                        new KeyguardManager.KeyguardDismissCallback() {
                            @Override
                            public void onDismissSucceeded() {
                                finish();
                            }

                            @Override
                            public void onDismissError() {
                                finish();
                            }
                        });
                return;
            }
        }
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void connectController() {
        SessionToken token = new SessionToken(
                this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, token).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                controller = controllerFuture.get();
                Log.i(TAG, "controller connected");
                controller.addListener(playerListener);
                refreshTrack();
                mainHandler.post(ticker);
            } catch (Exception e) {
                Log.w(TAG, "controller connect failed", e);
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void refreshTrack() {
        if (binding == null) {
            return;
        }
        if (controller == null) {
            return;
        }
        MediaItem item = controller.getCurrentMediaItem();
        if (item == null) {
            Log.w(TAG, "no current media item, finishing");
            finish();
            return;
        }
        String key = item.mediaId;
        if (!Objects.equals(key, mediaKey)) {
            mediaKey = key;
            lineIndex = Integer.MIN_VALUE;
            updateTitle(item);
            loadLyrics(item);
        }
        renderLine();
    }

    private void updateTitle(MediaItem item) {
        MediaMetadata metadata = item.mediaMetadata;
        String title = metadata != null && metadata.title != null
                ? metadata.title.toString() : "";
        String artist = metadata != null && metadata.artist != null
                ? metadata.artist.toString() : "";
        if (title.isEmpty()) {
            binding.lockTitle.setText(R.string.app_name);
        } else if (artist.isEmpty()) {
            binding.lockTitle.setText(getString(R.string.lock_lyric_now_playing, title));
        } else {
            binding.lockTitle.setText(getString(R.string.lock_lyric_now_playing_artist, title, artist));
        }
    }

    private void loadLyrics(MediaItem item) {
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
                lineIndex = Integer.MIN_VALUE;
                renderLine();
            });
        });
    }

    private void renderLine() {
        if (binding == null || controller == null) {
            return;
        }
        if (lyrics.isEmpty()) {
            binding.lockPrevious.setText("");
            binding.lockCurrent.setText(R.string.lock_lyric_no_lyrics);
            binding.lockNext.setText("");
            lineIndex = Integer.MIN_VALUE;
            return;
        }

        long position = Math.max(0L, controller.getCurrentPosition());
        int index = LrcParser.indexAt(lyrics, position);
        if (index == lineIndex) {
            return;
        }
        lineIndex = index;

        String previous = "";
        String current;
        String next = "";
        if (index < 0) {
            current = "♪";
            next = lyrics.get(0).getText();
        } else {
            current = lyrics.get(index).getText();
            if (index > 0) {
                previous = lyrics.get(index - 1).getText();
            }
            if (index + 1 < lyrics.size()) {
                next = lyrics.get(index + 1).getText();
            }
        }
        binding.lockPrevious.setText(previous);
        binding.lockCurrent.setText(TextUtils.isEmpty(current) ? "♪" : current);
        binding.lockNext.setText(next);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 锁屏页只是歌词展示，旋转时保持当前实例
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(unlockReceiver);
        } catch (Exception ignored) {
            // 未注册成功时忽略
        }
        if (controller != null) {
            controller.removeListener(playerListener);
            controller.release();
            controller = null;
        }
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            controllerFuture = null;
        }
        if (!isChangingConfigurations()) {
            notifyService(PlaybackService.ACTION_LOCK_LYRIC_CLOSED);
        }
        binding = null;
        super.onDestroy();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        handleUnlockGesture(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (binding != null) {
            // 通知服务：锁屏页已经真正显示，悬浮歌词可以让位
            notifyService(PlaybackService.ACTION_LOCK_LYRIC_SHOWN);
        }
    }

    private void notifyService(String action) {
        try {
            startService(new Intent(this, PlaybackService.class).setAction(action));
        } catch (Exception ignored) {
            // 服务已销毁时无需再同步状态
        }
    }
}
