package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class SpanishNumberParserTest {
    @Test
    public void parsesDigitsAndSpanishWords() {
        assertEquals(Integer.valueOf(3), SpanishNumberParser.parse("3"));
        assertEquals(Integer.valueOf(3), SpanishNumberParser.parse("tres"));
        assertEquals(Integer.valueOf(1), SpanishNumberParser.parse("un"));
        assertEquals(Integer.valueOf(10), SpanishNumberParser.parse("diez"));
    }

    @Test
    public void rejectsFractionsAndUnknownWords() {
        assertNull(SpanishNumberParser.parse("2.5"));
        assertNull(SpanishNumberParser.parse("muchos"));
        assertNull(SpanishNumberParser.parse(null));
    }
}
