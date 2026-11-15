package com.example.musicplayer.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 曲库仓库：MediaStore 扫描结果写入 Room，UI 通过 LiveData 观察。
 */
public class LibraryRepository {

    private static final String TAG = "LibraryRepository";

    private final TrackDao trackDao;
    private final ExecutorService executor;

    public LibraryRepository(TrackDao trackDao, ExecutorService executor) {
        this.trackDao = trackDao;
        this.executor = executor;
    }

    public LiveData<List<TrackEntity>> observeLibrary() {
        return trackDao.observeAll();
    }

    /**
     * 按关键词模糊搜索曲库；关键词为空时返回全部曲目。
     * 返回的 LiveData 会随 Room 表变化自动刷新，重新扫描后无需手动重查。
     */
    public LiveData<List<TrackEntity>> search(@Nullable String query) {
        return trackDao.observeMatching(SearchQuery.likePattern(query));
    }

    public interface Callback {
        /** 在 IO 线程回调，调用方需自行切回主线程。 */
        void onResult(int count, Throwable error);
    }

    public void refresh(Context context, Callback callback) {
        final Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            try {
                List<TrackEntity> tracks = MediaStoreScanner.scan(appContext);
                // 与目录导入重叠的文件只保留一份，避免同一首歌出现两次
                tracks = TrackMerge.withoutDirectoryDuplicates(tracks, trackDao.getAll());
                trackDao.replaceAll(tracks);
                if (callback != null) {
                    callback.onResult(tracks.size(), null);
                }
            } catch (Throwable t) {
                Log.e(TAG, "扫描曲库失败", t);
                if (callback != null) {
                    callback.onResult(0, t);
                }
            }
        });
    }

    /**
     * 导入用户选择的目录：递归读取其中的音频文件，去重后写入曲库。
     * 回调参数为实际新增（或刷新）的曲目数。
     */
    public void importDirectory(Context context, Uri treeUri, Callback callback) {
        final Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            try {
                List<TrackEntity> scanned = DirectoryScanner.scan(appContext, treeUri);
                List<TrackEntity> toInsert = TrackMerge.filterNew(scanned, trackDao.getAll());
                if (!toInsert.isEmpty()) {
                    trackDao.insertAll(toInsert);
                }
                if (callback != null) {
                    callback.onResult(toInsert.size(), null);
                }
            } catch (Throwable t) {
                Log.e(TAG, "导入目录失败", t);
                if (callback != null) {
                    callback.onResult(0, t);
                }
            }
        });
    }
}
