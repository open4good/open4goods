package org.open4goods.services.feedservice.model;

import java.util.UUID;

/**
 * In-memory statistics for one CSV feed indexing run.
 */
public class FeedIndexingJobStat {

    public static final String TYPE_CSV = "feed";

    private String id;
    private String datasource = "";
    private String url = "";
    private long startDate = 0;
    private long duration = 0;
    private long processed = 0;
    private long indexed = 0;
    private long validationFail = 0;
    private long exceptions = 0;
    private Boolean fail = false;
    private String type;

    public FeedIndexingJobStat() {
    }

    public FeedIndexingJobStat(String datasource, String url, String type) {
        this.startDate = System.currentTimeMillis();
        this.datasource = datasource;
        this.url = url;
        this.fail = false;
        this.id = UUID.randomUUID().toString();
        this.type = type;
    }

    public void terminate() {
        this.duration = System.currentTimeMillis() - startDate;
    }

    public void incrementLines() {
        processed++;
    }

    public void incrementErrors() {
        exceptions++;
    }

    public void incrementValidationFail() {
        validationFail++;
    }

    public void incrementIndexed() {
        indexed++;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getProcessed() {
        return processed;
    }

    public void setProcessed(long processed) {
        this.processed = processed;
    }

    public long getIndexed() {
        return indexed;
    }

    public void setIndexed(long indexed) {
        this.indexed = indexed;
    }

    public long getValidationFail() {
        return validationFail;
    }

    public void setValidationFail(long validationFail) {
        this.validationFail = validationFail;
    }

    public long getExceptions() {
        return exceptions;
    }

    public void setExceptions(long exceptions) {
        this.exceptions = exceptions;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Boolean getFail() {
        return fail;
    }

    public void setFail(Boolean fail) {
        this.fail = fail;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
