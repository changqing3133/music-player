package com.example.musicplayer.lyrics;

import android.content.Context;

import com.example.musicplayer.data.TrackEntity;
import com.example.musicplayer.model.LyricLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 歌词查找与解码。优先级：
 * 1. 应用专属目录 Android/data/<包名>/files/lyrics/（所有版本可读，推荐）
 * 2. 音频同目录同名 .lrc（Android 10 及以下通常有效；11+ 受分区存储限制）
 *
 * 文件名支持：<标题>.lrc、<标题> - <艺术家>.lrc（忽略大小写）。
 */
public final class LyricsRepository {

    private LyricsRepository() {
    }

    public static List<LyricLine> load(Context context, TrackEntity track, String title, String artist) {
        File lyricFile = findLyricFile(context, track, title, artist);
        if (lyricFile == null || !lyricFile.isFile() || !lyricFile.canRead()) {
            return Collections.emptyList();
        }
        try {
            return parse(readAllBytes(lyricFile));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** 解析 LRC 字节内容，自动处理 BOM 与常见中文编码。 */
    public static List<LyricLine> parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return Collections.emptyList();
        }
        return LrcParser.parse(decode(bytes));
    }

    private static File findLyricFile(Context context, TrackEntity track, String title, String artist) {
        List<String> baseNames = new ArrayList<>();
        if (title != null && !title.trim().isEmpty()) {
            String safeTitle = sanitize(title.trim());
            baseNames.add(safeTitle);
            if (artist != null && !artist.trim().isEmpty() && !"未知艺术家".equals(artist.trim())) {
                baseNames.add(safeTitle + " - " + sanitize(artist.trim()));
                baseNames.add(sanitize(title.trim() + " - " + artist.trim()));
            }
        }
        if (baseNames.isEmpty()) {
            return null;
        }

        File appLyricDir = context.getExternalFilesDir("lyrics");
        File found = findByBaseName(appLyricDir, baseNames);
        if (found != null) {
            return found;
        }

        // 尽力而为：音频同目录。Android 11+ 对非媒体文件的直接路径访问通常会被拒绝。
        String dataPath = track != null ? track.getDataPath() : null;
        if (dataPath != null) {
            File audioFile = new File(dataPath);
            File parent = audioFile.getParentFile();
            if (parent != null && parent.isDirectory()) {
                String audioBase = stripExtension(audioFile.getName());
                File sibling = new File(parent, audioBase + ".lrc");
                if (sibling.isFile() && sibling.canRead()) {
                    return sibling;
                }
                found = findByBaseName(parent, baseNames);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static File findByBaseName(File dir, List<String> baseNames) {
        if (dir == null || !dir.isDirectory()) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String name = file.getName();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".lrc")) {
                continue;
            }
            String base = name.substring(0, name.length() - 4);
            for (String candidate : baseNames) {
                if (base.equalsIgnoreCase(candidate)) {
                    return file;
                }
            }
        }
        return null;
    }

    private static String decode(byte[] bytes) {
        int offset = 0;
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            offset = 3;
        }
        String utf8 = tryDecode(bytes, offset, StandardCharsets.UTF_8);
        if (utf8 != null) {
            return utf8;
        }
        String gb18030 = tryDecode(bytes, offset, Charset.forName("GB18030"));
        if (gb18030 != null) {
            return gb18030;
        }
        return new String(bytes, offset, bytes.length - offset, StandardCharsets.ISO_8859_1);
    }

    private static String tryDecode(byte[] bytes, int offset, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static byte[] readAllBytes(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream((int) Math.max(1024L, file.length()))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static String stripExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
