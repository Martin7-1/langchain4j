package dev.langchain4j.model.qianfan.client.embedding;

import dev.langchain4j.model.qianfan.client.Usage;

import java.util.List;

public final class EmbeddingResponse {

    private String object;
    private String id;
    private Integer created;
    private List<EmbeddingData> data;
    private Usage usage;
    private String errorCode;
    private String errorMsg;

    public EmbeddingResponse() {
    }

    public String getObject() {
        return object;
    }

    public String getId() {
        return id;
    }

    public Integer getCreated() {
        return created;
    }

    public Usage getUsage() {
        return usage;
    }

    public List<EmbeddingData> getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    @Override
    public String toString() {
        return "EmbeddingResponse{" +
                "object='" + object + '\'' +
                ", id='" + id + '\'' +
                ", created=" + created +
                ", data=" + data +
                ", usage=" + usage +
                '}';
    }
}

