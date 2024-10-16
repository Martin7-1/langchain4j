package dev.langchain4j.model.qianfan.client.chat;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    FUNCTION("function");

    @JsonValue
    private final String stringValue;

    Role(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    @Override
    public String toString() {
        return this.stringValue;
    }

    static Role from(String stringValue) {
        Role[] roles = values();

        for (Role role : roles) {
            if (role.stringValue.equals(stringValue)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown role: '" + stringValue + "'");
    }
}

