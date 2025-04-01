package dev.langchain4j.model.ollama;

import static dev.langchain4j.data.message.ContentType.IMAGE;
import static dev.langchain4j.data.message.ContentType.TEXT;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.model.chat.request.json.JsonSchemaElementHelper.toMap;
import static dev.langchain4j.model.ollama.OllamaJsonUtils.fromJson;
import static dev.langchain4j.model.ollama.OllamaJsonUtils.toJson;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElementHelper;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class InternalOllamaHelper {

    private static final Predicate<ChatMessage> isUserMessage = UserMessage.class::isInstance;
    private static final Predicate<UserMessage> hasImages =
            userMessage -> userMessage.contents().stream().anyMatch(content -> IMAGE.equals(content.type()));

    static List<Message> toOllamaMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(message -> isUserMessage.test(message) && hasImages.test((UserMessage) message)
                        ? messagesWithImageSupport((UserMessage) message)
                        : otherMessages(message))
                .collect(Collectors.toList());
    }

    static List<Tool> toOllamaTools(List<ToolSpecification> toolSpecifications) {
        if (toolSpecifications == null) {
            return null;
        }
        return toolSpecifications.stream()
                .map(toolSpecification -> Tool.builder()
                        .function(Function.builder()
                                .name(toolSpecification.name())
                                .description(toolSpecification.description())
                                .parameters(toOllamaParameters(toolSpecification))
                                .build())
                        .build())
                .collect(Collectors.toList());
    }

    private static Parameters toOllamaParameters(ToolSpecification toolSpecification) {
        if (toolSpecification.parameters() != null) {
            JsonObjectSchema parameters = toolSpecification.parameters();
            return Parameters.builder()
                    .properties(toMap(parameters.properties()))
                    .required(parameters.required())
                    .build();
        } else {
            return null;
        }
    }

    static List<ToolExecutionRequest> toToolExecutionRequests(List<ToolCall> toolCalls) {
        return toolCalls.stream()
                .map(toolCall -> ToolExecutionRequest.builder()
                        .name(toolCall.getFunction().getName())
                        .arguments(toJson(toolCall.getFunction().getArguments()))
                        .build())
                .toList();
    }

    static String toOllamaResponseFormat(ResponseFormat responseFormat) {
        if (responseFormat == null || responseFormat == ResponseFormat.TEXT) {
            return null;
        } else if (responseFormat == ResponseFormat.JSON && responseFormat.jsonSchema() == null) {
            return "json";
        } else {
            return toJson(
                    JsonSchemaElementHelper.toMap(responseFormat.jsonSchema().rootElement()));
        }
    }

    static FinishReason toFinishReason(String reason) {
        if (reason == null) {
            return null;
        }

        return switch (reason) {
            case "stop" -> FinishReason.STOP;
            case "length" -> FinishReason.LENGTH;
            default -> FinishReason.OTHER;
        };
    }

    static void validate(OllamaChatRequestParameters parameters) {
        if (parameters.frequencyPenalty() != null) {
            throw new IllegalArgumentException("'frequencyPenalty' parameter is not suppoerted by Ollama");
        }
        if (parameters.presencePenalty() != null) {
            throw new IllegalArgumentException("'presencePenalty' parameter is not suppoerted by Ollama");
        }
        if (parameters.toolChoice() != null) {
            throw new IllegalArgumentException("'toolChoice' parameter is not suppoerted by Ollama");
        }
        if (parameters.maxOutputTokens() != null && parameters.numPredict() != null) {
            throw new IllegalArgumentException(
                    "'maxOutputTokens' parameter and 'numPredict' parameter can't be declared at the same time");
        }
    }

    static AiMessage aiMessageFrom(OllamaChatResponse ollamaChatResponse) {
        return ollamaChatResponse.getMessage().getToolCalls() != null
                ? AiMessage.from(
                        toToolExecutionRequests(ollamaChatResponse.getMessage().getToolCalls()))
                : AiMessage.from(ollamaChatResponse.getMessage().getContent());
    }

    static ChatResponseMetadata chatResponseMetadataFrom(OllamaChatResponse ollamaChatResponse) {
        return chatResponseMetadataFrom(
                ollamaChatResponse.getModel(),
                toFinishReason(ollamaChatResponse.getDoneReason()),
                new TokenUsage(ollamaChatResponse.getPromptEvalCount(), ollamaChatResponse.getEvalCount()));
    }

    static OllamaChatRequest toOllamaChatRequest(ChatRequest chatRequest) {
        OllamaChatRequestParameters requestParameters = (OllamaChatRequestParameters) chatRequest.parameters();
        return OllamaChatRequest.builder()
                .model(requestParameters.modelName())
                .messages(toOllamaMessages(chatRequest.messages()))
                .options(Options.builder()
                        .mirostat(requestParameters.mirostat())
                        .mirostatEta(requestParameters.mirostatEta())
                        .mirostatTau(requestParameters.mirostatTau())
                        .repeatLastN(requestParameters.repeatLastN())
                        .temperature(requestParameters.temperature())
                        .topK(requestParameters.topK())
                        .topP(requestParameters.topP())
                        .repeatPenalty(requestParameters.repeatPenalty())
                        .seed(requestParameters.seed())
                        // numPredict and maxOutputTokens are semantically identical
                        .numPredict(getOrDefault(requestParameters.maxOutputTokens(), requestParameters.numPredict()))
                        .numCtx(requestParameters.numCtx())
                        .stop(requestParameters.stopSequences())
                        .minP(requestParameters.minP())
                        .build())
                .format(toOllamaResponseFormat(requestParameters.responseFormat()))
                .stream(false)
                .tools(toOllamaTools(chatRequest.toolSpecifications()))
                .build();
    }

    static ChatResponseMetadata chatResponseMetadataFrom(
            String modelName, FinishReason finishReason, TokenUsage tokenUsage) {
        return ChatResponseMetadata.builder()
                .modelName(modelName)
                .finishReason(finishReason)
                .tokenUsage(tokenUsage)
                .build();
    }

    private static Message messagesWithImageSupport(UserMessage userMessage) {
        Map<ContentType, List<Content>> groupedContents =
                userMessage.contents().stream().collect(Collectors.groupingBy(Content::type));

        if (groupedContents.get(TEXT).size() != 1) {
            throw new RuntimeException("Expecting single text content, but got: " + userMessage.contents());
        }

        String text = ((TextContent) groupedContents.get(TEXT).get(0)).text();

        List<ImageContent> imageContents = groupedContents.get(IMAGE).stream()
                .map(content -> (ImageContent) content)
                .collect(Collectors.toList());

        return Message.builder()
                .role(toOllamaRole(userMessage.type()))
                .content(text)
                .images(ImageUtils.base64EncodeImageList(imageContents))
                .build();
    }

    private static Message otherMessages(ChatMessage chatMessage) {
        if (chatMessage instanceof CustomMessage customMessage) {
            return Message.builder()
                    .additionalFields(customMessage.attributes())
                    .build();
        }

        List<ToolCall> toolCalls = null;
        if (chatMessage instanceof AiMessage aiMessage) {
            List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
            toolCalls = Optional.ofNullable(toolExecutionRequests)
                    .map(reqs -> reqs.stream()
                            .map(toolExecutionRequest -> {
                                TypeReference<HashMap<String, Object>> typeReference =
                                        new TypeReference<HashMap<String, Object>>() {};
                                FunctionCall functionCall = FunctionCall.builder()
                                        .name(toolExecutionRequest.name())
                                        .arguments(fromJson(toolExecutionRequest.arguments(), typeReference))
                                        .build();
                                return ToolCall.builder().function(functionCall).build();
                            })
                            .collect(Collectors.toList()))
                    .orElse(null);
        }
        return Message.builder()
                .role(toOllamaRole(chatMessage.type()))
                .content(toText(chatMessage))
                .toolCalls(toolCalls)
                .build();
    }

    private static String toText(ChatMessage chatMessage) {
        if (chatMessage instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        } else if (chatMessage instanceof UserMessage userMessage) {
            return userMessage.singleText();
        } else if (chatMessage instanceof AiMessage aiMessage) {
            return aiMessage.text();
        } else if (chatMessage instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
            return toolExecutionResultMessage.text();
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + chatMessage.type());
        }
    }

    private static Role toOllamaRole(ChatMessageType chatMessageType) {
        return switch (chatMessageType) {
            case SYSTEM -> Role.SYSTEM;
            case USER -> Role.USER;
            case AI -> Role.ASSISTANT;
            case TOOL_EXECUTION_RESULT -> Role.TOOL;
            default -> throw new IllegalArgumentException("Unknown ChatMessageType: " + chatMessageType);
        };
    }
}
