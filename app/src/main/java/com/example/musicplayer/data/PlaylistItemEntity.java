package com.example.musicplayer.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 播放列表条目。track_uri 与 tracks.uri 对应，position 维护用户排序。
 */
@Entity(tableName = "playlist_items", indices = {@Index("playlist_id")})
public class PlaylistItemEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "playlist_id")
    public long playlistId;

    @NonNull
    @ColumnInfo(name = "track_uri")
    public String trackUri = "";

    public int position;
}
