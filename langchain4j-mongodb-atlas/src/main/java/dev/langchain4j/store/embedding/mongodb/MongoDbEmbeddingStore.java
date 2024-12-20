package dev.langchain4j.store.embedding.mongodb;

import static com.mongodb.client.model.SearchIndexType.vectorSearch;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.Utils.isNullOrEmpty;
import static dev.langchain4j.internal.Utils.randomUUID;
import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static dev.langchain4j.internal.ValidationUtils.ensureTrue;
import static dev.langchain4j.store.embedding.mongodb.IndexMapping.defaultIndexMapping;
import static dev.langchain4j.store.embedding.mongodb.MappingUtils.toMongoDbDocument;
import static dev.langchain4j.store.embedding.mongodb.MappingUtils.toVectorSearchFields;
import static dev.langchain4j.store.embedding.mongodb.MongoDbMetadataFilterMapper.map;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.SearchIndexModel;
import com.mongodb.client.model.SearchIndexType;
import com.mongodb.client.result.InsertManyResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a <a href="https://www.mongodb.com/">MongoDB</a> indexed collection as an embedding store.
 * <p>
 * More <a href="https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/">info</a>
 * on using MongoDb vector search.
 * <p>
 * <a href="https://www.mongodb.com/developer/products/atlas/semantic-search-mongodb-atlas-vector-search/">tutorial</a>
 * how to use vector search with MongoDB Atlas (great starting point).
 * <p>
 * To deploy a local instance of Atlas, see
 * <a href="https://www.mongodb.com/docs/atlas/cli/current/atlas-cli-deploy-local/">this guide</a>.
 * <p>
 * If you are using a free tier, {@code #createIndex = true} might not be supported,
 * so you will need to create an index manually.
 * In your Atlas web console go to: DEPLOYMENT -&gt; Database -&gt; {your cluster} -&gt; Atlas Search tab
 * -&gt; Create Index Search -&gt; "JSON Editor" under "Atlas Vector Search" (not "Atlas Search") -&gt; Next
 * -&gt; Select your database in the left pane -&gt; Insert the following JSON into the right pane
 * (set "numDimensions" and additional metadata fields to desired values)
 * <pre>
 * {
 *   "fields" : [ {
 *     "type" : "vector",
 *     "path" : "embedding",
 *     "numDimensions" : 384,
 *     "similarity" : "cosine"
 *   }, {
 *     "type" : "filter",
 *     "path" : "metadata.test-key"
 *   } ]
 * }
 * </pre>
 * -&gt; Next -&gt; Create Search Index
 */
public class MongoDbEmbeddingStore implements EmbeddingStore<TextSegment> {
    private static final int SECONDS_TO_WAIT_FOR_INDEX = 20;

    private static final Logger log = LoggerFactory.getLogger(MongoDbEmbeddingStore.class);

    private final MongoCollection<MongoDbDocument> collection;
    private final String vectorSearchIndexName;
    private final MongoDbConfiguration configuration;

    /**
     * Creates an instance of MongoDbEmbeddingStore.
     *
     * @param mongoClient             MongoDB client. Please close the client to release resources after usage.
     * @param databaseName            MongoDB database name.
     * @param collectionName          MongoDB collection name.
     * @param vectorSearchIndexName   MongoDB Atlas Vector Search Index name.
     * @param createCollectionOptions Options to create MongoDB collection.
     * @param indexMapping            MongoDB Atlas index mapping.
     * @param createVectorSearchIndex Whether to create Atlas Vector Search Index or not.
     * @param configuration           MongoDB configuration to use (ANN or ENN)
     */
    public MongoDbEmbeddingStore(
            MongoClient mongoClient,
            String databaseName,
            String collectionName,
            String vectorSearchIndexName,
            CreateCollectionOptions createCollectionOptions,
            IndexMapping indexMapping,
            Boolean createVectorSearchIndex,
            @NonNull MongoDbConfiguration configuration) {
        databaseName = ensureNotNull(databaseName, "databaseName");
        collectionName = ensureNotNull(collectionName, "collectionName");
        createVectorSearchIndex = getOrDefault(createVectorSearchIndex, false);
        this.vectorSearchIndexName = ensureNotNull(vectorSearchIndexName, "indexName");

        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder()
                .register(MongoDbDocument.class, MongoDbMatchedDocument.class)
                .build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        // create collection if not exist
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        if (!isCollectionExist(database, collectionName)) {
            createCollection(
                    database, collectionName, getOrDefault(createCollectionOptions, new CreateCollectionOptions()));
        }

        this.collection =
                database.getCollection(collectionName, MongoDbDocument.class).withCodecRegistry(codecRegistry);
        this.configuration = configuration;

        if (!indexExists(this.collection, this.vectorSearchIndexName)) {
            if (createVectorSearchIndex) {
                createIndex(
                        this.collection,
                        this.vectorSearchIndexName,
                        toVectorSearchFields(getOrDefault(indexMapping, defaultIndexMapping())),
                        vectorSearch());
            } else {
                throw new RuntimeException(String.format(
                        "Vector Search Index '%s' not found and must be created via builder().createVectorSearchIndex(true), or manually as a vector search index (not a regular index), via the createSearchIndexes command",
                        this.vectorSearchIndexName));
            }
        }

        // post-setup for configuration
        configuration.additionalSetup(this.collection);
    }

    @Deprecated
    public MongoDbEmbeddingStore(
            MongoClient mongoClient,
            String databaseName,
            String collectionName,
            String vectorSearchIndexName,
            Long maxResultRatio,
            CreateCollectionOptions createCollectionOptions,
            Bson filter,
            IndexMapping indexMapping,
            Boolean createIndex) {
        // For backward compatibility, default to MongoDbConfigurationANN
        this(
                mongoClient,
                databaseName,
                collectionName,
                vectorSearchIndexName,
                createCollectionOptions,
                indexMapping,
                createIndex,
                MongoDbConfigurationANN.builder()
                        .maxResultRatio(maxResultRatio)
                        .filter(filter)
                        .build());
        log.warn("Please setting configuration manually");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private MongoClient mongoClient;
        private String databaseName;
        private String collectionName;
        private String vectorSearchIndexName;
        private Long maxResultRatio;
        private CreateCollectionOptions createCollectionOptions;
        private Bson filter;
        private IndexMapping indexMapping;
        private MongoDbConfiguration configuration;
        /**
         * Whether MongoDB Atlas is deployed in cloud
         *
         * <p>if true, you need to create Atlas Vector Search Index in <a href="https://cloud.mongodb.com/">MongoDB Atlas</a></p>
         * <p>if false, {@link MongoDbEmbeddingStore} will create collection and index automatically</p>
         */
        private Boolean createVectorSearchIndex;

        /**
         * Build Mongo Client, Please close the client to release resources after usage
         */
        public Builder fromClient(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
            return this;
        }

        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }

        public Builder collectionName(String collectionName) {
            this.collectionName = collectionName;
            return this;
        }

        /**
         * Set the name of Atlas Vector Search Index.
         *
         * @param indexName The name of Atlas Vector Search Index
         * @return builder
         * @see <a href="https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/">Atlas Vector Search Index</a>
         * @deprecated Using {@link Builder#vectorSearchIndexName(String)} instead for clearer semantics.
         */
        @Deprecated(forRemoval = true)
        public Builder indexName(String indexName) {
            this.vectorSearchIndexName = indexName;
            return this;
        }

        /**
         * Set the name of Atlas Vector Search Index.
         *
         * @param vectorSearchIndexName The name of Atlas Vector Search Index
         * @return builder
         * @see <a href="https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/">Atlas Vector Search Index</a>
         */
        public Builder vectorSearchIndexName(String vectorSearchIndexName) {
            this.vectorSearchIndexName = vectorSearchIndexName;
            return this;
        }

        /**
         * Ratio to get nearest neighbors to using during the search. Only using in ANN search.
         *
         * <p>e.g. If {@code maxResultRatio} is 10, {@code maxResults} in {@link EmbeddingSearchRequest} is 10, then Mongo will search in 100 (10 * 10) candidates and get final 10 results.</p>
         *
         * @param maxResultRatio ratio to choose final search results.
         * @return builder
         * @deprecated Using {@link Builder#configuration(MongoDbConfiguration)} to manually set search configuration instead.
         */
        @Deprecated(forRemoval = true)
        public Builder maxResultRatio(Long maxResultRatio) {
            this.maxResultRatio = maxResultRatio;
            return this;
        }

        public Builder createCollectionOptions(CreateCollectionOptions createCollectionOptions) {
            this.createCollectionOptions = createCollectionOptions;
            return this;
        }

        /**
         * Document query filter, all fields included in filter must be contained in {@link IndexMapping#metadataFieldNames}
         *
         * <p>For example:</p>
         *
         * <ul>
         *     <li>AND filter: Filters.and(Filters.in("type", asList("TXT", "md")), Filters.eqFull("test-key", "test-value"))</li>
         *     <li>OR filter: Filters.or(Filters.in("type", asList("TXT", "md")), Filters.eqFull("test-key", "test-value"))</li>
         * </ul>
         *
         * @param filter document query filter
         * @return builder
         * @deprecated Using {@link Builder#configuration(MongoDbConfiguration)} to manually set search configuration instead.
         */
        @Deprecated(forRemoval = true)
        public Builder filter(Bson filter) {
            this.filter = filter;
            return this;
        }

        /**
         * set MongoDB search index fields mapping
         *
         * <p>if {@link Builder#createVectorSearchIndex} is true, then indexMapping not work</p>
         *
         * @param indexMapping MongoDB search index fields mapping
         * @return builder
         */
        public Builder indexMapping(IndexMapping indexMapping) {
            this.indexMapping = indexMapping;
            return this;
        }

        /**
         * Whether to create Atlas Vector Search Index or not.
         *
         * <p>If you are in production code, we recommend you to set it to false and create Atlas Vector Search Index manually.</p>
         *
         * <p>default value is false</p>
         *
         * @param createIndex Whether to create Atlas Vector Search Index or not.
         * @return builder
         * @see <a href="https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/">Atlas Vector Search Index</a>
         * @deprecated Using {@link Builder#createVectorSearchIndex(Boolean)} instead for clearer semantics.
         */
        @Deprecated(forRemoval = true)
        public Builder createIndex(Boolean createIndex) {
            this.createVectorSearchIndex = createIndex;
            return this;
        }

        /**
         * Whether to create Atlas Vector Search Index or not.
         *
         * <p>If you are in production code, we recommend you to set it to false and create Atlas Vector Search Index manually.</p>
         *
         * <p>default value is false</p>
         *
         * @param createVectorSearchIndex Whether to create Atlas Vector Search Index or not.
         * @return builder
         * @see <a href="https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/">Atlas Vector Search Index</a>
         */
        public Builder createVectorSearchIndex(Boolean createVectorSearchIndex) {
            this.createVectorSearchIndex = createVectorSearchIndex;
            return this;
        }

        /**
         * MongoDb search configuration, langchain4j-mongodb-atlas support:
         *
         * <p>see <a href="https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/">Mongo document</a> for more details</p>
         *
         * <ul>
         *     <li>{@link MongoDbConfigurationANN}</li>
         *     <li>{@link MongoDbConfigurationENN}</li>
         * </ul>
         *
         * @param configuration MongoDb search configuration
         * @return builder
         */
        public Builder configuration(MongoDbConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public MongoDbEmbeddingStore build() {
            if (configuration != null) {
                return new MongoDbEmbeddingStore(
                        mongoClient,
                        databaseName,
                        collectionName,
                        vectorSearchIndexName,
                        createCollectionOptions,
                        indexMapping,
                        createVectorSearchIndex,
                        configuration);
            }
            // For backward compatibility
            return new MongoDbEmbeddingStore(
                    mongoClient,
                    databaseName,
                    collectionName,
                    vectorSearchIndexName,
                    maxResultRatio,
                    createCollectionOptions,
                    filter,
                    indexMapping,
                    createVectorSearchIndex);
        }
    }

    @Override
    public String add(Embedding embedding) {
        String id = randomUUID();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = randomUUID();
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = embeddings.stream().map(ignored -> randomUUID()).collect(toList());
        addAll(ids, embeddings, null);
        return ids;
    }

    @Override
    public void removeAll() {
        collection.deleteMany(Filters.empty());
    }

    @Override
    public void removeAll(Collection<String> ids) {
        ensureNotEmpty(ids, "ids");
        collection.deleteMany(Filters.in("_id", ids));
    }

    @Override
    public void removeAll(Filter filter) {
        ensureNotNull(filter, "filter");
        collection.deleteMany(map(filter));
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        try {
            AggregateIterable<MongoDbMatchedDocument> results =
                    configuration.internalSearch(collection, vectorSearchIndexName, request);
            List<EmbeddingMatch<TextSegment>> result = StreamSupport.stream(results.spliterator(), false)
                    .map(MappingUtils::toEmbeddingMatch)
                    .collect(toList());
            return new EmbeddingSearchResult<>(result);
        } catch (MongoCommandException e) {
            log.error("Error in MongoDBEmbeddingStore.search", e);
            throw new RuntimeException(e);
        }
    }

    private void addInternal(String id, Embedding embedding, TextSegment embedded) {
        addAll(singletonList(id), singletonList(embedding), embedded == null ? null : singletonList(embedded));
    }

    @Override
    public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> embedded) {
        if (isNullOrEmpty(ids) || isNullOrEmpty(embeddings)) {
            log.info("do not add empty embeddings to MongoDB Atlas");
            return;
        }
        ensureTrue(ids.size() == embeddings.size(), "ids size is not equal to embeddings size");
        ensureTrue(
                embedded == null || embeddings.size() == embedded.size(),
                "embeddings size is not equal to embedded size");

        List<MongoDbDocument> documents = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            MongoDbDocument document =
                    toMongoDbDocument(id, embeddings.get(i), embedded == null ? null : embedded.get(i));
            documents.add(document);
        }

        InsertManyResult result = collection.insertMany(documents);
        if (!result.wasAcknowledged()) {
            String errMsg = String.format("[MongoDbEmbeddingStore] Add document failed, Document=%s", documents);
            log.error(errMsg);
            throw new RuntimeException(errMsg);
        }
    }

    private boolean isCollectionExist(MongoDatabase database, String collectionName) {
        return StreamSupport.stream(database.listCollectionNames().spliterator(), false)
                .anyMatch(collectionName::equals);
    }

    private void createCollection(
            MongoDatabase database, String collectionName, CreateCollectionOptions createCollectionOptions) {
        database.createCollection(collectionName, createCollectionOptions);
    }

    /**
     * Check whether the index exists or not.
     *
     * <p>package-private for reuse.</p>
     *
     * @param indexName Index name to check exist.
     * @return true if the index exists, false otherwise.
     */
    static boolean indexExists(MongoCollection<MongoDbDocument> collection, String indexName) {
        Document indexRecord = indexRecord(collection, indexName);
        return indexRecord != null && !indexRecord.getString("status").equals("DOES_NOT_EXIST");
    }

    private static Document indexRecord(MongoCollection<MongoDbDocument> collection, String indexName) {
        return StreamSupport.stream(collection.listSearchIndexes().spliterator(), false)
                .filter(index -> indexName.equals(index.getString("name")))
                .findAny()
                .orElse(null);
    }

    /**
     * Create index (Atlas Search Index or Atlas Vector Search Index).
     *
     * @param indexName       Index name to create.
     * @param mapping         mapping definition.
     * @param searchIndexType Index type. (Vector Search or Search)
     */
    static void createIndex(
            MongoCollection<MongoDbDocument> collection,
            String indexName,
            Document mapping,
            SearchIndexType searchIndexType) {
        collection.createSearchIndexes(List.of(new SearchIndexModel(indexName, mapping, searchIndexType)));

        waitForIndex(collection, indexName);
    }

    static void waitForIndex(MongoCollection<MongoDbDocument> collection, String indexName) {
        long startTime = System.nanoTime();
        long timeoutNanos = TimeUnit.SECONDS.toNanos(SECONDS_TO_WAIT_FOR_INDEX);
        while (System.nanoTime() - startTime < timeoutNanos) {
            Document indexRecord = indexRecord(collection, indexName);
            if (indexRecord != null) {
                if ("FAILED".equals(indexRecord.getString("status"))) {
                    throw new RuntimeException("Search index has failed status.");
                }
                if (indexRecord.getBoolean("queryable")) {
                    return;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        log.warn(
                "Index {} was not created or did not exit INITIAL_SYNC within {} seconds",
                indexName,
                SECONDS_TO_WAIT_FOR_INDEX);
    }
}
