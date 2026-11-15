package com.example.musicplayer.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 音频文件的扩展名 / MIME 判定规则，MediaStore 扫描与目录导入共用。
 */
final class AudioFileTypes {

    /** 必选格式：MP3 / AMR / AAC / OGG；额外兼容常见容器扩展名（含无损）。 */
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "aac", "m4a", "mp4", "amr", "awb", "ogg", "oga", "opus",
            "flac", "wav"
    ));

    private AudioFileTypes() {
    }

    static boolean isSupported(String displayName, String mimeType) {
        String extension = extensionOf(displayName);
        if (SUPPORTED_EXTENSIONS.contains(extension)) {
            return true;
        }
        // 无扩展名的文件交给 MIME 兜底
        return extension.isEmpty() && mimeType != null && mimeType.startsWith("audio/");
    }

    static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    static String stripExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
