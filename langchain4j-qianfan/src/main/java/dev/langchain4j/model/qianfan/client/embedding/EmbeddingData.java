package dev.langchain4j.model.qianfan.client.embedding;


import java.util.List;

public final class EmbeddingData {

    private String object;
    private List<Float> embedding;
    private Integer index;

    public EmbeddingData() {
    }

    public List<Float> getEmbedding() {
        return this.embedding;
    }

    public Integer getIndex() {
        return this.index;
    }

    public String getObject() {
        return this.object;
    }

    @Override
    public String toString() {
        return "EmbeddingData{" +
                "object='" + object + '\'' +
                ", embedding=" + embedding +
                ", index=" + index +
                '}';
    }
}

