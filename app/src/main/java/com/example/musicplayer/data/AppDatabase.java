package com.example.musicplayer.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Room 数据库。v2 包含三张表：
 * - tracks：本地曲库缓存（MediaStore 扫描 + 用户目录导入）
 * - playlists / playlist_items：自定义播放列表（表结构已就绪，界面在后续迭代接入）
 *
 * <p>v1 → v2：tracks 增加 source 列，用来区分 MediaStore 扫描与目录导入，
 * 全盘扫描时只替换前者，避免把导入的歌曲清掉。</p>
 */
@Database(
        entities = {TrackEntity.class, PlaylistEntity.class, PlaylistItemEntity.class},
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "music.db";
    private static volatile AppDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE tracks ADD COLUMN source INTEGER NOT NULL DEFAULT 0");
        }
    };

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
                    ).addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return instance;
    }
}
