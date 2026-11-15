package com.example.musicplayer.data;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 扫描用户通过存储访问框架（SAF）选择的目录，把其中的音频文件转换成曲库条目。
 *
 * <p>目录里的文件不一定被 MediaStore 收录，因此直接读取文档 Uri，并用
 * {@link MediaMetadataRetriever} 补上标题、艺术家、专辑与时长。目录读取权限由
 * 调用方通过 {@code takePersistableUriPermission} 持久化，重启后仍可播放。</p>
 */
public final class DirectoryScanner {

    private static final String TAG = "DirectoryScanner";
    private static final long MIN_DURATION_MS = 10_000L;
    /** 防止误选存储根目录时扫描失控。 */
    private static final int MAX_DEPTH = 8;
    private static final int MAX_ENTRIES = 5_000;

    private static final String[] PROJECTION = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
    };

    private DirectoryScanner() {
    }

    public static List<TrackEntity> scan(Context context, Uri treeUri) {
        List<TrackEntity> result = new ArrayList<>();
        if (treeUri == null) {
            return result;
        }
        String rootId;
        try {
            rootId = DocumentsContract.getTreeDocumentId(treeUri);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "不是目录 Uri：" + treeUri, e);
            return result;
        }

        Deque<Node> pending = new ArrayDeque<>();
        pending.add(new Node(rootId, 0));
        int entries = 0;
        while (!pending.isEmpty()) {
            Node node = pending.poll();
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri, node.documentId);
            try (Cursor cursor = context.getContentResolver()
                    .query(children, PROJECTION, null, null, null)) {
                if (cursor == null) {
                    continue;
                }
                int idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                int nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
                int sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
                int dateIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);

                while (cursor.moveToNext()) {
                    if (++entries > MAX_ENTRIES) {
                        Log.w(TAG, "目录条目超过上限，已截断");
                        return result;
                    }
                    String documentId = getString(cursor, idIndex);
                    if (documentId.isEmpty()) {
                        continue;
                    }
                    String mimeType = getString(cursor, mimeIndex);
                    if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                        if (node.depth < MAX_DEPTH) {
                            pending.add(new Node(documentId, node.depth + 1));
                        }
                        continue;
                    }
                    String displayName = getString(cursor, nameIndex);
                    if (!AudioFileTypes.isSupported(displayName, mimeType)) {
                        continue;
                    }
                    long size = cursor.isNull(sizeIndex) ? 0L : cursor.getLong(sizeIndex);
                    long modified = cursor.isNull(dateIndex) ? 0L : cursor.getLong(dateIndex);
                    Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
                    TrackEntity track = readTrack(
                            context, documentUri, displayName, mimeType, size, modified);
                    if (track != null) {
                        result.add(track);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "读取目录失败：" + node.documentId, e);
            }
        }
        return result;
    }

    private static TrackEntity readTrack(Context context, Uri uri, String displayName,
                                         String mimeType, long size, long modified) {
        long duration = 0L;
        String title = "";
        String artist = "";
        String album = "";
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            duration = parseLong(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION));
            title = trimToEmpty(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            artist = trimToEmpty(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            album = trimToEmpty(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        } catch (Exception e) {
            Log.w(TAG, "读取音频信息失败：" + displayName, e);
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }

        if (duration > 0L && duration < MIN_DURATION_MS) {
            // 与 MediaStore 扫描保持一致，跳过提示音、铃声片段
            return null;
        }
        if (title.isEmpty()) {
            title = AudioFileTypes.stripExtension(displayName);
        }
        if (title.isEmpty()) {
            title = "未命名曲目";
        }

        TrackEntity track = new TrackEntity();
        track.setUri(uri.toString());
        track.setMediaId(0L);
        track.setTitle(title);
        track.setArtist(normalize(artist, "未知艺术家"));
        track.setAlbum(normalize(album, "未知专辑"));
        track.setAlbumId(0L);
        track.setDurationMs(duration);
        track.setMimeType(mimeType == null ? "" : mimeType);
        track.setSizeBytes(size);
        track.setDateModified(modified);
        track.setDisplayName(displayName);
        track.setDataPath(null);
        track.setSource(TrackEntity.SOURCE_DIRECTORY);
        return track;
    }

    private static String getString(Cursor cursor, int index) {
        if (index < 0 || cursor.isNull(index)) {
            return "";
        }
        String value = cursor.getString(index);
        return value == null ? "" : value;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value, String fallback) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isEmpty() || "<unknown>".equalsIgnoreCase(trimmed)) {
            return fallback;
        }
        return trimmed;
    }

    private static long parseLong(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 待扫描的目录节点，depth 用于限制递归层数。 */
    private static final class Node {
        final String documentId;
        final int depth;

        Node(String documentId, int depth) {
            this.documentId = documentId;
            this.depth = depth;
        }
    }
}
