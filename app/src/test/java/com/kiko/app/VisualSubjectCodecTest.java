package com.kiko.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class VisualSubjectCodecTest {
    @Test
    public void roundTripsTypedPersonAndPetAssociations() throws Exception {
        List<VisualSubjectRecord> decoded = VisualSubjectCodec.decode(
                VisualSubjectCodec.encode(Arrays.asList(
                        new VisualSubjectRecord(
                                "person-photo",
                                VisualHistoryRecord.SubjectKind.PERSON,
                                "María José"
                        ),
                        new VisualSubjectRecord(
                                "pet-photo",
                                VisualHistoryRecord.SubjectKind.PET,
                                "Luna"
                        )
                ))
        );

        assertEquals(2, decoded.size());
        assertEquals(VisualHistoryRecord.SubjectKind.PERSON, decoded.get(0).getKind());
        assertEquals("María José", decoded.get(0).getName());
        assertEquals(VisualHistoryRecord.SubjectKind.PET, decoded.get(1).getKind());
        assertEquals("Luna", decoded.get(1).getName());
    }

    @Test
    public void rejectsUnsupportedAndTrailingData() throws Exception {
        byte[] valid = VisualSubjectCodec.encode(Arrays.asList(
                new VisualSubjectRecord(
                        "photo",
                        VisualHistoryRecord.SubjectKind.PERSON,
                        "Pedro"
                )
        ));
        byte[] trailing = Arrays.copyOf(valid, valid.length + 1);

        assertThrows(
                IOException.class,
                () -> VisualSubjectCodec.decode(new byte[]{0, 0, 0, 0})
        );
        assertThrows(
                IOException.class,
                () -> VisualSubjectCodec.decode(trailing)
        );
        assertThrows(
                IOException.class,
                () -> VisualSubjectCodec.encode(Arrays.asList(
                        new VisualSubjectRecord(
                                "photo",
                                VisualHistoryRecord.SubjectKind.PERSON,
                                "Pedro"
                        ),
                        new VisualSubjectRecord(
                                "photo",
                                VisualHistoryRecord.SubjectKind.PET,
                                "Luna"
                        )
                ))
        );
    }
}
