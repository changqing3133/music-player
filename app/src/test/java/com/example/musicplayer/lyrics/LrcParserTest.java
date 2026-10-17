package com.example.musicplayer.lyrics;

import static org.junit.Assert.assertEquals;

import com.example.musicplayer.model.LyricLine;

import org.junit.Test;

import java.util.List;

public class LrcParserTest {

    @Test
    public void parsesBasicLines() {
        List<LyricLine> lines = LrcParser.parse("[00:01.00]第一句\n[00:12.50]第二句");

        assertEquals(2, lines.size());
        assertEquals(1000L, lines.get(0).getTimeMs());
        assertEquals("第一句", lines.get(0).getText());
        assertEquals(12500L, lines.get(1).getTimeMs());
        assertEquals("第二句", lines.get(1).getText());
    }

    @Test
    public void expandsMultipleTimeTags() {
        List<LyricLine> lines = LrcParser.parse("[00:05.00][00:30.00]重复歌词");

        assertEquals(2, lines.size());
        assertEquals(5000L, lines.get(0).getTimeMs());
        assertEquals(30000L, lines.get(1).getTimeMs());
        assertEquals("重复歌词", lines.get(0).getText());
        assertEquals("重复歌词", lines.get(1).getText());
    }

    @Test
    public void appliesOffsetWhereverTheTagIs() {
        List<LyricLine> lines = LrcParser.parse("[00:10.00]歌词\n[offset:+500]");

        assertEquals(1, lines.size());
        assertEquals(9500L, lines.get(0).getTimeMs());
    }

    @Test
    public void doesNotProduceNegativeTimestamps() {
        List<LyricLine> lines = LrcParser.parse("[00:00.10]歌词\n[offset:500]");

        assertEquals(1, lines.size());
        assertEquals(0L, lines.get(0).getTimeMs());
    }

    @Test
    public void skipsMetadataAndEmptyText() {
        List<LyricLine> lines = LrcParser.parse(
                "[ti:标题]\n[ar:歌手]\n[00:01.00]\n[00:02.00]有效");

        assertEquals(1, lines.size());
        assertEquals("有效", lines.get(0).getText());
    }

    @Test
    public void indexAtFindsLastLineAtOrBeforePosition() {
        List<LyricLine> lines = LrcParser.parse("[00:01.00]a\n[00:02.00]b\n[00:03.00]c");

        assertEquals(-1, LrcParser.indexAt(lines, 500L));
        assertEquals(0, LrcParser.indexAt(lines, 1000L));
        assertEquals(1, LrcParser.indexAt(lines, 2500L));
        assertEquals(2, LrcParser.indexAt(lines, 99999L));
        assertEquals(-1, LrcParser.indexAt(null, 1000L));
    }
}
