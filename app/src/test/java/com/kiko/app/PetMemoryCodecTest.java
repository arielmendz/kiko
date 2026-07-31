package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class PetMemoryCodecTest {
    @Test
    public void roundTripsStructuredPetFacts() throws Exception {
        PetMemoryRecord source = new PetMemoryRecord(
                "luna",
                "Luna",
                PetMemoryCommand.Kind.GATA,
                "pedro",
                "Pedro",
                "el atún",
                Arrays.asList("dormir", "jugar"),
                3,
                1_785_529_600_000L
        );

        List<PetMemoryRecord> decoded = PetMemoryCodec.decode(
                PetMemoryCodec.encode(Arrays.asList(source))
        );

        assertEquals(1, decoded.size());
        PetMemoryRecord result = decoded.get(0);
        assertEquals("luna", result.getCanonicalName());
        assertEquals(PetMemoryCommand.Kind.GATA, result.getKind());
        assertEquals("Pedro", result.getDisplayOwnerName());
        assertEquals("el atún", result.getFavoriteFood());
        assertEquals(Arrays.asList("dormir", "jugar"), result.getLikes());
        assertEquals(Integer.valueOf(3), result.getAge());
    }

    @Test
    public void rejectsMalformedOrTrailingData() throws Exception {
        byte[] valid = PetMemoryCodec.encode(Arrays.asList(
                PetMemoryRecord.empty(
                        "Luna",
                        PetMemoryCommand.Kind.GATA,
                        1L
                )
        ));

        assertThrows(
                IOException.class,
                () -> PetMemoryCodec.decode(Arrays.copyOf(
                        valid,
                        valid.length + 1
                ))
        );
        assertThrows(
                IOException.class,
                () -> PetMemoryCodec.decode(new byte[]{1, 2, 3, 4})
        );
    }
}
