package com.example.musicplayer.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeUtilsTest {

    @Test
    public void formatsMinutesAndSeconds() {
        assertEquals("00:00", TimeUtils.format(0L));
        assertEquals("00:05", TimeUtils.format(5_999L));
        assertEquals("01:05", TimeUtils.format(65_000L));
        assertEquals("59:59", TimeUtils.format(3_599_000L));
    }

    @Test
    public void formatsHours() {
        assertEquals("1:01:01", TimeUtils.format(3_661_000L));
    }

    @Test
    public void clampsNegativeValues() {
        assertEquals("00:00", TimeUtils.format(-5_000L));
    }
}
