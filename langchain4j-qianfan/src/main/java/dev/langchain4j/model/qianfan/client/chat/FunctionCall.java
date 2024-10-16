package dev.langchain4j.model.qianfan.client.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.model.qianfan.client.Json;

import java.util.Map;
import java.util.Objects;

public class FunctionCall {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private String name;
    private String thoughts;
    private String arguments;

    public FunctionCall() {
    }

    private FunctionCall(Builder builder) {
        this.name = builder.name;
        this.thoughts = builder.thoughts;
        this.arguments = builder.arguments;
    }

    public String getName() {
        return this.name;
    }

    public String getThoughts() {
        return thoughts;
    }

    public String getArguments() {
        return this.arguments;
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> argumentsAsMap() {
        return (Map<String, T>) Json.fromJson(this.arguments, MAP_TYPE);
    }

    public <T> T argument(String name) {
        Map<String, T> arguments = this.argumentsAsMap();
        return arguments.get(name);
    }

    public boolean equals(Object another) {
        if (this == another) {
            return true;
        } else {
            return another instanceof FunctionCall
                    && this.equalTo((FunctionCall) another);
        }
    }

    private boolean equalTo(FunctionCall another) {
        return Objects.equals(this.name, another.name) && Objects.equals(this.arguments, another.arguments) && Objects.equals(this.thoughts, another.thoughts);
    }

    @Override
    public String toString() {
        return "{" +
                "name='" + name + '\'' +
                ", thoughts='" + thoughts + '\'' +
                ", arguments='" + arguments + '\'' +
                '}';
    }

    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(this.name);
        h += (h << 5) + Objects.hashCode(this.arguments);
        h += (h << 5) + Objects.hashCode(this.thoughts);
        return h;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String name;
        private String arguments;
        private String thoughts;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder arguments(String arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder thoughts(String thoughts) {
            this.thoughts = thoughts;
            return this;
        }

        public FunctionCall build() {
            return new FunctionCall(this);
        }
    }
}

