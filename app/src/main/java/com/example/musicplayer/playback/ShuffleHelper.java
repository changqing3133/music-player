package com.example.musicplayer.playback;

import androidx.annotation.Nullable;
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

    /**
     * 计算一次"随机开关"后的队列与起始下标。
     *
     * <p>必须以当前曲目的 mediaId 定位，而不是沿用控制器里的下标：
     * 曲库重新扫描后 queueSource 的顺序可能已经变化，直接套用旧下标会切到别的歌。
     * 当前曲目不在曲库中时（例如从文件管理器打开的外部音频）把它追加到队尾，避免丢失。</p>
     */
    public static QueuePlan planToggle(@Nullable List<MediaItem> library,
                                       @Nullable MediaItem currentItem,
                                       boolean enableShuffle) {
        List<MediaItem> source = library == null
                ? new ArrayList<>() : new ArrayList<>(library);

        int keep;
        if (currentItem == null) {
            keep = 0;
        } else {
            keep = indexOfMediaId(source, currentItem.mediaId);
            if (keep < 0) {
                source.add(currentItem);
                keep = source.size() - 1;
            }
        }

        if (source.isEmpty()) {
            return new QueuePlan(source, 0);
        }
        List<MediaItem> queue = enableShuffle
                ? shuffleKeepingIndex(source, keep) : source;
        return new QueuePlan(queue, keep);
    }

    /** 一次随机开关的目标队列：items 为最终队列，startIndex 为当前曲目所在下标。 */
    public static final class QueuePlan {
        public final List<MediaItem> items;
        public final int startIndex;

        QueuePlan(List<MediaItem> items, int startIndex) {
            this.items = items;
            this.startIndex = startIndex;
        }
    }
}
