package com.example.musicplayer.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TrackMergeTest {

    private static TrackEntity track(String uri, String displayName, long size) {
        TrackEntity entity = new TrackEntity();
        entity.setUri(uri);
        entity.setDisplayName(displayName);
        entity.setSizeBytes(size);
        return entity;
    }

    @Test
    public void mediaStoreCopyIsNotImportedTwice() {
        List<TrackEntity> existing = Collections.singletonList(
                track("content://media/1", "晴天.mp3", 1024L));
        List<TrackEntity> imported = Collections.singletonList(
                track("content://documents/1", "晴天.mp3", 1024L));

        List<TrackEntity> merged = TrackMerge.filterNew(imported, existing);

        assertEquals(0, merged.size());
    }

    @Test
    public void sameUriIsKeptToRefreshMetadata() {
        TrackEntity existingTrack = track("content://documents/1", "晴天.mp3", 1024L);
        TrackEntity importedTrack = track("content://documents/1", "晴天.mp3", 1024L);

        List<TrackEntity> merged = TrackMerge.filterNew(
                Collections.singletonList(importedTrack),
                Collections.singletonList(existingTrack));

        assertEquals(1, merged.size());
        assertEquals(importedTrack, merged.get(0));
    }

    @Test
    public void newFilesAreAppended() {
        List<TrackEntity> existing = Collections.singletonList(
                track("content://media/1", "晴天.mp3", 1024L));
        List<TrackEntity> imported = Arrays.asList(
                track("content://documents/1", "晴天.mp3", 1024L),
                track("content://documents/2", "稻香.mp3", 2048L));

        List<TrackEntity> merged = TrackMerge.filterNew(imported, existing);

        assertEquals(1, merged.size());
        assertEquals("content://documents/2", merged.get(0).getUri());
    }

    @Test
    public void duplicatesInsideImportAreDropped() {
        TrackEntity first = track("content://documents/1", "稻香.mp3", 2048L);
        TrackEntity second = track("content://documents/2", "稻香.mp3", 2048L);

        List<TrackEntity> merged = TrackMerge.filterNew(
                Arrays.asList(first, second), Collections.<TrackEntity>emptyList());

        assertEquals(1, merged.size());
        assertEquals(first, merged.get(0));
    }

    @Test
    public void nullCollectionsAreTolerated() {
        assertEquals(0, TrackMerge.filterNew(null, null).size());
        assertEquals(1, TrackMerge.filterNew(
                Collections.singletonList(track("u", "a.mp3", 1L)), null).size());
    }

    @Test
    public void fullScanSkipsFilesImportedFromDirectory() {
        TrackEntity imported = track("content://documents/1", "晴天.mp3", 1024L);
        imported.setSource(TrackEntity.SOURCE_DIRECTORY);
        TrackEntity scannedCopy = track("content://media/1", "晴天.mp3", 1024L);
        TrackEntity other = track("content://media/2", "稻香.mp3", 2048L);

        List<TrackEntity> result = TrackMerge.withoutDirectoryDuplicates(
                Arrays.asList(scannedCopy, other), Collections.singletonList(imported));

        assertEquals(1, result.size());
        assertEquals("content://media/2", result.get(0).getUri());
    }

    @Test
    public void fullScanComparesOnlyAgainstDirectoryRows() {
        TrackEntity mediaStoreRow = track("content://media/1", "晴天.mp3", 1024L);
        TrackEntity scannedCopy = track("content://media/9", "晴天.mp3", 1024L);

        List<TrackEntity> result = TrackMerge.withoutDirectoryDuplicates(
                Collections.singletonList(scannedCopy),
                Collections.singletonList(mediaStoreRow));

        assertEquals(1, result.size());
    }
}
