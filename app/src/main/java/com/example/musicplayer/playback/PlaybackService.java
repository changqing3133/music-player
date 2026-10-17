package com.example.musicplayer.playback;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.musicplayer.ui.MainActivity;

/**
 * 全应用唯一的播放器宿主：
 * - MediaSessionService 提供前台服务、媒体通知、锁屏/蓝牙/耳机线控
 * - ExoPlayer 只在这里创建，UI 全部通过 MediaController 操作
 */
@OptIn(markerClass = UnstableApi.class)
public class PlaybackService extends MediaSessionService {

    /** 连续解码失败超过该数量后停止，避免坏文件队列无限重试。 */
    private static final int MAX_CONSECUTIVE_SKIPS = 3;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private int consecutiveSkips;

    @Override
    public void onCreate() {
        super.onCreate();

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
            public void onPlayerError(@NonNull PlaybackException error) {
                skipBrokenItem();
            }
        });

        mediaSession = new MediaSession.Builder(this, player)
                .setSessionActivity(buildSessionActivity())
                .build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
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
