package com.kiko.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class PetMemoryCodec {
    private static final int MAGIC = 0x4b504554;
    private static final int VERSION = 1;
    private static final int MAX_PETS = 100;
    private static final int MAX_LIKES_PER_PET = 20;

    private PetMemoryCodec() {
    }

    static byte[] encode(List<PetMemoryRecord> records) throws IOException {
        if (records == null || records.size() > MAX_PETS) {
            throw new IOException("Invalid pet memory count");
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(records.size());
            for (PetMemoryRecord record : records) {
                output.writeUTF(record.getCanonicalName());
                output.writeUTF(record.getDisplayName());
                output.writeUTF(record.getKind().name());
                writeNullableString(output, record.getCanonicalOwnerName());
                writeNullableString(output, record.getDisplayOwnerName());
                writeNullableString(output, record.getFavoriteFood());
                List<String> likes = record.getLikes();
                if (likes.size() > MAX_LIKES_PER_PET) {
                    throw new IOException("Too many pet likes");
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

    static List<PetMemoryRecord> decode(byte[] encoded) throws IOException {
        if (encoded == null) {
            throw new IOException("Missing pet memory data");
        }
        List<PetMemoryRecord> records = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(encoded)
        )) {
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw new IOException("Unsupported pet memory data");
            }
            int count = input.readInt();
            if (count < 0 || count > MAX_PETS) {
                throw new IOException("Invalid pet memory count");
            }
            for (int index = 0; index < count; index++) {
                String canonicalName = input.readUTF();
                String displayName = input.readUTF();
                PetMemoryCommand.Kind kind;
                try {
                    kind = PetMemoryCommand.Kind.valueOf(input.readUTF());
                } catch (IllegalArgumentException error) {
                    throw new IOException("Invalid pet kind", error);
                }
                String canonicalOwner = readNullableString(input);
                String displayOwner = readNullableString(input);
                String favoriteFood = readNullableString(input);
                int likeCount = input.readInt();
                if (likeCount < 0 || likeCount > MAX_LIKES_PER_PET) {
                    throw new IOException("Invalid pet like count");
                }
                List<String> likes = new ArrayList<>();
                for (int likeIndex = 0; likeIndex < likeCount; likeIndex++) {
                    String like = input.readUTF();
                    if (like.trim().isEmpty()) {
                        throw new IOException("Invalid pet like");
                    }
                    likes.add(like);
                }
                int encodedAge = input.readInt();
                Integer age = encodedAge < 0 ? null : encodedAge;
                long updatedAt = input.readLong();
                if (canonicalName.isEmpty()
                        || displayName.trim().isEmpty()
                        || (canonicalOwner == null) != (displayOwner == null)
                        || (canonicalOwner != null && canonicalOwner.isEmpty())
                        || (displayOwner != null && displayOwner.trim().isEmpty())
                        || (favoriteFood != null && favoriteFood.trim().isEmpty())
                        || (age != null && (age <= 0 || age > 40))
                        || updatedAt <= 0L) {
                    throw new IOException("Invalid pet memory record");
                }
                records.add(new PetMemoryRecord(
                        canonicalName,
                        displayName,
                        kind,
                        canonicalOwner,
                        displayOwner,
                        favoriteFood,
                        likes,
                        age,
                        updatedAt
                ));
            }
            if (input.read() != -1) {
                throw new IOException("Unexpected trailing pet memory data");
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
