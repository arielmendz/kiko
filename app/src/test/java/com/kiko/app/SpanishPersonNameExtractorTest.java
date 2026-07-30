package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class SpanishPersonNameExtractorTest {
    @Test
    public void extractsAndFormatsBoundedSpanishNames() {
        assertEquals(
                "María José",
                SpanishPersonNameExtractor.extract(
                        Collections.singletonList("se llama maría josé")
                )
        );
        assertEquals(
                "Ana",
                SpanishPersonNameExtractor.extract(
                        Collections.singletonList("ella es ana")
                )
        );
    }

    @Test
    public void triesAlternateHypotheses() {
        assertEquals(
                "Lucía",
                SpanishPersonNameExtractor.extract(Arrays.asList(
                        "persona 123",
                        "Lucía"
                ))
        );
    }

    @Test
    public void rejectsCommandsAndUnboundedContent() {
        assertTrue(SpanishPersonNameExtractor.containsCancel(
                Collections.singletonList("No lo sé")
        ));
        assertTrue(SpanishPersonNameExtractor.containsCancel(
                Collections.singletonList("No sé quién es")
        ));
        assertNull(SpanishPersonNameExtractor.extract(
                Collections.singletonList("cancelar")
        ));
        assertNull(SpanishPersonNameExtractor.extract(
                Collections.singletonList("Juan Carlos Pedro Cuarto")
        ));
        assertNull(SpanishPersonNameExtractor.extract(
                Collections.singletonList("Ana 7")
        ));
    }
}
