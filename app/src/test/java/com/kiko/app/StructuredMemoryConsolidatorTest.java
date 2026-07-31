package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class StructuredMemoryConsolidatorTest {
    @Test
    public void mergesDuplicatePeopleUsingLatestReplacementFields() {
        PersonMemoryRecord older = new PersonMemoryRecord(
                "pedro",
                "Pedro",
                "la pasta",
                Arrays.asList("el fútbol", "LA PÁSTA"),
                9,
                10L
        );
        PersonMemoryRecord newer = new PersonMemoryRecord(
                "pedro",
                "Pedro",
                null,
                Arrays.asList("el futbol", "dibujar"),
                10,
                20L
        );

        MemoryConsolidationResult<PersonMemoryRecord> result =
                StructuredMemoryConsolidator.consolidatePeople(
                        Arrays.asList(older, newer)
                );

        assertTrue(result.changed());
        assertEquals(1, result.getDuplicateRecordsMerged());
        assertEquals(2, result.getDuplicateLikesRemoved());
        assertEquals(1, result.getRecords().size());
        PersonMemoryRecord consolidated = result.getRecords().get(0);
        assertEquals("la pasta", consolidated.getFavoriteFood());
        assertEquals(Integer.valueOf(10), consolidated.getAge());
        assertEquals(Arrays.asList("el futbol", "dibujar"), consolidated.getLikes());
        assertEquals(20L, consolidated.getUpdatedAtEpochMillis());
    }

    @Test
    public void mergesPetRecordsWithoutLosingAnOlderOwner() {
        PetMemoryRecord older = new PetMemoryRecord(
                "luna",
                "Luna",
                PetMemoryCommand.Kind.GATA,
                "pedro",
                "Pedro",
                "el atún",
                Collections.singletonList("dormir"),
                2,
                10L
        );
        PetMemoryRecord newer = new PetMemoryRecord(
                "luna",
                "Luna",
                PetMemoryCommand.Kind.GATA,
                null,
                null,
                null,
                Arrays.asList("Dormir", "jugar"),
                3,
                20L
        );

        MemoryConsolidationResult<PetMemoryRecord> result =
                StructuredMemoryConsolidator.consolidatePets(
                        Arrays.asList(older, newer)
                );

        assertEquals(1, result.getDuplicateRecordsMerged());
        assertEquals(1, result.getDuplicateLikesRemoved());
        PetMemoryRecord consolidated = result.getRecords().get(0);
        assertEquals("Pedro", consolidated.getDisplayOwnerName());
        assertEquals("el atún", consolidated.getFavoriteFood());
        assertEquals(Integer.valueOf(3), consolidated.getAge());
        assertEquals(Arrays.asList("Dormir", "jugar"), consolidated.getLikes());
    }

    @Test
    public void refusesToMergeWhenDistinctLikesWouldExceedTheSchemaLimit() {
        List<String> firstLikes = new ArrayList<>();
        List<String> secondLikes = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            firstLikes.add("gusto a" + letter(index));
            secondLikes.add("gusto b" + letter(index));
        }
        PersonMemoryRecord first = new PersonMemoryRecord(
                "ana",
                "Ana",
                null,
                firstLikes,
                null,
                20L
        );
        PersonMemoryRecord second = new PersonMemoryRecord(
                "ana",
                "Ana",
                null,
                secondLikes,
                null,
                10L
        );

        MemoryConsolidationResult<PersonMemoryRecord> result =
                StructuredMemoryConsolidator.consolidatePeople(
                        Arrays.asList(first, second)
                );

        assertFalse(result.changed());
        assertEquals(0, result.getDuplicateRecordsMerged());
        assertEquals(2, result.getRecords().size());
        assertEquals(22, result.getRecords().stream()
                .mapToInt(record -> record.getLikes().size())
                .sum());
    }

    @Test
    public void leavesAlreadyCanonicalRecordsUntouched() {
        PersonMemoryRecord record = new PersonMemoryRecord(
                "ana",
                "Ana",
                "el arroz",
                Collections.singletonList("leer"),
                12,
                20L
        );

        MemoryConsolidationResult<PersonMemoryRecord> result =
                StructuredMemoryConsolidator.consolidatePeople(
                        Collections.singletonList(record)
                );

        assertFalse(result.changed());
        assertEquals(1, result.getRecordsBefore());
        assertEquals(1, result.getRecords().size());
    }

    private static String letter(int index) {
        return Character.toString((char) ('a' + index));
    }
}
