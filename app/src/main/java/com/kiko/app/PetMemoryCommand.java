package com.kiko.app;

import java.util.Objects;

public final class PetMemoryCommand {
    public enum Type {
        REGISTER,
        SET_FAVORITE_FOOD,
        ADD_LIKE,
        SET_AGE,
        QUERY_LIKES,
        QUERY_SUMMARY,
        QUERY_FAVORITE_FOOD,
        QUERY_OWNER_PETS
    }

    public enum Kind {
        GATO("gato", "un gato", "el gato"),
        GATA("gata", "una gata", "la gata"),
        PERRO("perro", "un perro", "el perro"),
        PERRA("perra", "una perra", "la perra");

        private final String word;
        private final String indefinitePhrase;
        private final String definitePhrase;

        Kind(String word, String indefinitePhrase, String definitePhrase) {
            this.word = word;
            this.indefinitePhrase = indefinitePhrase;
            this.definitePhrase = definitePhrase;
        }

        String getWord() {
            return word;
        }

        String getIndefinitePhrase() {
            return indefinitePhrase;
        }

        String getDefinitePhrase() {
            return definitePhrase;
        }

        String getCalledWord() {
            return this == GATA || this == PERRA ? "llamada" : "llamado";
        }

        static Kind fromDescriptor(String descriptor) {
            String normalized = descriptor.trim().toLowerCase();
            for (Kind kind : values()) {
                if (normalized.equals(kind.word)
                        || normalized.equals(kind.indefinitePhrase)
                        || normalized.equals(kind.definitePhrase)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private final Type type;
    private final String petName;
    private final Kind kind;
    private final String ownerName;
    private final String textValue;
    private final Integer numberValue;

    private PetMemoryCommand(
            Type type,
            String petName,
            Kind kind,
            String ownerName,
            String textValue,
            Integer numberValue
    ) {
        this.type = Objects.requireNonNull(type);
        this.petName = petName;
        this.kind = kind;
        this.ownerName = ownerName;
        this.textValue = textValue;
        this.numberValue = numberValue;
    }

    static PetMemoryCommand register(
            String petName,
            Kind kind,
            String ownerName
    ) {
        return new PetMemoryCommand(
                Type.REGISTER,
                Objects.requireNonNull(petName),
                Objects.requireNonNull(kind),
                ownerName,
                null,
                null
        );
    }

    static PetMemoryCommand textUpdate(
            Type type,
            String petName,
            Kind kind,
            String value
    ) {
        return new PetMemoryCommand(type, petName, kind, null, value, null);
    }

    static PetMemoryCommand ageUpdate(String petName, Kind kind, int age) {
        return new PetMemoryCommand(
                Type.SET_AGE,
                petName,
                kind,
                null,
                null,
                age
        );
    }

    static PetMemoryCommand petQuery(Type type, String petName, Kind kind) {
        return new PetMemoryCommand(type, petName, kind, null, null, null);
    }

    static PetMemoryCommand ownerQuery(String ownerName) {
        return new PetMemoryCommand(
                Type.QUERY_OWNER_PETS,
                null,
                null,
                ownerName,
                null,
                null
        );
    }

    public Type getType() {
        return type;
    }

    public String getPetName() {
        return petName;
    }

    public Kind getKind() {
        return kind;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getTextValue() {
        return textValue;
    }

    public Integer getNumberValue() {
        return numberValue;
    }

    public boolean isUpdate() {
        return type == Type.REGISTER
                || type == Type.SET_FAVORITE_FOOD
                || type == Type.ADD_LIKE
                || type == Type.SET_AGE;
    }
}
