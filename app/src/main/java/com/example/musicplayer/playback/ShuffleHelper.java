package com.example.musicplayer.playback;

import androidx.media3.common.MediaItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 随机播放通过在队列层重排实现，而不是启用 ExoPlayer 的 shuffleMode。
 * 这样"下一首/上一首"始终是队列下标 +1/-1，单曲循环下手动切歌行为也保持一致。
 */
public final class ShuffleHelper {

    private ShuffleHelper() {
    }

    /**
     * 打乱除 currentIndex 之外的所有位置，当前曲目保持在原位置，播放不中断。
     */
    public static List<MediaItem> shuffleKeepingIndex(List<MediaItem> items, int currentIndex) {
        List<MediaItem> result = new ArrayList<>(items);
        if (result.size() <= 1) {
            return result;
        }
        int keep = (currentIndex >= 0 && currentIndex < result.size()) ? currentIndex : 0;
        List<Integer> others = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i != keep) {
                others.add(i);
            }
        }
        Collections.shuffle(others);
        MediaItem keptItem = items.get(keep);
        int cursor = 0;
        for (int i = 0; i < result.size(); i++) {
            if (i == keep) {
                result.set(i, keptItem);
            } else {
                result.set(i, items.get(others.get(cursor++)));
            }
        }
        return result;
    }

    public static int indexOfMediaId(List<MediaItem> items, String mediaId) {
        if (items == null || mediaId == null) {
            return -1;
        }
        for (int i = 0; i < items.size(); i++) {
            if (mediaId.equals(items.get(i).mediaId)) {
                return i;
            }
        }
        return -1;
    }
}
