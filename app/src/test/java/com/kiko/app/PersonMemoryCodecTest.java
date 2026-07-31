package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class PersonMemoryCodecTest {
    @Test
    public void roundTripsStructuredPersonFacts() throws Exception {
        PersonMemoryRecord source = new PersonMemoryRecord(
                "maria jose",
                "María José",
                "la pasta",
                Arrays.asList("el fútbol", "los perros"),
                10,
                1_785_529_600_000L
        );

        List<PersonMemoryRecord> decoded = PersonMemoryCodec.decode(
                PersonMemoryCodec.encode(Arrays.asList(source))
        );

        assertEquals(1, decoded.size());
        PersonMemoryRecord result = decoded.get(0);
        assertEquals("maria jose", result.getCanonicalName());
        assertEquals("María José", result.getDisplayName());
        assertEquals("la pasta", result.getFavoriteFood());
        assertEquals(Arrays.asList("el fútbol", "los perros"), result.getLikes());
        assertEquals(Integer.valueOf(10), result.getAge());
        assertEquals(1_785_529_600_000L, result.getUpdatedAtEpochMillis());
    }

    @Test
    public void rejectsMalformedOrTrailingData() throws Exception {
        byte[] valid = PersonMemoryCodec.encode(Arrays.asList(
                PersonMemoryRecord.empty("Pedro", 1L).apply(
                        SpanishPersonMemoryParser.parse(
                                "Pedro tiene 10 años"
                        ),
                        2L
                )
        ));

        assertThrows(
                IOException.class,
                () -> PersonMemoryCodec.decode(Arrays.copyOf(
                        valid,
                        valid.length + 1
                ))
        );
        assertThrows(
                IOException.class,
                () -> PersonMemoryCodec.decode(new byte[]{1, 2, 3, 4})
        );
    }
}
