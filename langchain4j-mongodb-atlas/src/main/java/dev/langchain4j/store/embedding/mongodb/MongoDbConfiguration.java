package dev.langchain4j.store.embedding.mongodb;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.vectorSearch;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Projections.metaVectorSearchScore;
import static com.mongodb.client.model.search.SearchPath.fieldPath;
import static dev.langchain4j.store.embedding.mongodb.MongoDbMetadataFilterMapper.map;
import static java.util.stream.Collectors.toList;

import com.mongodb.MongoCommandException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.search.VectorSearchOptions;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import java.util.ArrayList;
import java.util.List;
import org.bson.conversions.Bson;

/**
 * Represent MongoDB Atlas search configuration. For now, langchain4j support following configuration:
 *
 * <ul>
 *     <li>{@link MongoDbConfigurationANN}</li>
 *     <li>{@link MongoDbConfigurationENN}</li>
 * </ul>
 * <p>
 * Users can implement this interface to support custom search configuration.
 */
public interface MongoDbConfiguration {

    /**
     * Performing vector search on MongoDB collection.
     *
     * @param collection MongoDB collection.
     * @param indexName  Index name for searching. Currently, the index represents Atlas Vector Search Index.
     * @param request    Search request.
     * @return MongoDB searched results.
     * @throws MongoCommandException if MongoDB failed to obtain searched results.
     */
    default AggregateIterable<MongoDbMatchedDocument> internalSearch(
            MongoCollection<MongoDbDocument> collection, String indexName, EmbeddingSearchRequest request)
            throws MongoCommandException {
        List<Double> queryVector = request.queryEmbedding().vectorAsList().stream()
                .map(Float::doubleValue)
                .collect(toList());

        Bson postFilter = null;
        if (request.minScore() > 0) {
            postFilter = Filters.gte("score", request.minScore());
        }
        if (request.filter() != null) {
            Bson newFilter = map(request.filter());
            postFilter = postFilter == null ? newFilter : Filters.and(postFilter, newFilter);
        }

        VectorSearchOptions vectorSearchOptions = vectorSearchOptions(request);

        ArrayList<Bson> pipeline = new ArrayList<>();
        pipeline.add(vectorSearch(
                fieldPath("embedding"), queryVector, indexName, request.maxResults(), vectorSearchOptions));
        pipeline.add(project(fields(metaVectorSearchScore("score"), include("embedding", "metadata", "text"))));
        if (postFilter != null) {
            Bson match = match(postFilter);
            pipeline.add(match);
        }

        return collection.aggregate(pipeline, MongoDbMatchedDocument.class);
    }

    /**
     * Obtain vector search options based on the request.
     *
     * @param request Search request.
     * @return Vector search options.
     */
    default VectorSearchOptions vectorSearchOptions(EmbeddingSearchRequest request) {
        throw new UnsupportedOperationException("This configuration do not have proper vector search options.");
    }
}
