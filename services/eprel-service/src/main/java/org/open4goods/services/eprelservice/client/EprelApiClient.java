package org.open4goods.services.eprelservice.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Contract for EPREL API interactions.
 */
public interface EprelApiClient
{
    /**
     * Retrieves the list of product groups available from EPREL.
     *
     * @return list of product groups
     */
    List<EprelProductGroup> fetchProductGroups();

    /**
     * Downloads the catalogue associated with the provided group.
     *
     * @param urlCode the group identifier used by the download endpoint
     * @return path to a temporary ZIP file containing the catalogue
     * @throws IOException if the file cannot be written locally
     */
    Path downloadCatalogueZip(String urlCode) throws IOException;
}
