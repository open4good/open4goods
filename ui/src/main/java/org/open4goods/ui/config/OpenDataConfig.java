package org.open4goods.ui.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenDataConfig {

    private int downloadSpeedKb;
    private int concurrentDownloads;
    private long initialDelay;
    private long fixedDelay;

    public int getDownloadSpeedKb() {
        return downloadSpeedKb;
    }

    public void setDownloadSpeedKb(int downloadSpeedKb) {
        this.downloadSpeedKb = downloadSpeedKb;
    }

    public int getConcurrentDownloads() {
        return concurrentDownloads;
    }

    public void setConcurrentDownloads(int concurrentDownloads) {
        this.concurrentDownloads = concurrentDownloads;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    public long getFixedDelay() {
        return fixedDelay;
    }

    public void setFixedDelay(long fixedDelay) {
        this.fixedDelay = fixedDelay;
    }
}
