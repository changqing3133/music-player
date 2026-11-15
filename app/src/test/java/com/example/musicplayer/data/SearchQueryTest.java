package com.example.musicplayer.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SearchQueryTest {

    @Test
    public void emptyInputMatchesEverything() {
        assertEquals("%", SearchQuery.likePattern(null));
        assertEquals("%", SearchQuery.likePattern(""));
        assertEquals("%", SearchQuery.likePattern("   "));
    }

    @Test
    public void plainKeywordIsSurroundedByWildcards() {
        assertEquals("%周杰伦%", SearchQuery.likePattern("周杰伦"));
        assertEquals("%晴天%", SearchQuery.likePattern("  晴天  "));
        assertEquals("%hello%", SearchQuery.likePattern("hello"));
    }

    @Test
    public void underscoreIsEscaped() {
        assertEquals("%a\\_b%", SearchQuery.likePattern("a_b"));
        assertEquals("%a\\_b\\_c%", SearchQuery.likePattern("a_b_c"));
    }

    @Test
    public void percentIsEscaped() {
        assertEquals("%100\\%%", SearchQuery.likePattern("100%"));
    }

    @Test
    public void backslashIsEscaped() {
        assertEquals("%a\\\\b%", SearchQuery.likePattern("a\\b"));
    }
}
