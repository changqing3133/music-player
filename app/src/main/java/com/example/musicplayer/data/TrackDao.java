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

    /**
     * 模糊搜索：标题、艺术家、专辑或文件名包含关键词即可。
     *
     * <p>pattern 由 {@link SearchQuery#likePattern(String)} 生成；输入里的 % 与 _
     * 已经转义，所以这里用 {@code ESCAPE '\'} 声明反斜杠为转义符。LIKE 对 ASCII
     * 字母不区分大小写，中文按原样匹配。</p>
     */
    @Query("SELECT * FROM tracks"
            + " WHERE title LIKE :pattern ESCAPE '\\'"
            + " OR artist LIKE :pattern ESCAPE '\\'"
            + " OR album LIKE :pattern ESCAPE '\\'"
            + " OR display_name LIKE :pattern ESCAPE '\\'"
            + " ORDER BY title COLLATE NOCASE ASC")
    LiveData<List<TrackEntity>> observeMatching(String pattern);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TrackEntity> tracks);

    @Query("DELETE FROM tracks")
    void deleteAll();

    /** 只删除 MediaStore 扫描的曲目，目录导入的曲目保持不变。 */
    @Query("DELETE FROM tracks WHERE source = 0")
    void deleteMediaStoreTracks();

    /**
     * 全量替换 MediaStore 扫描结果。目录导入的曲目由“从目录导入”单独维护，
     * 全盘扫描不会把它们清掉。
     */
    @Transaction
    default void replaceAll(List<TrackEntity> tracks) {
        deleteMediaStoreTracks();
        insertAll(tracks);
    }
}
