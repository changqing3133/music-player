package com.example.musicplayer.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 本地曲目缓存。uri 作为主键（content://media/external/audio/media/<id>）。
 */
@Entity(tableName = "tracks", indices = {@Index("media_id")})
public class TrackEntity {

    /** 曲目来源：MediaStore 全盘扫描。 */
    public static final int SOURCE_MEDIASTORE = 0;
    /** 曲目来源：用户通过“从目录导入”选择的文件夹。 */
    public static final int SOURCE_DIRECTORY = 1;

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uri")
    private String uri = "";

    @ColumnInfo(name = "media_id")
    private long mediaId;

    @NonNull
    @ColumnInfo(name = "title")
    private String title = "";

    @NonNull
    @ColumnInfo(name = "artist")
    private String artist = "";

    @NonNull
    @ColumnInfo(name = "album")
    private String album = "";

    @ColumnInfo(name = "album_id")
    private long albumId;

    @ColumnInfo(name = "duration_ms")
    private long durationMs;

    @NonNull
    @ColumnInfo(name = "mime_type")
    private String mimeType = "";

    @ColumnInfo(name = "size_bytes")
    private long sizeBytes;

    @ColumnInfo(name = "date_modified")
    private long dateModified;

    @NonNull
    @ColumnInfo(name = "display_name")
    private String displayName = "";

    /** 仅用于尽力查找同名 .lrc；Android 11+ 分区存储下不保证可读。 */
    @ColumnInfo(name = "data_path")
    private String dataPath;

    /** 曲目来源：0 = MediaStore 扫描，1 = 目录导入。全盘扫描不会删除目录导入的曲目。 */
    @ColumnInfo(name = "source", defaultValue = "0")
    private int source = SOURCE_MEDIASTORE;

    public TrackEntity() {
    }

    @NonNull
    public String getUri() {
        return uri;
    }

    public void setUri(@NonNull String uri) {
        this.uri = uri;
    }

    public long getMediaId() {
        return mediaId;
    }

    public void setMediaId(long mediaId) {
        this.mediaId = mediaId;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public String getArtist() {
        return artist;
    }

    public void setArtist(@NonNull String artist) {
        this.artist = artist;
    }

    @NonNull
    public String getAlbum() {
        return album;
    }

    public void setAlbum(@NonNull String album) {
        this.album = album;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(@NonNull String mimeType) {
        this.mimeType = mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public long getDateModified() {
        return dateModified;
    }

    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }
}
