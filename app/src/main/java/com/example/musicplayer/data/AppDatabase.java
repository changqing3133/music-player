package com.example.musicplayer.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room 数据库。v1 包含三张表：
 * - tracks：本地曲库缓存（扫描结果）
 * - playlists / playlist_items：自定义播放列表（表结构已就绪，界面在后续迭代接入）
 */
@Database(
        entities = {TrackEntity.class, PlaylistEntity.class, PlaylistItemEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "music.db";
    private static volatile AppDatabase instance;

    public abstract TrackDao trackDao();

    public abstract PlaylistDao playlistDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DB_NAME
                    ).build();
                }
            }
        }
        return instance;
    }
}
