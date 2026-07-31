package com.kiko.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class FaceIdentityCodec {
    private static final int MAGIC = 0x4b464143;
    private static final int VERSION = 1;
    private static final int MAX_IDENTITIES = 100;

    private FaceIdentityCodec() {
    }

    static byte[] encode(List<FaceIdentityRecord> records) throws IOException {
        if (records == null || records.size() > MAX_IDENTITIES) {
            throw new IOException("Invalid face identity count");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(records.size());
            for (FaceIdentityRecord record : records) {
                output.writeUTF(record.getId());
                output.writeUTF(record.getSourceHistoryId());
                output.writeUTF(record.getName());
                output.writeLong(record.getEnrolledAtEpochMillis());
                float[] embedding = record.getEmbedding();
                output.writeInt(embedding.length);
                for (float value : embedding) {
                    if (!Float.isFinite(value)) {
                        throw new IOException("Invalid face embedding value");
                    }
                    output.writeFloat(value);
                }
            }
        }
        return bytes.toByteArray();
    }

    static List<FaceIdentityRecord> decode(byte[] encoded) throws IOException {
        if (encoded == null) {
            throw new IOException("Missing face identity data");
        }
        List<FaceIdentityRecord> records = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(encoded)
        )) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("Unsupported face identity data");
            }
            int count = input.readInt();
            if (count < 0 || count > MAX_IDENTITIES) {
                throw new IOException("Invalid face identity count");
            }
            for (int index = 0; index < count; index++) {
                String id = input.readUTF();
                String sourceHistoryId = input.readUTF();
                String name = input.readUTF();
                long enrolledAtEpochMillis = input.readLong();
                int size = input.readInt();
                if (id.isEmpty()
                        || sourceHistoryId.isEmpty()
                        || name.trim().isEmpty()
                        || enrolledAtEpochMillis <= 0L
                        || size != FaceIdentityRecord.EMBEDDING_SIZE) {
                    throw new IOException("Invalid face identity record");
                }
                float[] embedding = new float[size];
                for (int valueIndex = 0; valueIndex < size; valueIndex++) {
                    embedding[valueIndex] = input.readFloat();
                    if (!Float.isFinite(embedding[valueIndex])) {
                        throw new IOException("Invalid face embedding value");
                    }
                }
                records.add(new FaceIdentityRecord(
                        id,
                        sourceHistoryId,
                        name,
                        enrolledAtEpochMillis,
                        embedding
                ));
            }
            if (input.read() != -1) {
                throw new IOException("Unexpected trailing face identity data");
            }
        }
        return records;
    }
}
