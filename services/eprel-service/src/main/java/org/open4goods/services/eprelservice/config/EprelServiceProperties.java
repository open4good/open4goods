package org.open4goods.services.eprelservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the EPREL integration.
 */
@ConfigurationProperties(prefix = "eprel")
@Component
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
    private int indexBulkSize = 250;

    /**
     * Maximum number of spaces allowed in alternative model identifiers when searching.
     */
    @Min(0)
    private int excludeIfSpaces = 2;

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

    public int getExcludeIfSpaces()
    {
        return excludeIfSpaces;
    }

    public void setExcludeIfSpaces(int excludeIfSpaces)
    {
        this.excludeIfSpaces = excludeIfSpaces;
    }
}
