package org.open4goods.services.feedservice.config;

/**
 * Runtime options for CSV feed indexing.
 */
public class FeedIndexingProperties {

    private int concurrentFetcherTask = 1;

    private int workerPauseDurationMs = 4000;

    public int getConcurrentFetcherTask() {
        return concurrentFetcherTask;
    }

    public void setConcurrentFetcherTask(int concurrentFetcherTask) {
        this.concurrentFetcherTask = concurrentFetcherTask;
    }

    public int getWorkerPauseDurationMs() {
        return workerPauseDurationMs;
    }

    public void setWorkerPauseDurationMs(int workerPauseDurationMs) {
        this.workerPauseDurationMs = workerPauseDurationMs;
    }
}
