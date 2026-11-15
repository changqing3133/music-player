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

        // 集合相等 + 长度相等，才能同时排除"丢歌"和"重复"
        assertEquals(idsOf(source).size(), shuffled.size());
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

    // ==================== 随机开关：按 mediaId 定位当前曲目 ====================

    @Test
    public void enablingShuffleKeepsCurrentTrackById() {
        // 控制器队列可能是旧顺序（C, A, B, D），当前曲目是 id2：
        // 旧实现用控制器下标 0 去重排曲库，会把 id0 留在原位并切歌。
        List<MediaItem> library = createItems(4);
        MediaItem current = library.get(2);

        ShuffleHelper.QueuePlan plan = ShuffleHelper.planToggle(library, current, true);

        assertEquals("id2", plan.items.get(plan.startIndex).mediaId);
        assertEquals(2, plan.startIndex);
        assertEquals(idsOf(library), idsOf(plan.items));
        assertEquals(library.size(), plan.items.size());
    }

    @Test
    public void disablingShuffleRestoresLibraryOrderAtCurrentTrack() {
        List<MediaItem> library = createItems(5);
        MediaItem current = library.get(3);

        ShuffleHelper.QueuePlan plan = ShuffleHelper.planToggle(library, current, false);

        assertEquals(3, plan.startIndex);
        assertEquals(idsListOf(library), idsListOf(plan.items));
        assertEquals("id3", plan.items.get(3).mediaId);
    }

    @Test
    public void keepsCurrentTrackThatIsNoLongerInLibrary() {
        List<MediaItem> library = createItems(3);
        MediaItem removed = new MediaItem.Builder().setMediaId("removed").build();

        ShuffleHelper.QueuePlan plan = ShuffleHelper.planToggle(library, removed, true);

        assertEquals(4, plan.items.size());
        assertEquals(3, plan.startIndex);
        assertEquals("removed", plan.items.get(3).mediaId);
        assertEquals(1, idsListOf(plan.items).stream()
                .filter("removed"::equals).count());
    }

    @Test
    public void nullCurrentTrackFallsBackToFirstSlot() {
        List<MediaItem> library = createItems(4);

        ShuffleHelper.QueuePlan plan = ShuffleHelper.planToggle(library, null, true);

        assertEquals(0, plan.startIndex);
        assertEquals(idsOf(library), idsOf(plan.items));
    }

    @Test
    public void emptyLibraryKeepsPlayingItem() {
        MediaItem current = new MediaItem.Builder().setMediaId("only").build();

        ShuffleHelper.QueuePlan plan = ShuffleHelper.planToggle(
                new ArrayList<>(), current, true);

        assertEquals(1, plan.items.size());
        assertEquals(0, plan.startIndex);
        assertEquals("only", plan.items.get(0).mediaId);
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

    private static List<String> idsListOf(List<MediaItem> items) {
        List<String> ids = new ArrayList<>(items.size());
        for (MediaItem item : items) {
            ids.add(item.mediaId);
        }
        return ids;
    }
}
