package com.kiko.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class VisualSubjectCodec {
    private static final int MAGIC = 0x4b565355;
    private static final int VERSION = 1;
    private static final int MAX_SUBJECTS = 1_000;
    private static final int MAX_NAME_CHARACTERS = 100;

    private VisualSubjectCodec() {
    }

    static byte[] encode(List<VisualSubjectRecord> records) throws IOException {
        if (records == null || records.size() > MAX_SUBJECTS) {
            throw new IOException("Invalid visual subject count");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(records.size());
            Set<String> recordIds = new HashSet<>();
            for (VisualSubjectRecord record : records) {
                if (!recordIds.add(record.getHistoryRecordId())
                        || record.getName().trim().isEmpty()
                        || record.getName().length() > MAX_NAME_CHARACTERS) {
                    throw new IOException("Invalid visual subject record");
                }
                output.writeUTF(record.getHistoryRecordId());
                output.writeUTF(record.getKind().name());
                output.writeUTF(record.getName());
            }
        }
        return bytes.toByteArray();
    }

    static List<VisualSubjectRecord> decode(byte[] encoded) throws IOException {
        if (encoded == null) {
            throw new IOException("Missing visual subject data");
        }
        List<VisualSubjectRecord> records = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(encoded)
        )) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("Unsupported visual subject data");
            }
            int count = input.readInt();
            if (count < 0 || count > MAX_SUBJECTS) {
                throw new IOException("Invalid visual subject count");
            }
            Set<String> recordIds = new HashSet<>();
            for (int index = 0; index < count; index++) {
                String historyRecordId = input.readUTF();
                VisualHistoryRecord.SubjectKind kind;
                try {
                    kind = VisualHistoryRecord.SubjectKind.valueOf(input.readUTF());
                } catch (IllegalArgumentException error) {
                    throw new IOException("Invalid visual subject kind", error);
                }
                String name = input.readUTF();
                if (!recordIds.add(historyRecordId)
                        || historyRecordId.isEmpty()
                        || name.trim().isEmpty()
                        || name.length() > MAX_NAME_CHARACTERS) {
                    throw new IOException("Invalid visual subject record");
                }
                records.add(new VisualSubjectRecord(
                        historyRecordId,
                        kind,
                        name
                ));
            }
            if (input.read() != -1) {
                throw new IOException("Unexpected trailing visual subject data");
            }
        }
        return records;
    }
}
