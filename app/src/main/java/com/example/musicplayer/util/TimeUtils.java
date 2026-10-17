package com.example.musicplayer.util;

import java.util.Locale;

public final class TimeUtils {

    private TimeUtils() {
    }

    public static String format(long durationMs) {
        if (durationMs < 0) {
            durationMs = 0L;
        }
        long totalSeconds = durationMs / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
