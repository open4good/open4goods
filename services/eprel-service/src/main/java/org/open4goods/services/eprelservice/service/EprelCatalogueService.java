package org.open4goods.services.eprelservice.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.client.EprelApiClient;
import org.open4goods.services.eprelservice.client.EprelProductGroup;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

/**
 * Coordinates catalogue retrieval and indexing into Elasticsearch.
 * TODO : HealthChecks, metrics
 */
@Service
public class EprelCatalogueService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EprelCatalogueService.class);

    private final EprelApiClient apiClient;
    private final EprelCatalogueParser parser;
    private final ElasticsearchOperations elasticsearchOperations;
    private final EprelServiceProperties properties;

    /**
     * Creates the service.
     *
     * @param apiClient              HTTP client used to interact with EPREL
     * @param parser                 parser converting catalogues to products
     * @param elasticsearchOperations Elasticsearch operations facade
     * @param properties             module configuration
     */
    public EprelCatalogueService(EprelApiClient apiClient, EprelCatalogueParser parser,
            ElasticsearchOperations elasticsearchOperations, EprelServiceProperties properties)
    {
        this.apiClient = apiClient;
        this.parser = parser;
        this.elasticsearchOperations = elasticsearchOperations;
        this.properties = properties;
    }

    /**
     * Downloads the latest catalogues and re-indexes the EPREL data set.
     */
    public void refreshCatalogue()
    {
        List<EprelProductGroup> groups = apiClient.fetchProductGroups();
        if (groups.isEmpty())
        {
            LOGGER.warn("No EPREL product groups returned by the API");
            return;
        }
        groups.forEach(group -> processGroup(group));
    }

    private void processGroup(EprelProductGroup group)
    {
        Path zipPath = null;
        try
        {
            LOGGER.info("Downloading EPREL catalogue for {}", group.urlCode());
            zipPath = apiClient.downloadCatalogueZip(group.urlCode());
            List<EprelProduct> buffer = new ArrayList<>(properties.getIndexBulkSize());
            parser.parse(zipPath, product ->
            {
                buffer.add(product);
                if (buffer.size() >= properties.getIndexBulkSize())
                {
                    flush(buffer);
                }
            });
            if (!buffer.isEmpty())
            {
                flush(buffer);
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to process EPREL catalogue for group {}", group.urlCode(), e);
        }
        finally
        {
            if (zipPath != null)
            {
                try
                {
                    Files.deleteIfExists(zipPath);
                }
                catch (IOException e)
                {
                    LOGGER.warn("Unable to delete temporary file {}", zipPath, e);
                }
            }
        }
    }

    private void flush(List<EprelProduct> buffer)
    {
        if (buffer.isEmpty())
        {
            return;
        }
        try
        {
            LOGGER.info("Indexing {} eprel products", buffer.size());
            elasticsearchOperations.save(buffer);
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to index EPREL products batch", e);
        }
        finally
        {
            buffer.clear();
        }
    }
}
