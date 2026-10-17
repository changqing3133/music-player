package com.example.musicplayer.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 自定义播放列表（表结构已就绪，供后续"收藏 / 自建列表"功能使用）。
 */
@Entity(tableName = "playlists")
public class PlaylistEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name = "";

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
