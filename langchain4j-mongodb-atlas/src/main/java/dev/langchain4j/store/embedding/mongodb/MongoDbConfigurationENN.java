package dev.langchain4j.store.embedding.mongodb;

import static com.mongodb.client.model.search.VectorSearchOptions.exactVectorSearchOptions;

import com.mongodb.client.model.search.VectorSearchOptions;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.bson.conversions.Bson;

public class MongoDbConfigurationENN implements MongoDbConfiguration {

    private final Bson globalPrefilter;

    public MongoDbConfigurationENN(Bson globalPrefilter) {
        this.globalPrefilter = globalPrefilter;
    }

    @Override
    public VectorSearchOptions vectorSearchOptions(EmbeddingSearchRequest request) {
        return this.globalPrefilter == null
                ? exactVectorSearchOptions()
                : exactVectorSearchOptions().filter(this.globalPrefilter);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Bson filter;

        public Builder filter(Bson filter) {
            this.filter = filter;
            return this;
        }

        public MongoDbConfigurationENN build() {
            return new MongoDbConfigurationENN(filter);
        }
    }
}
