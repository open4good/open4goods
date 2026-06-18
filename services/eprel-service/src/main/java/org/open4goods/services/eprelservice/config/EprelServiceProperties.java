package org.open4goods.services.eprelservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

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

    /**
     * Minimum number of alphanumeric characters a model candidate must contain to be used
     * in searches. Candidates with fewer characters are discarded as too generic.
     */
    @Min(1)
    private int minAlnumLength = 3;

    /**
     * Maximum number of results returned per Elasticsearch query. Guards against silent
     * truncation at the ES default of 10; increase cautiously on large indices.
     */
    @Min(1)
    private int maxSearchResults = 100;

    /**
     * List of EPREL product groups/categories to index. Empty means all.
     */
    private List<String> groupsToIndex = List.of();

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

    public int getMinAlnumLength()
    {
        return minAlnumLength;
    }

    public void setMinAlnumLength(int minAlnumLength)
    {
        this.minAlnumLength = minAlnumLength;
    }

    public int getMaxSearchResults()
    {
        return maxSearchResults;
    }

    public void setMaxSearchResults(int maxSearchResults)
    {
        this.maxSearchResults = maxSearchResults;
    }

    public List<String> getGroupsToIndex()
    {
        return groupsToIndex;
    }

    public void setGroupsToIndex(List<String> groupsToIndex)
    {
        this.groupsToIndex = groupsToIndex;
    }
}
