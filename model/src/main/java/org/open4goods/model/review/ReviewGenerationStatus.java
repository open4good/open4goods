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
        ALREADY_PROCESSED,
        FAILED;
    }

    /**
     * Streaming progress events for review generation.
     */
    public enum ProgressEventType {
        STARTED,
        SEARCHING,
        STREAM_CHUNK,
        COMPLETED,
        ERROR
    }

    /**
     * Event entry representing a progress update.
     *
     * @param type      the event type
     * @param message   the associated message
     * @param chunk     the streamed content chunk (if any)
     * @param timestamp the event timestamp in epoch milliseconds
     */
    public record ReviewGenerationEvent(ProgressEventType type, String message, String chunk, long timestamp) {
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
     * List of progress events emitted during streaming.
     */
    private List<ReviewGenerationEvent> events = new ArrayList<>();

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

    public List<ReviewGenerationEvent> getEvents() {
        return events;
    }

    public void setEvents(List<ReviewGenerationEvent> events) {
        this.events = events;
    }

    /**
     * Appends a new message to the process status messages.
     *
     * @param message the message to add.
     */
    public void addMessage(String message) {
        this.messages.add(message);
    }

    /**
     * Appends a new progress event to the status.
     *
     * @param type    the event type
     * @param message the event message
     * @param chunk   the streamed chunk (if any)
     */
    public void addEvent(ProgressEventType type, String message, String chunk) {
        this.events.add(new ReviewGenerationEvent(type, message, chunk, System.currentTimeMillis()));
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
                ", events=" + events +
                ", duration=" + duration +
                ", remaining=" + remaining +
                ", gtin='" + gtin + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(upc, status, startTime, endTime, result, errorMessage, messages, events, duration, remaining,
                gtin);
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
                Objects.equals(events, that.events) &&
                Objects.equals(gtin, that.gtin);
    }
}
