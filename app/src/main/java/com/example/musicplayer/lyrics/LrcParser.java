package com.example.musicplayer.lyrics;

import com.example.musicplayer.model.LyricLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LRC 解析器。支持：
 * - 一行多个时间标签（[00:12.00][01:20.50]同一句）
 * - 毫秒 / 厘秒精度（.00 / .000）
 * - 元数据行（[ti:]、[ar:] 等自动忽略）
 * - [offset:+500]（正值表示歌词整体提前，显示时间 = 标签时间 - offset）
 */
public final class LrcParser {

    private static final Pattern TIME_TAG =
            Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]");
    private static final Pattern OFFSET_TAG =
            Pattern.compile("\\[offset:\\s*([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE);

    private LrcParser() {
    }

    public static List<LyricLine> parse(String raw) {
        List<LyricLine> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        String content = raw.startsWith("\uFEFF") ? raw.substring(1) : raw;
        long offsetMs = findOffset(content);

        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            Matcher matcher = TIME_TAG.matcher(line);
            List<Long> times = new ArrayList<>();
            int textStart = -1;
            while (matcher.find()) {
                long minutes = parseLong(matcher.group(1));
                long seconds = parseLong(matcher.group(2));
                long fraction = parseFraction(matcher.group(3));
                times.add(minutes * 60_000L + seconds * 1_000L + fraction);
                textStart = matcher.end();
            }
            if (times.isEmpty() || textStart < 0) {
                continue;
            }

            String text = line.substring(textStart).trim();
            if (text.isEmpty()) {
                continue;
            }
            for (long time : times) {
                result.add(new LyricLine(Math.max(0L, time - offsetMs), text));
            }
        }

        Collections.sort(result, Comparator.comparingLong(LyricLine::getTimeMs));
        return result;
    }

    /**
     * 返回最后一条 timeMs <= positionMs 的行号；尚未到第一句时返回 -1。
     */
    public static int indexAt(List<LyricLine> lines, long positionMs) {
        if (lines == null || lines.isEmpty()) {
            return -1;
        }
        int low = 0;
        int high = lines.size() - 1;
        int answer = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (lines.get(mid).getTimeMs() <= positionMs) {
                answer = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return answer;
    }

    private static long findOffset(String content) {
        Matcher matcher = OFFSET_TAG.matcher(content);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static long parseFraction(String value) {
        if (value == null || value.isEmpty()) {
            return 0L;
        }
        long parsed = parseLong(value);
        if (value.length() == 1) {
            return parsed * 100L;
        }
        if (value.length() == 2) {
            return parsed * 10L;
        }
        return parsed;
    }
}
