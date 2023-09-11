package dev.langchain4j.store.embedding.redis;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

/**
 * TODO
 * Represents a <a href="https://www.elastic.co/">Elasticsearch</a> index as an embedding store.
 * Current implementation assumes the index uses the cosine distance metric.
 * To use ElasticsearchEmbeddingStore, please add the "langchain4j-elasticsearch" dependency to your project.
 */
public class RedisEmbeddingStore implements EmbeddingStore<TextSegment> {

    @Override
    public String add(Embedding embedding) {
        return null;
    }

    @Override
    public void add(String id, Embedding embedding) {

    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        return null;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return null;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        return null;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        return null;
    }
}
