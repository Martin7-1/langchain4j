package dev.langchain4j.model.qianfan.client;

import java.util.Objects;

public final class Usage {

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    public Usage() {
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public boolean equals(Object another) {
        if (this == another) {
            return true;
        } else {
            return another instanceof Usage && this.equalTo((Usage) another);
        }
    }

    private boolean equalTo(Usage another) {
        return Objects.equals(this.promptTokens, another.promptTokens) && Objects.equals(this.completionTokens, another.completionTokens) && Objects.equals(this.totalTokens, another.totalTokens);
    }

    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Objects.hashCode(this.promptTokens);
        h += (h << 5) + Objects.hashCode(this.completionTokens);
        h += (h << 5) + Objects.hashCode(this.totalTokens);
        return h;
    }

    public String toString() {
        return "Usage{promptTokens=" + this.promptTokens + ", completionTokens=" + this.completionTokens + ", totalTokens=" + this.totalTokens + "}";
    }
}

