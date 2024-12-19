package dev.langchain4j.store.embedding.mongodb;

import static com.mongodb.client.model.search.VectorSearchOptions.exactVectorSearchOptions;

import com.mongodb.client.model.search.VectorSearchOptions;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import org.bson.conversions.Bson;

/**
 * Represents ENN(Exact Nearest Neighbor) search using Atlas Vector Search Index.
 *
 * <p><b>NOTE: ENN search is computationally intensive and might negatively impact query latency.</b> So MongoDB recommend it for the following use cases:</p>
 *
 * <ul>
 *     <li>You want to determine the recall and accuracy of your ANN query using the ideal, exact results for the ENN query.</li>
 *     <li>You want to query less than 10000 documents without having to tune the number of nearest neighbors to consider.</li>
 *     <li>Your want to include selective pre-filters in your query against collections where less than 5% of your data meets the given pre-filter.</li>
 * </ul>
 *
 * <p>For more details, see <a href="https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/">MongoDB docs</a></p>
 */
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
