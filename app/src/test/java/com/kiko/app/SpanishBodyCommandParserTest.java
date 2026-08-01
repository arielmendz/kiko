package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class SpanishBodyCommandParserTest {
    @Test
    public void parsesBoundedStepFormsWithWakeVariants() {
        SpanishBodyCommandParser.Result words = SpanishBodyCommandParser.parse(
                "Kiko, da tres pasos"
        );
        SpanishBodyCommandParser.Result digits = SpanishBodyCommandParser.parse(
                "Quico camina 5 pasos"
        );

        assertEquals(BodyActionRequest.Type.MOVE_STEPS, words.getAction().getType());
        assertEquals(Integer.valueOf(3), words.getAction().getStepCount());
        assertEquals(Integer.valueOf(5), digits.getAction().getStepCount());
    }

    @Test
    public void parsesDanceAndExactEmergencyStop() {
        assertEquals(
                BodyActionRequest.Type.DANCE,
                SpanishBodyCommandParser.parse("haz un baile").getAction().getType()
        );
        assertTrue(SpanishBodyCommandParser.containsEmergencyStop(
                Arrays.asList("ruido", "detente")
        ));
        assertFalse(SpanishBodyCommandParser.containsEmergencyStop(
                Collections.singletonList("esto es para Pedro")
        ));
    }

    @Test
    public void requestsClarificationForMissingOrInvalidCount() {
        assertEquals(
                SpanishBodyCommandParser.Issue.MISSING_STEP_COUNT,
                SpanishBodyCommandParser.parse("Kiko, da pasos").getIssue()
        );
        assertEquals(
                SpanishBodyCommandParser.Issue.INVALID_STEP_COUNT,
                SpanishBodyCommandParser.parse("camina muchos pasos").getIssue()
        );
    }

    @Test
    public void ignoresUnrelatedSpeech() {
        assertNull(SpanishBodyCommandParser.parse("qué sabes de Pedro"));
        assertNull(SpanishBodyCommandParser.parse((String) null));
    }
}
