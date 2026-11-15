package com.example.musicplayer.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

/**
 * 通过 MediaStore 扫描本地音频。优先使用系统媒体索引，避免直接遍历外部存储
 * （Android 10+ 分区存储下路径访问不可靠）。
 */
public final class MediaStoreScanner {

    private static final long MIN_DURATION_MS = 10_000L;

    private MediaStoreScanner() {
    }

    public static List<TrackEntity> scan(Context context) {
        List<TrackEntity> result = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DATA
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
                + " AND " + MediaStore.Audio.Media.DURATION + " >= ?";
        String[] selectionArgs = {String.valueOf(MIN_DURATION_MS)};
        String sortOrder = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";

        try (Cursor cursor = resolver.query(collection, projection, selection, selectionArgs, sortOrder)) {
            if (cursor == null) {
                return result;
            }

            int idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
            int dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED);
            int nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

            while (cursor.moveToNext()) {
                String displayName = getString(cursor, nameIndex);
                String mimeType = getString(cursor, mimeIndex);
                if (!AudioFileTypes.isSupported(displayName, mimeType)) {
                    continue;
                }

                long mediaId = cursor.getLong(idIndex);
                String title = getString(cursor, titleIndex).trim();
                if (title.isEmpty()) {
                    title = AudioFileTypes.stripExtension(displayName);
                }
                if (title.isEmpty()) {
                    title = "未命名曲目";
                }

                String artist = normalize(getString(cursor, artistIndex), "未知艺术家");
                String album = normalize(getString(cursor, albumIndex), "未知专辑");
                long duration = cursor.isNull(durationIndex) ? 0L : cursor.getLong(durationIndex);
                long size = cursor.isNull(sizeIndex) ? 0L : cursor.getLong(sizeIndex);
                long albumId = cursor.isNull(albumIdIndex) ? 0L : cursor.getLong(albumIdIndex);
                long dateModified = cursor.isNull(dateIndex) ? 0L : cursor.getLong(dateIndex);
                String dataPath = cursor.isNull(dataIndex) ? null : cursor.getString(dataIndex);

                TrackEntity track = new TrackEntity();
                track.setUri(ContentUris.withAppendedId(collection, mediaId).toString());
                track.setMediaId(mediaId);
                track.setTitle(title);
                track.setArtist(artist);
                track.setAlbum(album);
                track.setAlbumId(albumId);
                track.setDurationMs(duration);
                track.setMimeType(mimeType);
                track.setSizeBytes(size);
                track.setDateModified(dateModified);
                track.setDisplayName(displayName);
                track.setDataPath(dataPath);
                result.add(track);
            }
        }
        // 查询失败（例如权限被撤销）会抛出异常，由 LibraryRepository 捕获并保留旧曲库，
        // 避免用空列表覆盖数据库。
        return result;
    }

    private static String getString(Cursor cursor, int index) {
        if (index < 0 || cursor.isNull(index)) {
            return "";
        }
        String value = cursor.getString(index);
        return value == null ? "" : value;
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "<unknown>".equalsIgnoreCase(trimmed)) {
            return fallback;
        }
        return trimmed;
    }
}
