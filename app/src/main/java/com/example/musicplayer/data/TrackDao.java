package com.example.musicplayer.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface TrackDao {

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    LiveData<List<TrackEntity>> observeAll();

    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    List<TrackEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TrackEntity> tracks);

    @Query("DELETE FROM tracks")
    void deleteAll();

    /**
     * 全量替换。首次扫描与手动刷新都走这里；后续可在此基础上做增量指纹比对。
     */
    @Transaction
    default void replaceAll(List<TrackEntity> tracks) {
        deleteAll();
        insertAll(tracks);
    }
}
