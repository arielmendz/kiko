package com.kiko.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class VisualHistoryGrouping {
    private VisualHistoryGrouping() {
    }

    static List<VisualHistoryRecord> arrange(List<VisualHistoryRecord> input) {
        List<VisualHistoryRecord> output = new ArrayList<>(input);
        output.sort(Comparator
                .comparingInt(VisualHistoryGrouping::subjectOrder)
                .thenComparing(VisualHistoryGrouping::canonicalSubjectName)
                .thenComparing(
                        VisualHistoryRecord::getCapturedAtEpochMillis,
                        Comparator.reverseOrder()
                )
                .thenComparing(
                        VisualHistoryRecord::getId,
                        Comparator.reverseOrder()
                ));
        return output;
    }

    static boolean startsGroup(List<VisualHistoryRecord> records, int position) {
        return position == 0 || !groupKey(records.get(position)).equals(
                groupKey(records.get(position - 1))
        );
    }

    static int groupSize(List<VisualHistoryRecord> records, int position) {
        String key = groupKey(records.get(position));
        int count = 0;
        for (VisualHistoryRecord record : records) {
            if (key.equals(groupKey(record))) {
                count++;
            }
        }
        return count;
    }

    private static int subjectOrder(VisualHistoryRecord record) {
        if (record.getSubjectKind() == VisualHistoryRecord.SubjectKind.PERSON) {
            return 0;
        }
        if (record.getSubjectKind() == VisualHistoryRecord.SubjectKind.PET) {
            return 1;
        }
        return 2;
    }

    private static String canonicalSubjectName(VisualHistoryRecord record) {
        return record.getSubjectName() == null
                ? ""
                : PersonMemoryRecord.canonicalizeName(record.getSubjectName());
    }

    private static String groupKey(VisualHistoryRecord record) {
        if (record.getSubjectName() == null) {
            return "UNKNOWN";
        }
        return VisualHistoryMaintenancePlan.groupKey(
                record.getSubjectKind(),
                record.getSubjectName()
        );
    }
}
