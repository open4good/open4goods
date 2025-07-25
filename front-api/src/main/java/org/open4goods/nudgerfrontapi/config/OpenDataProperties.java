package org.open4goods.nudgerfrontapi.config;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for open data dataset locations and download limits.
 */
@Component
@ConfigurationProperties(prefix = "front.opendata")
public class OpenDataProperties {

    private String gtinZipPath = "./opendata/gtin.zip";
    private String isbnZipPath = "./opendata/isbn.zip";
    private int downloadSpeedKb = 256;
    private int concurrentDownloads = 4;

    public File gtinZipFile() {
        return new File(gtinZipPath);
    }

    public File isbnZipFile() {
        return new File(isbnZipPath);
    }

    public String getGtinZipPath() {
        return gtinZipPath;
    }

    public void setGtinZipPath(String gtinZipPath) {
        this.gtinZipPath = gtinZipPath;
    }

    public String getIsbnZipPath() {
        return isbnZipPath;
    }

    public void setIsbnZipPath(String isbnZipPath) {
        this.isbnZipPath = isbnZipPath;
    }

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
