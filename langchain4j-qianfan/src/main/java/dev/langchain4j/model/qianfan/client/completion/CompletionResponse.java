package dev.langchain4j.model.qianfan.client.completion;

import dev.langchain4j.model.qianfan.client.Usage;
import dev.langchain4j.model.qianfan.client.chat.FunctionCall;

public final class CompletionResponse {

    private String id;
    private Integer errorCode;
    private String errorMsg;
    private String object;
    private Integer created;
    private Integer sentenceId;
    private Boolean isEnd;
    private Boolean isTruncated;
    private String result;
    private String finishReason;
    private Boolean needClearHistory;
    private Integer banRound;
    private Usage usage;
    private FunctionCall functionCall;

    public CompletionResponse() {
    }

    @Override
    public String toString() {
        return "CompletionResponse{" +
                "id='" + id + '\'' +
                ", errorCode=" + errorCode +
                ", errorMsg='" + errorMsg + '\'' +
                ", object='" + object + '\'' +
                ", created=" + created +
                ", sentenceId=" + sentenceId +
                ", isEnd=" + isEnd +
                ", isTruncated=" + isTruncated +
                ", result='" + result + '\'' +
                ", finishReason='" + finishReason + '\'' +
                ", needClearHistory=" + needClearHistory +
                ", banRound=" + banRound +
                ", usage=" + usage +
                ", functionCall=" + functionCall +
                '}';
    }

    public String getId() {
        return id;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public String getObject() {
        return object;
    }

    public Integer getCreated() {
        return created;
    }

    public Integer getSentenceId() {
        return sentenceId;
    }

    public Boolean getEnd() {
        return isEnd;
    }

    public Boolean getTruncated() {
        return isTruncated;
    }

    public String getResult() {
        return result;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public Boolean getNeedClearHistory() {
        return needClearHistory;
    }

    public Integer getBanRound() {
        return banRound;
    }

    public Usage getUsage() {
        return usage;
    }

    public FunctionCall getFunctionCall() {
        return functionCall;
    }
}
