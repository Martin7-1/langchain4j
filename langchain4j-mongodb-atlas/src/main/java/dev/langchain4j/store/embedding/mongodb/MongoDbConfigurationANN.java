package dev.langchain4j.store.embedding.mongodb;

import static com.mongodb.client.model.search.VectorSearchOptions.approximateVectorSearchOptions;
import static dev.langchain4j.internal.Utils.getOrDefault;

import com.mongodb.client.model.search.VectorSearchOptions;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.bson.conversions.Bson;

/**
 * Represents ANN(Approximate Nearest Neighbor) search using Atlas Vector Search Index.
 *
 * <p>For more details, see <a href="https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/">MongoDB docs</a></p>
 */
public class MongoDbConfigurationANN implements MongoDbConfiguration {

    private final long maxResultRatio;
    private final Bson globalPrefilter;

    public MongoDbConfigurationANN(Long maxResultRatio, Bson globalPrefilter) {
        this.maxResultRatio = getOrDefault(maxResultRatio, 10L);
        this.globalPrefilter = globalPrefilter;
    }

    @Override
    public VectorSearchOptions vectorSearchOptions(EmbeddingSearchRequest request) {
        long numCandidates = request.maxResults() * maxResultRatio;

        return this.globalPrefilter == null
                ? approximateVectorSearchOptions(numCandidates)
                : approximateVectorSearchOptions(numCandidates).filter(this.globalPrefilter);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Long maxResultRatio;
        private Bson filter;

        public Builder maxResultRatio(Long maxResultRatio) {
            this.maxResultRatio = maxResultRatio;
            return this;
        }

        public Builder filter(Bson filter) {
            this.filter = filter;
            return this;
        }

        public MongoDbConfigurationANN build() {
            return new MongoDbConfigurationANN(maxResultRatio, filter);
        }
    }
}
