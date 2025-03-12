package org.open4goods.services.reviewgeneration.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.open4goods.model.ai.AiReview;

/**
 * DTO representing the status of a review generation process.
 */
public class ProcessStatus {

    /**
     * Possible states of a generation process.
     */
    public enum Status {
        PENDING, QUEUED, PROCESSING, SUCCESS, FAILED;
    }
    
    private long upc;
    private Status status;
    private Instant startTime;
    private Instant endTime;
    private AiReview result;
    private String errorMessage;
    
    /**
     * List of processing messages that track the internal state.
     */
    private List<String> messages = new ArrayList<>();

    // Getters and setters

    public long getUpc() {
        return upc;
    }

    public void setUpc(long upc) {
        this.upc = upc;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public AiReview getResult() {
        return result;
    }

    public void setResult(AiReview result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
    
    /**
     * Appends a new message to the process status messages.
     *
     * @param message the message to add.
     */
    public void addMessage(String message) {
        this.messages.add(message);
    }

    // toString, equals, and hashCode

    @Override
    public String toString() {
        return "ProcessStatus{" +
                "upc=" + upc +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", result=" + result +
                ", errorMessage='" + errorMessage + '\'' +
                ", messages=" + messages +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(upc, status, startTime, endTime, result, errorMessage, messages);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessStatus)) return false;
        ProcessStatus that = (ProcessStatus) o;
        return upc == that.upc &&
                status == that.status &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(result, that.result) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(messages, that.messages);
    }
}
