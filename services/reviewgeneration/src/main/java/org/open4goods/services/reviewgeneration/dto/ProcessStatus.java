package org.open4goods.services.reviewgeneration.dto;

import java.time.Instant;
import java.util.Objects;

/**
 * DTO representing the status of a review generation process.
 */
public class ProcessStatus {

    /**
     * Possible states of a generation process.
     */
    public enum Status {
        PENDING, PROCESSING, SUCCESS, FAILED;
    }
    
    private long upc;
    private Status status;
    private Instant startTime;
    private Instant endTime;
    private String result;
    private String errorMessage;

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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // toString, equals, and hashCode

    @Override
    public String toString() {
        return "ProcessStatus{" +
                "upc=" + upc +
                ", status=" + status +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", result='" + result + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(upc, status, startTime, endTime, result, errorMessage);
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
                Objects.equals(errorMessage, that.errorMessage);
    }
}
