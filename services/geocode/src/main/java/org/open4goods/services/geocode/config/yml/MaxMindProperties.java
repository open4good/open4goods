package org.open4goods.services.geocode.config.yml;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the MaxMind GeoIP dataset.
 */
@ConfigurationProperties(prefix = "geocode.maxmind")
public class MaxMindProperties
{
    private String url = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-City&license_key=${MAXMIND_LICENSE_KEY}&suffix=tar.gz";
    private int refreshInDays = 7;
    private String databaseFileName = "GeoLite2-City.mmdb";

    /**
     * Returns the MaxMind dataset download URL.
     *
     * @return download URL
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Sets the MaxMind dataset download URL.
     *
     * @param url download URL
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
     * Returns the expected database filename inside the archive.
     *
     * @return database filename
     */
    public String getDatabaseFileName()
    {
        return databaseFileName;
    }

    /**
     * Sets the expected database filename inside the archive.
     *
     * @param databaseFileName database filename
     */
    public void setDatabaseFileName(String databaseFileName)
    {
        this.databaseFileName = databaseFileName;
    }
}
