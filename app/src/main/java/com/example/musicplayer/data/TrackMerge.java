package com.example.musicplayer.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 目录导入结果与已有曲库的合并规则。
 *
 * <p>同一个文件可能既被 MediaStore 收录、又出现在用户选择的目录里，
 * 两者的 Uri 不同但“文件名 + 大小”相同，合并时只保留一条。</p>
 */
public final class TrackMerge {

    private TrackMerge() {
    }

    /**
     * 过滤掉与已有曲目重复的导入结果：
     * <ul>
     *   <li>Uri 已存在：保留，重新导入同一目录时刷新元数据；</li>
     *   <li>文件名与大小相同但 Uri 不同：跳过，说明这首歌已经由 MediaStore 收录；</li>
     *   <li>同一批导入内部的重复也只保留第一条。</li>
     * </ul>
     */
    public static List<TrackEntity> filterNew(List<TrackEntity> imported,
                                              List<TrackEntity> existing) {
        List<TrackEntity> safeImported = imported == null
                ? Collections.<TrackEntity>emptyList() : imported;
        List<TrackEntity> safeExisting = existing == null
                ? Collections.<TrackEntity>emptyList() : existing;

        Set<String> uris = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (TrackEntity track : safeExisting) {
            uris.add(track.getUri());
            keys.add(keyOf(track));
        }

        List<TrackEntity> result = new ArrayList<>();
        for (TrackEntity track : safeImported) {
            if (uris.contains(track.getUri())) {
                result.add(track);
                continue;
            }
            String key = keyOf(track);
            if (keys.contains(key)) {
                continue;
            }
            keys.add(key);
            result.add(track);
        }
        return result;
    }

    /**
     * 全盘扫描时跳过与“目录导入”重复的文件，避免同一首歌在曲库里出现两次。
     * 保留的是用户指定目录里的那条（已持久化读取授权）。
     */
    public static List<TrackEntity> withoutDirectoryDuplicates(List<TrackEntity> scanned,
                                                               List<TrackEntity> existing) {
        List<TrackEntity> safeScanned = scanned == null
                ? Collections.<TrackEntity>emptyList() : scanned;
        List<TrackEntity> safeExisting = existing == null
                ? Collections.<TrackEntity>emptyList() : existing;

        Set<String> directoryKeys = new HashSet<>();
        for (TrackEntity track : safeExisting) {
            if (track.getSource() == TrackEntity.SOURCE_DIRECTORY) {
                directoryKeys.add(keyOf(track));
            }
        }

        List<TrackEntity> result = new ArrayList<>();
        for (TrackEntity track : safeScanned) {
            if (!directoryKeys.contains(keyOf(track))) {
                result.add(track);
            }
        }
        return result;
    }

    private static String keyOf(TrackEntity track) {
        return track.getDisplayName() + "|" + track.getSizeBytes();
    }
}
