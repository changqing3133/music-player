package com.example.musicplayer;

import android.app.Application;

import com.example.musicplayer.data.AppDatabase;
import com.example.musicplayer.data.LibraryRepository;
import com.example.musicplayer.data.Prefs;
import com.example.musicplayer.util.ArtworkLoader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用级单例入口：统一持有数据库、IO 线程池、封面加载器与偏好设置。
 * 播放器实例不在这里，它只属于 PlaybackService，避免出现两份播放器。
 */
public class MusicApp extends Application {

    private static MusicApp instance;

    private AppDatabase database;
    private ExecutorService ioExecutor;
    private ArtworkLoader artworkLoader;
    private Prefs prefs;
    private LibraryRepository libraryRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = AppDatabase.getInstance(this);
        ioExecutor = Executors.newFixedThreadPool(3);
        artworkLoader = new ArtworkLoader();
        prefs = new Prefs(this);
        libraryRepository = new LibraryRepository(database.trackDao(), ioExecutor);
    }

    public static MusicApp get() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public ExecutorService getIoExecutor() {
        return ioExecutor;
    }

    public ArtworkLoader getArtworkLoader() {
        return artworkLoader;
    }

    public Prefs getPrefs() {
        return prefs;
    }

    public LibraryRepository getLibraryRepository() {
        return libraryRepository;
    }
}
