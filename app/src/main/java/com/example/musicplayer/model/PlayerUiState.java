package com.example.musicplayer.model;

import androidx.media3.common.Player;

/**
 * UI 唯一的状态来源。每次发布都是新实例，避免 LiveData 收到可变对象。
 */
public final class PlayerUiState {

    public final int playbackState;
    public final boolean isPlaying;
    public final boolean playWhenReady;
    public final long positionMs;
    public final long durationMs;
    public final int repeatMode;
    public final boolean shuffleEnabled;
    public final String mediaId;
    public final String uri;
    public final String title;
    public final String artist;
    public final String album;
    public final int lyricLineIndex;
    public final boolean hasLyrics;

    public PlayerUiState(int playbackState,
                         boolean isPlaying,
                         boolean playWhenReady,
                         long positionMs,
                         long durationMs,
                         int repeatMode,
                         boolean shuffleEnabled,
                         String mediaId,
                         String uri,
                         String title,
                         String artist,
                         String album,
                         int lyricLineIndex,
                         boolean hasLyrics) {
        this.playbackState = playbackState;
        this.isPlaying = isPlaying;
        this.playWhenReady = playWhenReady;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        this.repeatMode = repeatMode;
        this.shuffleEnabled = shuffleEnabled;
        this.mediaId = mediaId;
        this.uri = uri;
        this.title = title == null ? "" : title;
        this.artist = artist == null ? "" : artist;
        this.album = album == null ? "" : album;
        this.lyricLineIndex = lyricLineIndex;
        this.hasLyrics = hasLyrics;
    }

    public static PlayerUiState empty() {
        return new PlayerUiState(
                Player.STATE_IDLE, false, false, 0L, 0L,
                Player.REPEAT_MODE_OFF, false,
                null, null, "", "", "", -1, false
        );
    }
}
