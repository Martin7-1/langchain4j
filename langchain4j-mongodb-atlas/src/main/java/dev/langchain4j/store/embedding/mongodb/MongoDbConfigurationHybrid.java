package dev.langchain4j.store.embedding.mongodb;

import static com.mongodb.client.model.SearchIndexType.search;
import static com.mongodb.client.model.search.VectorSearchOptions.exactVectorSearchOptions;
import static dev.langchain4j.store.embedding.mongodb.MappingUtils.toSearchFields;
import static dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore.createIndex;
import static dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore.indexExists;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.search.VectorSearchOptions;
import dev.langchain4j.Experimental;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import java.util.Map;

/**
 * TODO
 */
@Experimental
public class MongoDbConfigurationHybrid implements MongoDbConfiguration {

    private final String searchIndexName;
    private final boolean createSearchIndex;
    private final Map<String, String> metadataMapping;

    public MongoDbConfigurationHybrid(
            String searchIndexName, boolean createSearchIndex, Map<String, String> metadataMapping) {
        this.searchIndexName = searchIndexName;
        this.createSearchIndex = createSearchIndex;
        this.metadataMapping = metadataMapping;
    }

    @Override
    public VectorSearchOptions vectorSearchOptions(EmbeddingSearchRequest request) {
        // TODO: override it or internalSearch?
        return exactVectorSearchOptions();
    }

    @Override
    public void additionalSetup(MongoCollection<MongoDbDocument> collection) {
        if (!indexExists(collection, searchIndexName)) {
            if (createSearchIndex) {
                createIndex(collection, searchIndexName, toSearchFields(metadataMapping), search());
            } else {
                throw new RuntimeException(String.format(
                        "Search Index '%s' not found and must be created via builder().createSearchIndex(true), or manually as a vector search index (a regular index), via the createSearchIndexes command",
                        searchIndexName));
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String searchIndexName;
        private boolean createSearchIndex;
        private Map<String, String> metadataMapping;

        public Builder searchIndexName(String searchIndexName) {
            this.searchIndexName = searchIndexName;
            return this;
        }

        public Builder createSearchIndex(boolean createSearchIndex) {
            this.createSearchIndex = createSearchIndex;
            return this;
        }

        public Builder metadataMapping(Map<String, String> metadataMapping) {
            this.metadataMapping = metadataMapping;
            return this;
        }

        public MongoDbConfigurationHybrid build() {
            return new MongoDbConfigurationHybrid(searchIndexName, createSearchIndex, metadataMapping);
        }
    }
}
