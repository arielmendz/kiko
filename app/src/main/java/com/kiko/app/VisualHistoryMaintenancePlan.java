package com.kiko.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VisualHistoryMaintenancePlan {
    private final List<SubjectUpdate> subjectUpdates;
    private final List<VisualHistoryRecord> recordsToDelete;
    private final int namedGroupCount;

    private VisualHistoryMaintenancePlan(
            List<SubjectUpdate> subjectUpdates,
            List<VisualHistoryRecord> recordsToDelete,
            int namedGroupCount
    ) {
        this.subjectUpdates = Collections.unmodifiableList(
                new ArrayList<>(subjectUpdates)
        );
        this.recordsToDelete = Collections.unmodifiableList(
                new ArrayList<>(recordsToDelete)
        );
        this.namedGroupCount = namedGroupCount;
    }

    static VisualHistoryMaintenancePlan create(
            List<VisualHistoryRecord> records,
            List<FaceIdentityRecord> identities,
            boolean deleteUnrecognized
    ) {
        Map<String, String> personNamesBySource = new HashMap<>();
        for (FaceIdentityRecord identity : identities) {
            personNamesBySource.put(
                    identity.getSourceHistoryId(),
                    identity.getName()
            );
        }

        List<SubjectUpdate> updates = new ArrayList<>();
        List<VisualHistoryRecord> deletions = new ArrayList<>();
        Set<String> namedGroups = new HashSet<>();
        for (VisualHistoryRecord record : records) {
            String enrolledPerson = personNamesBySource.get(record.getId());
            VisualHistoryRecord.SubjectKind effectiveKind =
                    record.getSubjectKind();
            String effectiveName = record.getSubjectName();
            if (enrolledPerson != null) {
                effectiveKind = VisualHistoryRecord.SubjectKind.PERSON;
                effectiveName = enrolledPerson;
                if (record.getSubjectKind() != effectiveKind
                        || !sameName(record.getSubjectName(), effectiveName)) {
                    updates.add(new SubjectUpdate(
                            record.getId(),
                            effectiveKind,
                            effectiveName
                    ));
                }
            }

            if (effectiveName != null) {
                namedGroups.add(groupKey(effectiveKind, effectiveName));
            }
            if (deleteUnrecognized
                    && effectiveName == null
                    && record.getRecognitionStatus()
                    == VisualHistoryRecord.RecognitionStatus.UNRECOGNIZED) {
                deletions.add(record);
            }
        }
        return new VisualHistoryMaintenancePlan(
                updates,
                deletions,
                namedGroups.size()
        );
    }

    static String groupKey(
            VisualHistoryRecord.SubjectKind kind,
            String name
    ) {
        if (kind == null || name == null) {
            return "";
        }
        return kind.name() + ":" + PersonMemoryRecord.canonicalizeName(name);
    }

    private static boolean sameName(String left, String right) {
        return left != null
                && right != null
                && PersonMemoryRecord.canonicalizeName(left).equals(
                        PersonMemoryRecord.canonicalizeName(right)
                );
    }

    List<SubjectUpdate> getSubjectUpdates() {
        return subjectUpdates;
    }

    List<VisualHistoryRecord> getRecordsToDelete() {
        return recordsToDelete;
    }

    int getNamedGroupCount() {
        return namedGroupCount;
    }

    static final class SubjectUpdate {
        private final String recordId;
        private final VisualHistoryRecord.SubjectKind subjectKind;
        private final String subjectName;

        private SubjectUpdate(
                String recordId,
                VisualHistoryRecord.SubjectKind subjectKind,
                String subjectName
        ) {
            this.recordId = recordId;
            this.subjectKind = subjectKind;
            this.subjectName = subjectName;
        }

        String getRecordId() {
            return recordId;
        }

        VisualHistoryRecord.SubjectKind getSubjectKind() {
            return subjectKind;
        }

        String getSubjectName() {
            return subjectName;
        }
    }
}
