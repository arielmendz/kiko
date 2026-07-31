package com.kiko.app;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class SpanishPetMemoryResponsesTest {
    @Test
    public void composesRegistrationAndStructuredSummary() {
        PetMemoryCommand register = SpanishPetMemoryParser.parse(
                "Luna es la gata de Pedro"
        );
        PetMemoryRecord record = PetMemoryRecord.empty(
                "Luna",
                PetMemoryCommand.Kind.GATA,
                1L
        ).apply(register, 2L)
                .apply(SpanishPetMemoryParser.parse(
                        "la comida favorita de la gata Luna es el atún"
                ), 3L)
                .apply(SpanishPetMemoryParser.parse(
                        "a la gata Luna le gusta dormir al sol"
                ), 4L)
                .apply(SpanishPetMemoryParser.parse(
                        "la gata Luna tiene 3 años"
                ), 5L);

        assertEquals(
                "Recordaré que Luna es la gata de Pedro.",
                SpanishPetMemoryResponses.updateResponse(register)
        );
        assertEquals(
                "Sé que Luna es la gata de Pedro, le gusta el atún, "
                        + "le gusta dormir al sol y tiene 3 años.",
                SpanishPetMemoryResponses.queryResponse(
                        SpanishPetMemoryParser.parse(
                                "qué sabes de la gata Luna"
                        ),
                        record,
                        null
                )
        );
    }

    @Test
    public void listsAnOwnersPetsAndAdmitsUnknowns() {
        PetMemoryRecord luna = PetMemoryRecord.empty(
                "Luna",
                PetMemoryCommand.Kind.GATA,
                1L
        ).apply(SpanishPetMemoryParser.parse(
                "Luna es la gata de Pedro"
        ), 2L);
        PetMemoryRecord toby = PetMemoryRecord.empty(
                "Toby",
                PetMemoryCommand.Kind.PERRO,
                1L
        ).apply(SpanishPetMemoryParser.parse(
                "Toby es el perro de Pedro"
        ), 2L);
        PetMemoryCommand ownerQuery = SpanishPetMemoryParser.parse(
                "qué mascotas tiene Pedro"
        );

        assertEquals(
                "Pedro tiene una gata llamada Luna y un perro llamado Toby.",
                SpanishPetMemoryResponses.queryResponse(
                        ownerQuery,
                        null,
                        Arrays.asList(luna, toby)
                )
        );
        assertEquals(
                "No sé qué mascotas tiene Pedro todavía.",
                SpanishPetMemoryResponses.queryResponse(
                        ownerQuery,
                        null,
                        Collections.emptyList()
                )
        );
    }
}
