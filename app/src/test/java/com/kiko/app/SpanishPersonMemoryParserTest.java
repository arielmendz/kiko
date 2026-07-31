package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public final class SpanishPersonMemoryParserTest {
    @Test
    public void parsesFavoriteFoodUpdateAfterWakeWord() {
        PersonMemoryCommand command = SpanishPersonMemoryParser.parse(
                Arrays.asList("Kiko, la comida favorita de Pedro es la pasta")
        );

        assertEquals(
                PersonMemoryCommand.Type.SET_FAVORITE_FOOD,
                command.getType()
        );
        assertEquals("Pedro", command.getPersonName());
        assertEquals("la pasta", command.getTextValue());
        assertTrue(command.isUpdate());
    }

    @Test
    public void parsesLikesAndAgeUpdates() {
        PersonMemoryCommand like = SpanishPersonMemoryParser.parse(
                "a María José le gusta el fútbol"
        );
        PersonMemoryCommand age = SpanishPersonMemoryParser.parse(
                "Pedro tiene 10 años"
        );

        assertEquals(PersonMemoryCommand.Type.ADD_LIKE, like.getType());
        assertEquals("María José", like.getPersonName());
        assertEquals("el fútbol", like.getTextValue());
        assertEquals(PersonMemoryCommand.Type.SET_AGE, age.getType());
        assertEquals(Integer.valueOf(10), age.getNumberValue());
    }

    @Test
    public void parsesTheThreePersonQueries() {
        PersonMemoryCommand likes = SpanishPersonMemoryParser.parse(
                "¿Qué le gusta a Pedro?"
        );
        PersonMemoryCommand summary = SpanishPersonMemoryParser.parse(
                "que sabes de Pedro"
        );
        PersonMemoryCommand favorite = SpanishPersonMemoryParser.parse(
                "¿Cuál es la comida favorita de Pedro?"
        );

        assertEquals(PersonMemoryCommand.Type.QUERY_LIKES, likes.getType());
        assertEquals(PersonMemoryCommand.Type.QUERY_SUMMARY, summary.getType());
        assertEquals(
                PersonMemoryCommand.Type.QUERY_FAVORITE_FOOD,
                favorite.getType()
        );
        assertFalse(favorite.isUpdate());
    }

    @Test
    public void rejectsUnboundedOrUnsupportedClaims() {
        assertNull(SpanishPersonMemoryParser.parse("Pedro tiene 999 años"));
        assertNull(SpanishPersonMemoryParser.parse(
                "la comida favorita de una persona con nombre demasiado largo es pasta"
        ));
        assertNull(SpanishPersonMemoryParser.parse(
                "Pedro vive en una casa azul"
        ));
        assertNull(SpanishPersonMemoryParser.parse((String) null));
    }
}
