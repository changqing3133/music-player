package com.example.musicplayer.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AudioFileTypesTest {

    @Test
    public void supportedExtensionsMatchIgnoringCase() {
        assertTrue(AudioFileTypes.isSupported("song.MP3", null));
        assertTrue(AudioFileTypes.isSupported("song.flac", "application/octet-stream"));
        assertTrue(AudioFileTypes.isSupported("a.b.ogg", null));
    }

    @Test
    public void unsupportedExtensionsAreRejected() {
        assertFalse(AudioFileTypes.isSupported("cover.jpg", "image/jpeg"));
        assertFalse(AudioFileTypes.isSupported("readme.txt", "text/plain"));
        assertFalse(AudioFileTypes.isSupported(null, null));
    }

    @Test
    public void missingExtensionFallsBackToMimeType() {
        assertTrue(AudioFileTypes.isSupported("recording", "audio/mpeg"));
        assertFalse(AudioFileTypes.isSupported("clip", "video/mp4"));
    }

    @Test
    public void stripAndReadExtension() {
        assertEquals("mp3", AudioFileTypes.extensionOf("Song.MP3"));
        assertEquals("", AudioFileTypes.extensionOf("noext"));
        assertEquals("", AudioFileTypes.extensionOf("trailing."));
        assertEquals("a.b", AudioFileTypes.stripExtension("a.b.mp3"));
        assertEquals("noext", AudioFileTypes.stripExtension("noext"));
    }
}
