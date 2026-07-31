package com.kiko.app;

import java.util.Objects;

public final class PersonMemoryCommand {
    public enum Type {
        SET_FAVORITE_FOOD,
        ADD_LIKE,
        SET_AGE,
        QUERY_LIKES,
        QUERY_SUMMARY,
        QUERY_FAVORITE_FOOD
    }

    private final Type type;
    private final String personName;
    private final String textValue;
    private final Integer numberValue;

    private PersonMemoryCommand(
            Type type,
            String personName,
            String textValue,
            Integer numberValue
    ) {
        this.type = Objects.requireNonNull(type);
        this.personName = Objects.requireNonNull(personName);
        this.textValue = textValue;
        this.numberValue = numberValue;
    }

    static PersonMemoryCommand textUpdate(
            Type type,
            String personName,
            String value
    ) {
        return new PersonMemoryCommand(type, personName, value, null);
    }

    static PersonMemoryCommand ageUpdate(String personName, int age) {
        return new PersonMemoryCommand(Type.SET_AGE, personName, null, age);
    }

    static PersonMemoryCommand query(Type type, String personName) {
        return new PersonMemoryCommand(type, personName, null, null);
    }

    public Type getType() {
        return type;
    }

    public String getPersonName() {
        return personName;
    }

    public String getTextValue() {
        return textValue;
    }

    public Integer getNumberValue() {
        return numberValue;
    }

    public boolean isUpdate() {
        return type == Type.SET_FAVORITE_FOOD
                || type == Type.ADD_LIKE
                || type == Type.SET_AGE;
    }
}
