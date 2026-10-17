package com.example.musicplayer.data;

import android.content.Context;
import android.util.Log;

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

    public interface Callback {
        /** 在 IO 线程回调，调用方需自行切回主线程。 */
        void onResult(int count, Throwable error);
    }

    public void refresh(Context context, Callback callback) {
        final Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            try {
                List<TrackEntity> tracks = MediaStoreScanner.scan(appContext);
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
}
