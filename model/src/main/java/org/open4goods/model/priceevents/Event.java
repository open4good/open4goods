package org.open4goods.model.priceevents;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Commercial event definition exposed to client applications.
 *
 * <p>
 * The object is intentionally mutable to allow Spring Boot configuration
 * binding. Dates are serialised using the {@code yyyy-MM-dd} ISO pattern so
 * that clients can safely parse them regardless of locale.
 * </p>
 */
public class Event {

    /** Human readable event label (already localised). */
    private String label;

    /** Event start date, formatted as {@code yyyy-MM-dd}. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** Event end date, formatted as {@code yyyy-MM-dd}. */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** CSS compatible colour hint used by the frontend. */
    private String color;

    /**
     * Return the human readable label for the event.
     *
     * @return localised label, never {@code null} once configured
     */
    public String getLabel() {
        return label;
    }

    /**
     * Update the human readable label for the event.
     *
     * @param label new label value
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Return the start date of the event.
     *
     * @return start date using ISO-8601 format
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Update the start date of the event.
     *
     * @param startDate new start date
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Return the end date of the event.
     *
     * @return end date using ISO-8601 format
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Update the end date of the event.
     *
     * @param endDate new end date
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Return the colour hint associated with the event.
     *
     * @return CSS compatible colour string
     */
    public String getColor() {
        return color;
    }

    /**
     * Update the colour hint associated with the event.
     *
     * @param color CSS compatible colour string
     */
    public void setColor(String color) {
        this.color = color;
    }
}
