package org.open4goods.model.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.open4goods.model.product.AiReviewHolder;

/**
 * DTO representing the status of a review generation process.
 */
public class ReviewGenerationStatus {

    /**
     * Possible states of a generation process.
     */
    public enum Status {
        PENDING,
        QUEUED,
        SEARCHING,
        FETCHING,
        ANALYSING,
        SUCCESS,
        PREPROCESSING,
        FAILED;
    }

    private long upc;
    private Status status;
    private Long startTime;
    private Long endTime;
    private AiReviewHolder result;
    private String errorMessage;
    private Integer percent = 0;

    /**
     * List of processing messages that track the internal state.
     */
    private List<String> messages = new ArrayList<>();

    /**
     * Duration of the review generation process in milliseconds.
     */
    private long duration;

    /**
     * Remaining time (in milliseconds) computed as estimatedTime - duration.
     */
    private long remaining;

    /**
     * The product GTIN associated with the generation request.
     */
    private String gtin;

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


    public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}



    public AiReviewHolder getResult() {
		return result;
	}

	public void setResult(AiReviewHolder result) {
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    public String getGtin() {
        return gtin;
    }

    public void setGtin(String gtin) {
        this.gtin = gtin;
    }




    // toString, equals, and hashCode

    public Integer getPercent() {
		return percent;
	}

	public void setPercent(Integer percent) {
		this.percent = percent;
	}

	@Override
    public String toString() {
        return "ReviewGenerationStatus{" +
                "upc=" + upc +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", result=" + result +
                ", errorMessage='" + errorMessage + '\'' +
                ", messages=" + messages +
                ", duration=" + duration +
                ", remaining=" + remaining +
                ", gtin='" + gtin + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(upc, status, startTime, endTime, result, errorMessage, messages, duration, remaining, gtin);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewGenerationStatus)) return false;
        ReviewGenerationStatus that = (ReviewGenerationStatus) o;
        return upc == that.upc &&
                duration == that.duration &&
                remaining == that.remaining &&
                status == that.status &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(result, that.result) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                Objects.equals(messages, that.messages) &&
                Objects.equals(gtin, that.gtin);
    }
}
