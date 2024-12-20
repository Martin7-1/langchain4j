package dev.langchain4j.store.embedding.mongodb;

import static dev.langchain4j.store.embedding.mongodb.MongoDbTestFixture.EMBEDDING_MODEL;
import static dev.langchain4j.store.embedding.mongodb.MongoDbTestFixture.createDefaultClient;

import com.mongodb.client.MongoClient;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreWithFilteringIT;
import org.junit.jupiter.api.AfterEach;

/**
 * TODO
 */
class MongoDbEmbeddingStoreHybridIT extends EmbeddingStoreWithFilteringIT {
    private MongoDbTestFixture fixture = new MongoDbTestFixture(createClient())
            .initializeWithConfiguration(MongoDbConfigurationHybrid.builder()
                    .createSearchIndex(true)
                    .searchIndexName("test_search_index")
                    .build());

    MongoClient createClient() {
        return createDefaultClient();
    }

    @Override
    protected EmbeddingStore<TextSegment> embeddingStore() {
        return fixture.getEmbeddingStore();
    }

    @Override
    protected EmbeddingModel embeddingModel() {
        return EMBEDDING_MODEL;
    }

    @AfterEach
    void afterEach() {
        fixture.afterTests();
    }
}
