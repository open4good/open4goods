package org.open4goods.services.opendata.config;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.Localisable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for the OpenData service.
 */
@Configuration
@ConfigurationProperties(prefix = "open-data-config")
public class OpenDataConfig {

    /**
     * Maximum download speed per client in kilobytes.
     */
    private int downloadSpeedKb = 512;

    /**
     * Maximum concurrent downloads allowed.
     */
    private int concurrentDownloads = 4;

    /**
     * Flag indicating whether scheduled generation is allowed.
     */
    private Boolean generationEnabled = false;

    /**
     * Root folder where OpenData assets are stored.
     */
    private String rootFolder = File.separator + "opt" + File.separator + "open4goods" + File.separator;

    /**
     * Sub-folder storing OpenData exports.
     */
    private String openDataFolder = "opendata";

    private String openDataArchiveFilename = "full.zip";
    private String isbnFilename = "isbn.zip";
    private String tmpIsbnFilename = "isbn-tmp.zip";
    private String gtinFilename = "gtin.zip";
    private String tmpGtinFilename = "gtin-tmp.zip";

    public OpenDataConfig() {
		super();
	}

	/**
     * Base URLs used to build product links in the export.
     */
    private Localisable<String, String> baseUrls = new Localisable<>();

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



    public Boolean getGenerationEnabled() {
		return generationEnabled;
	}

	public void setGenerationEnabled(Boolean generationEnabled) {
		this.generationEnabled = generationEnabled;
	}

	public boolean isGenerationEnabled() {
        return Boolean.TRUE.equals(generationEnabled);
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public String getOpenDataFolder() {
        return openDataFolder;
    }

    public void setOpenDataFolder(String openDataFolder) {
        this.openDataFolder = openDataFolder;
    }

    public String getOpenDataArchiveFilename() {
        return openDataArchiveFilename;
    }

    public void setOpenDataArchiveFilename(String openDataArchiveFilename) {
        this.openDataArchiveFilename = openDataArchiveFilename;
    }

    public String getIsbnFilename() {
        return isbnFilename;
    }

    public void setIsbnFilename(String isbnFilename) {
        this.isbnFilename = isbnFilename;
    }

    public String getTmpIsbnFilename() {
        return tmpIsbnFilename;
    }

    public void setTmpIsbnFilename(String tmpIsbnFilename) {
        this.tmpIsbnFilename = tmpIsbnFilename;
    }

    public String getGtinFilename() {
        return gtinFilename;
    }

    public void setGtinFilename(String gtinFilename) {
        this.gtinFilename = gtinFilename;
    }

    public String getTmpGtinFilename() {
        return tmpGtinFilename;
    }

    public void setTmpGtinFilename(String tmpGtinFilename) {
        this.tmpGtinFilename = tmpGtinFilename;
    }

    public Localisable<String, String> getBaseUrls() {
        return baseUrls;
    }

    public void setBaseUrls(Localisable<String, String> baseUrls) {
        this.baseUrls = baseUrls;
    }

    public Path openDataDirectory() {
        return Paths.get(rootFolder).resolve(openDataFolder);
    }

    public File openDataFile() {
        return openDataDirectory().resolve(openDataArchiveFilename).toFile();
    }

    public File isbnZipFile() {
        return openDataDirectory().resolve(isbnFilename).toFile();
    }

    public File tmpIsbnZipFile() {
        return openDataDirectory().resolve(tmpIsbnFilename).toFile();
    }

    public File gtinZipFile() {
        return openDataDirectory().resolve(gtinFilename).toFile();
    }

    public File tmpGtinZipFile() {
        return openDataDirectory().resolve(tmpGtinFilename).toFile();
    }

    /**
     * Resolve the base URL for a given locale, falling back to the default entry when available.
     *
     * @param locale the requested locale
     * @return the base URL or {@code null} when none configured
     */
    public String getBaseUrl(Locale locale) {
        if (baseUrls == null || baseUrls.isEmpty()) {
            return null;
        }
        String languageKey = locale.getLanguage();
        String url = baseUrls.get(languageKey);
        if (StringUtils.isBlank(url)) {
            url = baseUrls.get("default");
        }
        return url;
    }
}
