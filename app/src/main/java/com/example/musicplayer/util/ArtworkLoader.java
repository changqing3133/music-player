package com.example.musicplayer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 内嵌封面加载器：MediaMetadataRetriever 读取 -> 降采样 -> LruCache。
 * 避免大图直接解码导致 OOM。
 */
public class ArtworkLoader {

    public interface Callback {
        void onLoaded(Bitmap bitmap);
    }

    private final LruCache<String, Bitmap> cache;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ArtworkLoader() {
        int maxKb = (int) (Runtime.getRuntime().maxMemory() / 1024L / 8L);
        cache = new LruCache<>(maxKb) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };
    }

    public void load(Context context, Uri uri, int maxSize, Callback callback) {
        if (uri == null) {
            deliver(callback, null);
            return;
        }
        String key = uri + "@" + maxSize;
        Bitmap cached = cache.get(key);
        if (cached != null) {
            deliver(callback, cached);
            return;
        }

        final Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            Bitmap bitmap = null;
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(appContext, uri);
                byte[] embedded = retriever.getEmbeddedPicture();
                if (embedded != null) {
                    bitmap = decodeSampled(embedded, maxSize);
                }
            } catch (Exception ignored) {
                // 没有内嵌封面或无法读取，保持 null，由 UI 显示占位图
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
            if (bitmap != null) {
                cache.put(key, bitmap);
            }
            final Bitmap result = bitmap;
            mainHandler.post(() -> callback.onLoaded(result));
        });
    }

    private void deliver(Callback callback, Bitmap bitmap) {
        mainHandler.post(() -> callback.onLoaded(bitmap));
    }

    private static Bitmap decodeSampled(byte[] bytes, int maxSize) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        int sample = 1;
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        while (longest / sample > maxSize) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }
}
