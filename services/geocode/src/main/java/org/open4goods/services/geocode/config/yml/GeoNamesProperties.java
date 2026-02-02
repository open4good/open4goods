package org.open4goods.services.geocode.config.yml;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for loading GeoNames datasets.
 */
@ConfigurationProperties(prefix = "geocode.geonames")
public class GeoNamesProperties
{
    private String url = "https://download.geonames.org/export/dump/cities5000.zip";
    private int refreshInDays = 7;
    private String extractedFileName = "cities5000.txt";

    /**
     * Returns the remote GeoNames zip URL.
     *
     * @return GeoNames URL
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Sets the remote GeoNames zip URL.
     *
     * @param url GeoNames URL
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * Returns the refresh interval in days for cached downloads.
     *
     * @return refresh interval in days
     */
    public int getRefreshInDays()
    {
        return refreshInDays;
    }

    /**
     * Sets the refresh interval in days for cached downloads.
     *
     * @param refreshInDays refresh interval in days
     */
    public void setRefreshInDays(int refreshInDays)
    {
        this.refreshInDays = refreshInDays;
    }

    /**
     * Returns the expected extracted file name within the GeoNames archive.
     *
     * @return extracted file name
     */
    public String getExtractedFileName()
    {
        return extractedFileName;
    }

    /**
     * Sets the expected extracted file name within the GeoNames archive.
     *
     * @param extractedFileName extracted file name
     */
    public void setExtractedFileName(String extractedFileName)
    {
        this.extractedFileName = extractedFileName;
    }
}
