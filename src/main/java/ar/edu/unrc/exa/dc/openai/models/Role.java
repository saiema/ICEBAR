package ar.edu.unrc.exa.dc.openai.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    USER,
    ASSISTANT,
    SYSTEM;

    @JsonCreator
    public static Role forValue(String value) {
        return Role.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

}
