package com.example.musicplayer.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.MediaItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShuffleHelperTest {

    @Test
    public void keepsCurrentItemAtIndex() {
        List<MediaItem> source = createItems(20);

        List<MediaItem> shuffled = ShuffleHelper.shuffleKeepingIndex(source, 7);

        assertEquals(20, shuffled.size());
        assertEquals(source.get(7).mediaId, shuffled.get(7).mediaId);
    }

    @Test
    public void keepsEveryItemExactlyOnce() {
        List<MediaItem> source = createItems(50);

        List<MediaItem> shuffled = ShuffleHelper.shuffleKeepingIndex(source, 0);

        assertEquals(idsOf(source), idsOf(shuffled));
    }

    @Test
    public void handlesEmptySingleAndInvalidIndex() {
        assertTrue(ShuffleHelper.shuffleKeepingIndex(new ArrayList<>(), 0).isEmpty());

        List<MediaItem> single = createItems(1);
        assertEquals("id0",
                ShuffleHelper.shuffleKeepingIndex(single, 0).get(0).mediaId);

        List<MediaItem> source = createItems(5);
        assertEquals("id0",
                ShuffleHelper.shuffleKeepingIndex(source, -3).get(0).mediaId);
        assertEquals("id0",
                ShuffleHelper.shuffleKeepingIndex(source, 99).get(0).mediaId);
    }

    @Test
    public void findsMediaId() {
        List<MediaItem> source = createItems(3);

        assertEquals(2, ShuffleHelper.indexOfMediaId(source, "id2"));
        assertEquals(-1, ShuffleHelper.indexOfMediaId(source, "missing"));
        assertEquals(-1, ShuffleHelper.indexOfMediaId(null, "id0"));
        assertEquals(-1, ShuffleHelper.indexOfMediaId(source, null));
    }

    private static List<MediaItem> createItems(int count) {
        List<MediaItem> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // 本地单元测试只使用 mediaId；setUri 会触发未 mock 的 android.net.Uri.parse
            items.add(new MediaItem.Builder()
                    .setMediaId("id" + i)
                    .build());
        }
        return items;
    }

    private static Set<String> idsOf(List<MediaItem> items) {
        Set<String> ids = new HashSet<>();
        for (MediaItem item : items) {
            ids.add(item.mediaId);
        }
        return ids;
    }
}
