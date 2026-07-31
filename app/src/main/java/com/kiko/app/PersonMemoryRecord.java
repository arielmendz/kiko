package com.kiko.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class PersonMemoryRecord {
    private final String canonicalName;
    private final String displayName;
    private final String favoriteFood;
    private final List<String> likes;
    private final Integer age;
    private final long updatedAtEpochMillis;

    PersonMemoryRecord(
            String canonicalName,
            String displayName,
            String favoriteFood,
            List<String> likes,
            Integer age,
            long updatedAtEpochMillis
    ) {
        this.canonicalName = Objects.requireNonNull(canonicalName);
        this.displayName = Objects.requireNonNull(displayName);
        this.favoriteFood = favoriteFood;
        this.likes = Collections.unmodifiableList(new ArrayList<>(likes));
        this.age = age;
        this.updatedAtEpochMillis = updatedAtEpochMillis;
    }

    static PersonMemoryRecord empty(String displayName, long now) {
        return new PersonMemoryRecord(
                canonicalizeName(displayName),
                displayName,
                null,
                Collections.emptyList(),
                null,
                now
        );
    }

    PersonMemoryRecord apply(PersonMemoryCommand command, long now) {
        switch (command.getType()) {
            case SET_FAVORITE_FOOD:
                return new PersonMemoryRecord(
                        canonicalName,
                        command.getPersonName(),
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
                return new PersonMemoryRecord(
                        canonicalName,
                        command.getPersonName(),
                        favoriteFood,
                        updatedLikes,
                        age,
                        now
                );
            case SET_AGE:
                return new PersonMemoryRecord(
                        canonicalName,
                        command.getPersonName(),
                        favoriteFood,
                        likes,
                        command.getNumberValue(),
                        now
                );
            default:
                throw new IllegalArgumentException("Command is not a memory update");
        }
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getDisplayName() {
        return displayName;
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

    public boolean hasFacts() {
        return favoriteFood != null || !likes.isEmpty() || age != null;
    }

    public static String canonicalizeName(String name) {
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        return normalized;
    }

    static String normalizeFact(String fact) {
        return Normalizer.normalize(fact.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}
