package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class VisualHistoryMaintenancePlanTest {
    @Test
    public void restoresEnrolledNamesGroupsSubjectsAndDeletesOnlyUnknownPhotos() {
        VisualHistoryRecord enrolledSource = unrecognizedRecord(
                "enrolled",
                40L
        );
        VisualHistoryRecord recognizedPedro = record(
                "recognized",
                30L,
                VisualHistoryRecord.SubjectKind.PERSON,
                "Pédro"
        );
        VisualHistoryRecord petLuna = record(
                "pet",
                20L,
                VisualHistoryRecord.SubjectKind.PET,
                "Luna"
        );
        VisualHistoryRecord unknown = unrecognizedRecord("unknown", 10L);
        FaceIdentityRecord identity = new FaceIdentityRecord(
                "identity",
                "enrolled",
                "Pedro",
                50L,
                embedding()
        );

        VisualHistoryMaintenancePlan plan = VisualHistoryMaintenancePlan.create(
                Arrays.asList(
                        enrolledSource,
                        recognizedPedro,
                        petLuna,
                        unknown
                ),
                Collections.singletonList(identity),
                true
        );

        assertEquals(1, plan.getSubjectUpdates().size());
        assertEquals("enrolled", plan.getSubjectUpdates().get(0).getRecordId());
        assertEquals(1, plan.getRecordsToDelete().size());
        assertEquals("unknown", plan.getRecordsToDelete().get(0).getId());
        assertEquals(2, plan.getNamedGroupCount());
    }

    @Test
    public void leavesUnknownPhotosWhenCleanupIsDisabled() {
        VisualHistoryMaintenancePlan plan = VisualHistoryMaintenancePlan.create(
                Collections.singletonList(record("unknown", 10L, null, null)),
                Collections.emptyList(),
                false
        );

        assertTrue(plan.getRecordsToDelete().isEmpty());
        assertEquals(0, plan.getNamedGroupCount());
    }

    @Test
    public void keepsLegacyPhotoWithUnknownRecognitionStatus() {
        VisualHistoryMaintenancePlan plan = VisualHistoryMaintenancePlan.create(
                Collections.singletonList(record("legacy", 10L, null, null)),
                Collections.emptyList(),
                true
        );

        assertTrue(plan.getRecordsToDelete().isEmpty());
    }

    @Test
    public void keepsRecognizedUnnamedAndExplicitlyTaggedPhotos() {
        VisualHistoryRecord recognizedObject = new VisualHistoryRecord(
                "car",
                20L,
                "Veo un auto.",
                VisualHistoryRecord.RecognitionStatus.RECOGNIZED,
                null,
                null,
                new File("car.jpg")
        );
        VisualHistoryRecord taggedFalseNegative = new VisualHistoryRecord(
                "luna",
                10L,
                "No logro distinguir qué hay delante de mí.",
                VisualHistoryRecord.RecognitionStatus.UNRECOGNIZED,
                VisualHistoryRecord.SubjectKind.PET,
                "Luna",
                new File("luna.jpg")
        );

        VisualHistoryMaintenancePlan plan = VisualHistoryMaintenancePlan.create(
                Arrays.asList(recognizedObject, taggedFalseNegative),
                Collections.emptyList(),
                true
        );

        assertTrue(plan.getRecordsToDelete().isEmpty());
        assertEquals(1, plan.getNamedGroupCount());
    }

    @Test
    public void arrangesPhotosInPersonPetAndUnknownGroups() {
        List<VisualHistoryRecord> arranged = VisualHistoryGrouping.arrange(
                Arrays.asList(
                        record("unknown", 50L, null, null),
                        record("luna", 20L, VisualHistoryRecord.SubjectKind.PET, "Luna"),
                        record("pedro-old", 10L, VisualHistoryRecord.SubjectKind.PERSON, "Pedro"),
                        record("pedro-new", 40L, VisualHistoryRecord.SubjectKind.PERSON, "Pédro")
                )
        );

        assertEquals("pedro-new", arranged.get(0).getId());
        assertEquals("pedro-old", arranged.get(1).getId());
        assertEquals("luna", arranged.get(2).getId());
        assertEquals("unknown", arranged.get(3).getId());
        assertTrue(VisualHistoryGrouping.startsGroup(arranged, 0));
        assertEquals(2, VisualHistoryGrouping.groupSize(arranged, 0));
        assertTrue(VisualHistoryGrouping.startsGroup(arranged, 2));
        assertTrue(VisualHistoryGrouping.startsGroup(arranged, 3));
    }

    private static VisualHistoryRecord record(
            String id,
            long capturedAt,
            VisualHistoryRecord.SubjectKind kind,
            String name
    ) {
        return new VisualHistoryRecord(
                id,
                capturedAt,
                "descripción",
                kind,
                name,
                new File(id + ".jpg")
        );
    }

    private static VisualHistoryRecord unrecognizedRecord(
            String id,
            long capturedAt
    ) {
        return new VisualHistoryRecord(
                id,
                capturedAt,
                "sin detecciones",
                VisualHistoryRecord.RecognitionStatus.UNRECOGNIZED,
                null,
                null,
                new File(id + ".jpg")
        );
    }

    private static float[] embedding() {
        float[] values = new float[FaceIdentityRecord.EMBEDDING_SIZE];
        values[0] = 1f;
        return values;
    }
}
