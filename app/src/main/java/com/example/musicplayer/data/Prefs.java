package com.example.musicplayer.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.media3.common.Player;

/**
 * 轻量偏好设置。Java 工程中不用 DataStore（其 API 面向协程），SharedPreferences 足够。
 */
public class Prefs {

    private static final String FILE = "music_prefs";

    private static final String KEY_REPEAT_MODE = "repeat_mode";
    private static final String KEY_SHUFFLE = "shuffle_enabled";
    private static final String KEY_LAST_URI = "last_track_uri";
    private static final String KEY_LAST_POSITION = "last_position_ms";
    private static final String KEY_AUDIO_PERMISSION_ASKED = "audio_permission_asked";
    private static final String KEY_NOTIFICATION_ASKED = "notification_asked";

    private final SharedPreferences prefs;

    public Prefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public int getRepeatMode() {
        return prefs.getInt(KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF);
    }

    public void setRepeatMode(int mode) {
        prefs.edit().putInt(KEY_REPEAT_MODE, mode).apply();
    }

    public boolean isShuffleEnabled() {
        return prefs.getBoolean(KEY_SHUFFLE, false);
    }

    public void setShuffleEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply();
    }

    public String getLastTrackUri() {
        return prefs.getString(KEY_LAST_URI, null);
    }

    public void setLastTrackUri(String uri) {
        prefs.edit().putString(KEY_LAST_URI, uri).apply();
    }

    public long getLastPositionMs() {
        return prefs.getLong(KEY_LAST_POSITION, 0L);
    }

    public void setLastPositionMs(long positionMs) {
        prefs.edit().putLong(KEY_LAST_POSITION, Math.max(0L, positionMs)).apply();
    }

    public boolean wasAudioPermissionAsked() {
        return prefs.getBoolean(KEY_AUDIO_PERMISSION_ASKED, false);
    }

    public void markAudioPermissionAsked() {
        prefs.edit().putBoolean(KEY_AUDIO_PERMISSION_ASKED, true).apply();
    }

    public boolean wasNotificationAsked() {
        return prefs.getBoolean(KEY_NOTIFICATION_ASKED, false);
    }

    public void markNotificationAsked() {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ASKED, true).apply();
    }
}
