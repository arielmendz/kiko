package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class SpeechLanguageSelectorTest {
    @Test
    public void prefersUnitedStatesSpanish() {
        assertEquals(
                "es-US",
                SpeechLanguageSelector.selectSpanish(Arrays.asList("es-ES", "es-US"))
        );
    }

    @Test
    public void fallsBackToAnyInstalledSpanishVariant() {
        assertEquals(
                "es-ES",
                SpeechLanguageSelector.selectSpanish(Arrays.asList("en-US", "es-ES"))
        );
    }

    @Test
    public void rejectsNonSpanishAndMissingLanguages() {
        assertNull(SpeechLanguageSelector.selectSpanish(
                Collections.singletonList("en-US")
        ));
        assertNull(SpeechLanguageSelector.selectSpanish(null));
    }
}
