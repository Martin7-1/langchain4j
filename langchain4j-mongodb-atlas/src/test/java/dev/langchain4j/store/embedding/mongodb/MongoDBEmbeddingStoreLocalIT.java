package dev.langchain4j.store.embedding.mongodb;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIT;
import lombok.SneakyThrows;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDBEmbeddingStoreLocalIT extends EmbeddingStoreIT {

    static final String MONGO_SERVICE_NAME = "mongo";
    static final Integer MONGO_SERVICE_PORT = 27778;
    static DockerComposeContainer<?> mongodb = new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yml"))
            .withStartupTimeout(Duration.ofMinutes(10))
            .withExposedService(MONGO_SERVICE_NAME, MONGO_SERVICE_PORT, Wait.defaultWaitStrategy());

    static MongoClient client;

    EmbeddingStore<TextSegment> embeddingStore = MongoDBEmbeddingStore.builder()
            .fromClient(client)
            .databaseName("test_database")
            .collectionName("test_collection")
            .indexName("test_index")
            .build();

    EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

    @BeforeAll
    @SneakyThrows
    static void start() {
        mongodb.start();

        MongoCredential credential = MongoCredential.createCredential("root", "admin", "root".toCharArray());
        client = MongoClients.create(
                MongoClientSettings.builder()
                        .credential(credential)
                        .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
                        .applyConnectionString(new ConnectionString(String.format("mongodb://%s:%s/?directConnection=true",
                                mongodb.getServiceHost(MONGO_SERVICE_NAME, MONGO_SERVICE_PORT), mongodb.getServicePort(MONGO_SERVICE_NAME, MONGO_SERVICE_PORT))))
                        .build());
    }

    @AfterAll
    static void stop() {
        mongodb.stop();
        client.close();
    }

    @Override
    protected EmbeddingStore<TextSegment> embeddingStore() {
        return embeddingStore;
    }

    @Override
    protected EmbeddingModel embeddingModel() {
        return embeddingModel;
    }

    @Override
    protected void clearStore() {
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder()
                .register(MongoDBDocument.class, MongoDBMatchedDocument.class)
                .build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);

        MongoCollection<MongoDBDocument> collection = client.getDatabase("test_database")
                .getCollection("test_collection", MongoDBDocument.class)
                .withCodecRegistry(codecRegistry);

        Bson filter = Filters.exists("embedding");
        collection.deleteMany(filter);
    }

    @Override
    @SneakyThrows
    protected void awaitUntilPersisted() {
        Thread.sleep(2000);
    }
}
