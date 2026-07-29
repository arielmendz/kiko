package com.kiko.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class WakeWordMatcherTest {
    @Test
    public void matchesKikoIgnoringCaseAndPunctuation() {
        assertTrue(WakeWordMatcher.containsKiko(
                Collections.singletonList("¡KIKO!")
        ));
    }

    @Test
    public void matchesAnyRecognitionHypothesis() {
        assertTrue(WakeWordMatcher.containsKiko(
                Arrays.asList("chico", "oye kiko ven")
        ));
    }

    @Test
    public void matchesCommonSpanishTranscriptions() {
        assertTrue(WakeWordMatcher.containsKiko(
                Arrays.asList("oye Quico", "ven aquí")
        ));
        assertTrue(WakeWordMatcher.containsKiko(
                Collections.singletonList("quiko")
        ));
    }

    @Test
    public void rejectsSubstrings() {
        assertFalse(WakeWordMatcher.containsKiko(
                Arrays.asList("kikongo", "chikito")
        ));
    }

    @Test
    public void rejectsMissingOrNullResults() {
        assertFalse(WakeWordMatcher.containsKiko(Collections.emptyList()));
        assertFalse(WakeWordMatcher.containsKiko(null));
    }
}
