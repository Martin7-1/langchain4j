package dev.langchain4j.model.qianfan.client.chat;

import dev.langchain4j.model.qianfan.client.Usage;

public final class ChatCompletionResponse {

    private String id;
    private Integer errorCode;
    private String errorMsg;
    private String object;
    private Integer created;
    private Integer sentenceId;
    private Boolean isEnd;
    private Boolean isTruncated;
    private String result;
    private Boolean needClearHistory;
    private Integer banRound;
    private Usage usage;
    private FunctionCall functionCall;
    private String finishReason;

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

    public Boolean getNeedClearHistory() {
        return needClearHistory;
    }

    public Usage getUsage() {
        return usage;
    }

    public Integer getBanRound() {
        return banRound;
    }

    public FunctionCall getFunctionCall() {
        return functionCall;
    }

    public String getFinishReason() {
        return finishReason;
    }

    @Override
    public String toString() {
        return "ChatCompletionResponse{" +
                "id='" + id + '\'' +
                ", errorCode=" + errorCode +
                ", errorMsg='" + errorMsg + '\'' +
                ", object='" + object + '\'' +
                ", created=" + created +
                ", sentenceId=" + sentenceId +
                ", isEnd=" + isEnd +
                ", isTruncated=" + isTruncated +
                ", result='" + result + '\'' +
                ", needClearHistory=" + needClearHistory +
                ", banRound=" + banRound +
                ", usage=" + usage +
                ", functionCall=" + functionCall +
                ", finishReason='" + finishReason + '\'' +
                '}';
    }
}

