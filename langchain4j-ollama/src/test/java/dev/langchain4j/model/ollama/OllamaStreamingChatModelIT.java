package dev.langchain4j.model.ollama;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;
import static dev.langchain4j.model.chat.request.ResponseFormat.JSON;
import static dev.langchain4j.model.ollama.OllamaImage.TINY_DOLPHIN_MODEL;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.TestStreamingChatResponseHandler;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * TODO: add more tests about ChatRequest and ChatRequestParameters
 */
class OllamaStreamingChatModelIT extends AbstractOllamaLanguageModelInfrastructure {

    static final String MODEL_NAME = TINY_DOLPHIN_MODEL;

    StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
            .baseUrl(ollamaBaseUrl(ollama))
            .modelName(MODEL_NAME)
            .temperature(0.0)
            .logRequests(true)
            .logResponses(true)
            .build();

    @Test
    void should_stream_answer() {

        // given
        String userMessage = "What is the capital of Germany?";

        // when
        TestStreamingChatResponseHandler handler = new TestStreamingChatResponseHandler();
        model.chat(userMessage, handler);
        ChatResponse response = handler.get();
        String answer = response.aiMessage().text();

        // then
        assertThat(answer).contains("Berlin");

        AiMessage aiMessage = response.aiMessage();
        assertThat(aiMessage.text()).isEqualTo(answer);
        assertThat(aiMessage.toolExecutionRequests()).isNull();

        ChatResponseMetadata metadata = response.metadata();

        assertThat(metadata.modelName()).isEqualTo(MODEL_NAME);

        TokenUsage tokenUsage = metadata.tokenUsage();
        assertThat(tokenUsage.inputTokenCount()).isGreaterThan(0);
        assertThat(tokenUsage.outputTokenCount()).isGreaterThan(0);
        assertThat(tokenUsage.totalTokenCount())
                .isEqualTo(tokenUsage.inputTokenCount() + tokenUsage.outputTokenCount());

        assertThat(metadata.finishReason()).isEqualTo(FinishReason.STOP);
    }

    @Test
    void should_respect_numPredict() {

        // given
        int numPredict = 1; // max output tokens

        StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .numPredict(numPredict)
                .temperature(0.0)
                .logRequests(true)
                .logResponses(true)
                .build();

        UserMessage userMessage = UserMessage.from("What is the capital of Germany?");

        // when
        TestStreamingChatResponseHandler handler = new TestStreamingChatResponseHandler();
        model.chat(singletonList(userMessage), handler);
        ChatResponse response = handler.get();
        String answer = response.aiMessage().text();

        // then
        assertThat(answer).doesNotContain("Berlin");
        assertThat(response.aiMessage().text()).isEqualTo(answer);

        ChatResponseMetadata metadata = response.metadata();
        assertThat(metadata.modelName()).isEqualTo(MODEL_NAME);
        assertThat(metadata.finishReason()).isEqualTo(FinishReason.LENGTH);
        assertThat(metadata.tokenUsage().outputTokenCount()).isBetween(numPredict, numPredict + 2); // bug in Ollama
    }

    @Test
    void should_respect_system_message() {

        // given
        SystemMessage systemMessage = SystemMessage.from("Translate messages from user into German");
        UserMessage userMessage = UserMessage.from("I love you");

        // when
        TestStreamingChatResponseHandler handler = new TestStreamingChatResponseHandler();
        model.chat(asList(systemMessage, userMessage), handler);
        dev.langchain4j.model.chat.response.ChatResponse response = handler.get();
        String answer = response.aiMessage().text();

        // then
        assertThat(answer).containsIgnoringCase("liebe");
        assertThat(response.aiMessage().text()).isEqualTo(answer);
        ChatResponseMetadata metadata = response.metadata();
        assertThat(metadata.modelName()).isEqualTo(MODEL_NAME);
        assertThat(metadata.finishReason()).isEqualTo(FinishReason.STOP);
    }

    @Test
    void should_respond_to_few_shot() {

        // given
        List<ChatMessage> messages = asList(
                UserMessage.from("1 + 1 ="),
                AiMessage.from(">>> 2"),
                UserMessage.from("2 + 2 ="),
                AiMessage.from(">>> 4"),
                UserMessage.from("4 + 4 ="));

        // when
        TestStreamingChatResponseHandler handler = new TestStreamingChatResponseHandler();
        model.chat(messages, handler);
        dev.langchain4j.model.chat.response.ChatResponse response = handler.get();
        String answer = response.aiMessage().text();

        // then
        assertThat(answer).startsWith(">>> 8");
        assertThat(response.aiMessage().text()).isEqualTo(answer);
        ChatResponseMetadata metadata = response.metadata();
        assertThat(metadata.modelName()).isEqualTo(MODEL_NAME);
        assertThat(metadata.finishReason()).isEqualTo(FinishReason.STOP);
    }

    @Test
    void should_generate_valid_json() {

        // given
        StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(MODEL_NAME)
                .responseFormat(JSON)
                .temperature(0.0)
                .build();

        String userMessage = "Return JSON with two fields: name and age of John Doe, 42 years old.";

        // when
        TestStreamingChatResponseHandler handler = new TestStreamingChatResponseHandler();
        model.chat(userMessage, handler);
        dev.langchain4j.model.chat.response.ChatResponse response = handler.get();
        String answer = response.aiMessage().text();

        // then
        assertThat(answer).isEqualToIgnoringWhitespace("{\"name\": \"John Doe\", \"age\": 42}");
        assertThat(response.aiMessage().text()).isEqualTo(answer);
        ChatResponseMetadata metadata = response.metadata();
        assertThat(metadata.modelName()).isEqualTo(MODEL_NAME);
        assertThat(metadata.finishReason()).isEqualTo(FinishReason.STOP);
    }

    @Test
    void should_propagate_failure_to_handler_onError() throws Exception {

        // given
        String wrongModelName = "banana";

        StreamingChatLanguageModel model = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(wrongModelName)
                .build();

        CompletableFuture<Throwable> future = new CompletableFuture<>();

        // when
        model.chat("does not matter", new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                future.completeExceptionally(new Exception("onPartialResponse() should never be called"));
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                future.completeExceptionally(new Exception("onCompleteResponse() should never be called"));
            }

            @Override
            public void onError(Throwable error) {
                future.complete(error);
            }
        });

        // then
        Throwable throwable = future.get();
        assertThat(throwable).isExactlyInstanceOf(HttpException.class);

        HttpException httpException = (HttpException) throwable;
        assertThat(httpException.statusCode()).isEqualTo(404);
        assertThat(httpException.getMessage()).contains("banana", "not found");
    }

    @Test
    void should_return_set_capabilities() {
        OllamaStreamingChatModel model = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl(ollama))
                .modelName(TINY_DOLPHIN_MODEL)
                .supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)
                .build();

        assertThat(model.supportedCapabilities()).contains(RESPONSE_FORMAT_JSON_SCHEMA);
    }
}
