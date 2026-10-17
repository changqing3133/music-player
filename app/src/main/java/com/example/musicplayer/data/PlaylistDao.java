package com.example.musicplayer.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface PlaylistDao {

    @Insert
    long insertPlaylist(PlaylistEntity playlist);

    @Query("SELECT * FROM playlists ORDER BY created_at DESC")
    LiveData<List<PlaylistEntity>> observePlaylists();

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    PlaylistEntity findByName(String name);

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    void deletePlaylist(long playlistId);

    @Insert
    void insertItems(List<PlaylistItemEntity> items);

    @Query("DELETE FROM playlist_items WHERE playlist_id = :playlistId")
    void clearItems(long playlistId);

    @Query("SELECT t.* FROM tracks t " +
            "INNER JOIN playlist_items i ON t.uri = i.track_uri " +
            "WHERE i.playlist_id = :playlistId ORDER BY i.position ASC")
    LiveData<List<TrackEntity>> observeTracks(long playlistId);

    @Transaction
    default void replaceItems(long playlistId, List<PlaylistItemEntity> items) {
        clearItems(playlistId);
        insertItems(items);
    }
}
