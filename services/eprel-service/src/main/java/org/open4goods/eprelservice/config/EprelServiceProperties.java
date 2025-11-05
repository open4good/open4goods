package org.open4goods.eprelservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the EPREL integration.
 */
@ConfigurationProperties(prefix = "open4goods.eprel")
public class EprelServiceProperties
{
    /**
     * Base URL of the EPREL API.
     */
    @NotBlank
    private String apiUrl = "https://eprel.ec.europa.eu/api";

    /**
     * API key required to download product catalogues.
     */
    @NotBlank
    private String apiKey = "DUMMY KEY";

    /**
     * Number of days between two catalogue synchronisations.
     */
    @Min(1)
    private int schedulingFrequencyDays = 2;

    /**
     * Number of products sent to Elasticsearch in a single bulk request.
     */
    @Min(1)
    private int indexBulkSize = 100;

    public String getApiUrl()
    {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl)
    {
        this.apiUrl = apiUrl;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    public int getSchedulingFrequencyDays()
    {
        return schedulingFrequencyDays;
    }

    public void setSchedulingFrequencyDays(int schedulingFrequencyDays)
    {
        this.schedulingFrequencyDays = schedulingFrequencyDays;
    }

    public int getIndexBulkSize()
    {
        return indexBulkSize;
    }

    public void setIndexBulkSize(int indexBulkSize)
    {
        this.indexBulkSize = indexBulkSize;
    }
}
