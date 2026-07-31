package com.kiko.app;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class SpanishPersonMemoryResponsesTest {
    @Test
    public void composesTheRequestedFavoriteFoodReaction() {
        PersonMemoryCommand command = SpanishPersonMemoryParser.parse(
                "la comida favorita de Pedro es la pasta"
        );

        assertEquals(
                "¡A mí también me gusta la pasta!",
                SpanishPersonMemoryResponses.updateResponse(command)
        );
    }

    @Test
    public void combinesLikesAndAgeWithoutGuessing() {
        PersonMemoryRecord record = PersonMemoryRecord.empty("Pedro", 1L)
                .apply(SpanishPersonMemoryParser.parse(
                        "la comida favorita de Pedro es la pasta"
                ), 2L)
                .apply(SpanishPersonMemoryParser.parse(
                        "a Pedro le gusta el fútbol"
                ), 3L)
                .apply(SpanishPersonMemoryParser.parse(
                        "Pedro tiene 10 años"
                ), 4L);

        assertEquals(
                "A Pedro le gusta la pasta y el fútbol.",
                SpanishPersonMemoryResponses.queryResponse(
                        SpanishPersonMemoryParser.parse(
                                "qué le gusta a Pedro"
                        ),
                        record
                )
        );
        assertEquals(
                "Sé que le gusta la pasta, le gusta el fútbol y tiene 10 años.",
                SpanishPersonMemoryResponses.queryResponse(
                        SpanishPersonMemoryParser.parse(
                                "qué sabes de Pedro"
                        ),
                        record
                )
        );
        assertEquals(
                "La pasta.",
                SpanishPersonMemoryResponses.queryResponse(
                        SpanishPersonMemoryParser.parse(
                                "cuál es la comida favorita de Pedro"
                        ),
                        record
                )
        );
    }

    @Test
    public void givesSpecificUnknownAnswers() {
        assertEquals(
                "No sé nada de Pedro todavía.",
                SpanishPersonMemoryResponses.queryResponse(
                        SpanishPersonMemoryParser.parse("qué sabes de Pedro"),
                        null
                )
        );
        assertEquals(
                "No sé cuál es la comida favorita de Pedro.",
                SpanishPersonMemoryResponses.queryResponse(
                        SpanishPersonMemoryParser.parse(
                                "cuál es la comida favorita de Pedro"
                        ),
                        null
                )
        );
    }

    @Test
    public void deduplicatesFavoriteFoodFromGeneralLikes() {
        PersonMemoryRecord record = PersonMemoryRecord.empty("Pedro", 1L)
                .apply(SpanishPersonMemoryParser.parse(
                        "la comida favorita de Pedro es la pasta"
                ), 2L)
                .apply(SpanishPersonMemoryParser.parse(
                        "a Pedro le gusta la pasta"
                ), 3L);

        assertEquals(
                "A Pedro le gusta la pasta.",
                SpanishPersonMemoryResponses.queryResponse(
                        SpanishPersonMemoryParser.parse(
                                "qué le gusta a Pedro"
                        ),
                        record
                )
        );
    }
}
