package com.kiko.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class PersonMemoryCodec {
    private static final int MAGIC = 0x4b504d45;
    private static final int VERSION = 1;
    private static final int MAX_PEOPLE = 100;
    private static final int MAX_LIKES_PER_PERSON = 20;

    private PersonMemoryCodec() {
    }

    static byte[] encode(List<PersonMemoryRecord> records) throws IOException {
        if (records == null || records.size() > MAX_PEOPLE) {
            throw new IOException("Invalid person memory count");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(records.size());
            for (PersonMemoryRecord record : records) {
                output.writeUTF(record.getCanonicalName());
                output.writeUTF(record.getDisplayName());
                writeNullableString(output, record.getFavoriteFood());
                List<String> likes = record.getLikes();
                if (likes.size() > MAX_LIKES_PER_PERSON) {
                    throw new IOException("Too many person likes");
                }
                output.writeInt(likes.size());
                for (String like : likes) {
                    output.writeUTF(like);
                }
                output.writeInt(record.getAge() == null ? -1 : record.getAge());
                output.writeLong(record.getUpdatedAtEpochMillis());
            }
        }
        return bytes.toByteArray();
    }

    static List<PersonMemoryRecord> decode(byte[] encoded) throws IOException {
        if (encoded == null) {
            throw new IOException("Missing person memory data");
        }
        List<PersonMemoryRecord> records = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(encoded)
        )) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("Unsupported person memory data");
            }
            int count = input.readInt();
            if (count < 0 || count > MAX_PEOPLE) {
                throw new IOException("Invalid person memory count");
            }
            for (int index = 0; index < count; index++) {
                String canonicalName = input.readUTF();
                String displayName = input.readUTF();
                String favoriteFood = readNullableString(input);
                int likeCount = input.readInt();
                if (likeCount < 0 || likeCount > MAX_LIKES_PER_PERSON) {
                    throw new IOException("Invalid person like count");
                }
                List<String> likes = new ArrayList<>();
                for (int likeIndex = 0; likeIndex < likeCount; likeIndex++) {
                    String like = input.readUTF();
                    if (like.trim().isEmpty()) {
                        throw new IOException("Invalid person like");
                    }
                    likes.add(like);
                }
                int encodedAge = input.readInt();
                Integer age = encodedAge < 0 ? null : encodedAge;
                long updatedAt = input.readLong();
                if (canonicalName.isEmpty()
                        || displayName.trim().isEmpty()
                        || (favoriteFood != null && favoriteFood.trim().isEmpty())
                        || (age != null && (age <= 0 || age > 130))
                        || updatedAt <= 0L) {
                    throw new IOException("Invalid person memory record");
                }
                records.add(new PersonMemoryRecord(
                        canonicalName,
                        displayName,
                        favoriteFood,
                        likes,
                        age,
                        updatedAt
                ));
            }
            if (input.read() != -1) {
                throw new IOException("Unexpected trailing person memory data");
            }
        }
        return records;
    }

    private static void writeNullableString(
            DataOutputStream output,
            String value
    ) throws IOException {
        output.writeBoolean(value != null);
        if (value != null) {
            output.writeUTF(value);
        }
    }

    private static String readNullableString(DataInputStream input)
            throws IOException {
        return input.readBoolean() ? input.readUTF() : null;
    }
}
