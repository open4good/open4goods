package org.open4goods.services.eprelservice.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.client.EprelApiClient;
import org.open4goods.services.eprelservice.client.EprelProductGroup;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
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
     * Downloads the latest catalogues and re-indexes the EPREL data set. A group that
     * fails does not prevent the following ones from being refreshed.
     *
     * @throws IOException if at least one group could not be refreshed
     */
    public void refreshCatalogue() throws IOException
    {
        List<EprelProductGroup> groups = apiClient.fetchProductGroups();
        if (groups.isEmpty())
        {
            LOGGER.warn("No EPREL product groups returned by the API");
            return;
        }

        List<String> groupsToIndex = properties.getGroupsToIndex();
        List<String> failedGroups = new ArrayList<>();
        for (EprelProductGroup group : groups)
        {
            if (groupsToIndex != null && !groupsToIndex.isEmpty() && !groupsToIndex.contains(group.urlCode()))
            {
                LOGGER.info("Skipping EPREL group {}", group.urlCode());
                continue;
            }
            try
            {
                processGroup(group);
            }
            catch (IOException | RuntimeException e)
            {
                LOGGER.error("Failed to process EPREL catalogue for group {} : {}", group.urlCode(), e.getMessage());
                failedGroups.add(group.urlCode());
            }
        }
        if (!failedGroups.isEmpty())
        {
            throw new IOException("EPREL catalogue refresh failed for groups " + failedGroups);
        }
    }


    public List<EprelProductGroup>  getCatalog () {

    	return apiClient.fetchProductGroups();
    }
    private void processGroup(EprelProductGroup group) throws IOException
    {
        Path zipPath = null;
        try
        {
            LOGGER.info("Downloading EPREL catalogue for {}", group.urlCode());
            zipPath = apiClient.downloadCatalogueZip(group.urlCode());
            List<EprelProduct> buffer = new ArrayList<>(properties.getIndexBulkSize());
            Set<String> seen = new HashSet<>();
            parser.parse(zipPath, product ->
            {
                seen.add(product.getEprelRegistrationNumber());
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
            markWithdrawn(group, seen);
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to process EPREL catalogue for group {}", group.urlCode(), e);
            throw e;
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

    /**
     * Flags the documents of a group that are missing from the catalogue just downloaded (GOU-1893). Called only
     * once the group was downloaded, parsed and indexed without error; an empty catalogue marks nothing, so a
     * truncated download cannot withdraw a whole group. Documents are kept (not deleted) so that the withdrawal
     * is dated and reversible: indexing the entry again clears the flag.
     *
     * @param group the refreshed group
     * @param seen registration numbers present in the new catalogue
     * @return the number of documents flagged
     */
    int markWithdrawn(EprelProductGroup group, Set<String> seen)
    {
        if (seen.isEmpty())
        {
            LOGGER.warn("EPREL group {} : empty catalogue, no document marked as withdrawn", group.urlCode());
            return 0;
        }
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t.field("productGroup").value(group.urlCode())))
                        .mustNot(m -> m.term(t -> t.field("withdrawn").value(true)))))
                .withSourceFilter(new org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder()
                        .withIncludes("eprelRegistrationNumber", "productGroup").build())
                .withPageable(org.springframework.data.domain.PageRequest.of(0, properties.getIndexBulkSize()))
                .build();
        long now = System.currentTimeMillis();
        List<UpdateQuery> updates = new ArrayList<>();
        int marked = 0;
        try (SearchHitsIterator<EprelProduct> hits = elasticsearchOperations.searchForStream(query, EprelProduct.class))
        {
            while (hits.hasNext())
            {
                EprelProduct stored = hits.next().getContent();
                String id = stored.getEprelRegistrationNumber();
                if (id == null || seen.contains(id) || !group.urlCode().equals(stored.getProductGroup()))
                {
                    continue;
                }
                updates.add(UpdateQuery.builder(id)
                        .withDocument(Document.create().append("withdrawn", true).append("withdrawnAt", now))
                        .build());
                marked++;
                if (updates.size() >= properties.getIndexBulkSize())
                {
                    bulkUpdate(updates);
                }
            }
            bulkUpdate(updates);
        }
        LOGGER.info("EPREL group {} : {} documents marked as withdrawn", group.urlCode(), marked);
        return marked;
    }

    private void bulkUpdate(List<UpdateQuery> updates)
    {
        if (updates.isEmpty())
        {
            return;
        }
        try
        {
            elasticsearchOperations.bulkUpdate(List.copyOf(updates), IndexCoordinates.of("eprel-products"));
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to mark EPREL products as withdrawn", e);
        }
        finally
        {
            updates.clear();
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
