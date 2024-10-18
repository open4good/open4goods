package org.open4goods.ui.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenDataConfig {

    private int downloadSpeedKb;
    private int concurrentDownloads;

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

  
}
