package dev.langchain4j.data.document.source.aliyun.oss;

import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.Metadata;

import java.io.InputStream;

import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.lang.String.format;

public class AliyunOssSource implements DocumentSource {

    public static final String SOURCE = "source";

    private final InputStream inputStream;
    private final String bucket;
    private final String key;

    public AliyunOssSource(InputStream inputStream, String bucket, String key) {
        this.inputStream = ensureNotNull(inputStream, "inputStream");
        this.bucket = ensureNotBlank(bucket, "bucket");
        this.key = ensureNotBlank(key, "key");
    }

    @Override
    public InputStream inputStream() {
        return inputStream;
    }

    @Override
    public Metadata metadata() {
        return Metadata.from(SOURCE, format("oss://%s/%s", bucket, key));
    }
}
