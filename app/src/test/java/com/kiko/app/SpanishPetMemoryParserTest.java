package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public final class SpanishPetMemoryParserTest {
    @Test
    public void registersOnlyCatsAndDogsWithOptionalOwner() {
        PetMemoryCommand relation = SpanishPetMemoryParser.parse(
                Arrays.asList("Kiko, Luna es la gata de Pedro")
        );
        PetMemoryCommand ownerForm = SpanishPetMemoryParser.parse(
                "María tiene un perro que se llama Toby"
        );
        PetMemoryCommand myPet = SpanishPetMemoryParser.parse(
                "mi perra se llama Nala"
        );

        assertEquals(PetMemoryCommand.Type.REGISTER, relation.getType());
        assertEquals("Luna", relation.getPetName());
        assertEquals(PetMemoryCommand.Kind.GATA, relation.getKind());
        assertEquals("Pedro", relation.getOwnerName());
        assertEquals("Toby", ownerForm.getPetName());
        assertEquals(PetMemoryCommand.Kind.PERRO, ownerForm.getKind());
        assertEquals("María", ownerForm.getOwnerName());
        assertEquals("Nala", myPet.getPetName());
        assertNull(myPet.getOwnerName());
        assertTrue(relation.isUpdate());
        assertNull(SpanishPetMemoryParser.parse("Pipo es un loro"));
    }

    @Test
    public void parsesPetFactsWithAnExplicitSpecies() {
        PetMemoryCommand food = SpanishPetMemoryParser.parse(
                "la comida favorita de la gata Luna es el atún"
        );
        PetMemoryCommand like = SpanishPetMemoryParser.parse(
                "a la gata Luna le gusta dormir al sol"
        );
        PetMemoryCommand age = SpanishPetMemoryParser.parse(
                "el perro Toby tiene 4 años"
        );

        assertEquals(PetMemoryCommand.Type.SET_FAVORITE_FOOD, food.getType());
        assertEquals("el atún", food.getTextValue());
        assertEquals(PetMemoryCommand.Type.ADD_LIKE, like.getType());
        assertEquals("dormir al sol", like.getTextValue());
        assertEquals(PetMemoryCommand.Type.SET_AGE, age.getType());
        assertEquals(Integer.valueOf(4), age.getNumberValue());
        assertNull(SpanishPetMemoryParser.parse("Luna tiene 4 años"));
        assertNull(SpanishPetMemoryParser.parse("la tortuga Luna tiene 4 años"));
        assertNull(SpanishPetMemoryParser.parse("la gata Luna tiene 99 años"));
    }

    @Test
    public void parsesPetAndOwnerQueries() {
        PetMemoryCommand summary = SpanishPetMemoryParser.parse(
                "¿Qué sabes de la gata Luna?"
        );
        PetMemoryCommand likes = SpanishPetMemoryParser.parse(
                "qué le gusta a la gata Luna"
        );
        PetMemoryCommand favorite = SpanishPetMemoryParser.parse(
                "cuál es la comida favorita de la gata Luna"
        );
        PetMemoryCommand owner = SpanishPetMemoryParser.parse(
                "qué mascotas tiene Pedro"
        );

        assertEquals(PetMemoryCommand.Type.QUERY_SUMMARY, summary.getType());
        assertEquals(PetMemoryCommand.Type.QUERY_LIKES, likes.getType());
        assertEquals(
                PetMemoryCommand.Type.QUERY_FAVORITE_FOOD,
                favorite.getType()
        );
        assertEquals(PetMemoryCommand.Type.QUERY_OWNER_PETS, owner.getType());
        assertEquals("Pedro", owner.getOwnerName());
        assertFalse(owner.isUpdate());
    }
}
