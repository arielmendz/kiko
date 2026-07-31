package com.kiko.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PetMemoryRecord {
    private final String canonicalName;
    private final String displayName;
    private final PetMemoryCommand.Kind kind;
    private final String canonicalOwnerName;
    private final String displayOwnerName;
    private final String favoriteFood;
    private final List<String> likes;
    private final Integer age;
    private final long updatedAtEpochMillis;

    PetMemoryRecord(
            String canonicalName,
            String displayName,
            PetMemoryCommand.Kind kind,
            String canonicalOwnerName,
            String displayOwnerName,
            String favoriteFood,
            List<String> likes,
            Integer age,
            long updatedAtEpochMillis
    ) {
        this.canonicalName = Objects.requireNonNull(canonicalName);
        this.displayName = Objects.requireNonNull(displayName);
        this.kind = Objects.requireNonNull(kind);
        this.canonicalOwnerName = canonicalOwnerName;
        this.displayOwnerName = displayOwnerName;
        this.favoriteFood = favoriteFood;
        this.likes = Collections.unmodifiableList(new ArrayList<>(likes));
        this.age = age;
        this.updatedAtEpochMillis = updatedAtEpochMillis;
    }

    static PetMemoryRecord empty(
            String displayName,
            PetMemoryCommand.Kind kind,
            long now
    ) {
        return new PetMemoryRecord(
                canonicalize(displayName),
                displayName,
                kind,
                null,
                null,
                null,
                Collections.emptyList(),
                null,
                now
        );
    }

    PetMemoryRecord apply(PetMemoryCommand command, long now) {
        PetMemoryCommand.Kind updatedKind = command.getKind() == null
                ? kind : command.getKind();
        switch (command.getType()) {
            case REGISTER:
                return new PetMemoryRecord(
                        canonicalName,
                        command.getPetName(),
                        updatedKind,
                        command.getOwnerName() == null
                                ? canonicalOwnerName
                                : canonicalize(command.getOwnerName()),
                        command.getOwnerName() == null
                                ? displayOwnerName
                                : command.getOwnerName(),
                        favoriteFood,
                        likes,
                        age,
                        now
                );
            case SET_FAVORITE_FOOD:
                return new PetMemoryRecord(
                        canonicalName,
                        command.getPetName(),
                        updatedKind,
                        canonicalOwnerName,
                        displayOwnerName,
                        command.getTextValue(),
                        likes,
                        age,
                        now
                );
            case ADD_LIKE:
                List<String> updatedLikes = new ArrayList<>(likes);
                String normalizedNewLike = normalizeFact(command.getTextValue());
                boolean alreadyStored = updatedLikes.stream().anyMatch(value ->
                        normalizeFact(value).equals(normalizedNewLike));
                if (!alreadyStored) {
                    updatedLikes.add(command.getTextValue());
                }
                return new PetMemoryRecord(
                        canonicalName,
                        command.getPetName(),
                        updatedKind,
                        canonicalOwnerName,
                        displayOwnerName,
                        favoriteFood,
                        updatedLikes,
                        age,
                        now
                );
            case SET_AGE:
                return new PetMemoryRecord(
                        canonicalName,
                        command.getPetName(),
                        updatedKind,
                        canonicalOwnerName,
                        displayOwnerName,
                        favoriteFood,
                        likes,
                        command.getNumberValue(),
                        now
                );
            default:
                throw new IllegalArgumentException("Command is not a pet update");
        }
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PetMemoryCommand.Kind getKind() {
        return kind;
    }

    public String getCanonicalOwnerName() {
        return canonicalOwnerName;
    }

    public String getDisplayOwnerName() {
        return displayOwnerName;
    }

    public String getFavoriteFood() {
        return favoriteFood;
    }

    public List<String> getLikes() {
        return likes;
    }

    public Integer getAge() {
        return age;
    }

    public long getUpdatedAtEpochMillis() {
        return updatedAtEpochMillis;
    }

    static String canonicalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    static String normalizeFact(String fact) {
        return canonicalize(fact);
    }
}
