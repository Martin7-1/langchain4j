package dev.langchain4j.store.embedding.mongodb;

import static dev.langchain4j.internal.Utils.isNullOrEmpty;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;

class MappingUtils {

    private MappingUtils() throws InstantiationException {
        throw new InstantiationException("Can't instantiate this class");
    }

    static MongoDbDocument toMongoDbDocument(String id, Embedding embedding, TextSegment textSegment) {
        boolean hasTextSegment = textSegment != null;
        return MongoDbDocument.builder()
                .id(id)
                .embedding(embedding.vectorAsList())
                .text(hasTextSegment ? textSegment.text() : null)
                .metadata(hasTextSegment ? textSegment.metadata().toMap() : null)
                .build();
    }

    static EmbeddingMatch<TextSegment> toEmbeddingMatch(MongoDbMatchedDocument matchedDocument) {
        TextSegment textSegment = null;
        if (matchedDocument.getText() != null) {
            textSegment = matchedDocument.getMetadata() == null
                    ? TextSegment.from(matchedDocument.getText())
                    : TextSegment.from(matchedDocument.getText(), Metadata.from(matchedDocument.getMetadata()));
        }
        return new EmbeddingMatch<>(
                matchedDocument.getScore(),
                matchedDocument.getId(),
                Embedding.from(matchedDocument.getEmbedding()),
                textSegment);
    }

    static Document toVectorSearchFields(IndexMapping indexMapping) {
        List<Document> list = new ArrayList<>();
        list.add(new Document()
                .append("type", "vector")
                .append("path", "embedding")
                .append("numDimensions", indexMapping.getDimension())
                .append("similarity", "cosine"));
        Set<String> metadataFields = indexMapping.getMetadataFieldNames();
        if (metadataFields != null && !metadataFields.isEmpty()) {
            metadataFields.forEach(field -> {
                list.add(new Document().append("type", "filter").append("path", "metadata." + field));
            });
        }
        return new Document("fields", list);
    }

    static Document toSearchFields(Map<String, String> metadataMapping) {
        // ref: https://www.mongodb.com/docs/atlas/atlas-search/define-field-mappings/#std-label-static-dynamic-mappings
        Document mapping = new Document();
        mapping.append("dynamic", false);
        Document fields = new Document();
        fields.append("metadata", mappingMetadata(metadataMapping));
        mapping.append("fields", fields);

        return new Document("mappings", mapping);
    }

    private static Document mappingMetadata(Map<String, String> metadataMapping) {
        Document metadata = new Document();
        metadata.append("type", "document");

        if (isNullOrEmpty(metadataMapping)) {
            metadata.append("dynamic", true);
            return metadata;
        }

        metadata.append("dynamic", false);
        metadataMapping.forEach(metadata::append);

        return metadata;
    }
}
