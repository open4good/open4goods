package org.open4goods.api.dto.metriks;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetriksResponse {

    private String schemaVersion = "2.0";
    private MetriksPeriod period;
    private List<MetriksEvent> events;

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public MetriksPeriod getPeriod() {
        return period;
    }

    public void setPeriod(MetriksPeriod period) {
        this.period = period;
    }

    public List<MetriksEvent> getEvents() {
        return events;
    }

    public void setEvents(List<MetriksEvent> events) {
        this.events = events;
    }
}
