package com.kiko.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class SpanishCommandMatcherTest {
    @Test
    public void matchesQueVesIgnoringAccentsCaseAndPunctuation() {
        assertTrue(SpanishCommandMatcher.containsDescribeScene(
                Collections.singletonList("Kiko, ¿QUÉ VES?")
        ));
    }

    @Test
    public void matchesSupportedNaturalVariants() {
        assertTrue(SpanishCommandMatcher.containsDescribeScene(
                Arrays.asList("ruido", "dime qué puedes ver")
        ));
        assertTrue(SpanishCommandMatcher.containsDescribeScene(
                Collections.singletonList("qué estás viendo")
        ));
    }

    @Test
    public void rejectsSimilarButDifferentRequests() {
        assertFalse(SpanishCommandMatcher.containsDescribeScene(
                Arrays.asList("que viene", "a qué hora ves la película")
        ));
    }

    @Test
    public void rejectsMissingResults() {
        assertFalse(SpanishCommandMatcher.containsDescribeScene(
                Collections.emptyList()
        ));
        assertFalse(SpanishCommandMatcher.containsDescribeScene(null));
    }
}
