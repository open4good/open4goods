package org.open4goods.nudgerfrontapi.service.exposed;

import org.open4goods.nudgerfrontapi.config.properties.ExposedDocsProperties;
import org.springframework.stereotype.Service;

/**
 * Exposes security decisions for exposed docs endpoints.
 */
@Service("exposedDocsSecurity")
public class ExposedDocsSecurity
{

    private final ExposedDocsProperties properties;

    /**
     * Creates the security helper.
     *
     * @param properties exposed docs configuration
     */
    public ExposedDocsSecurity(ExposedDocsProperties properties)
    {
        this.properties = properties;
    }

    /**
     * Indicates whether public access is allowed for exposed docs endpoints.
     *
     * @return true when public access is enabled
     */
    public boolean isPublicAccess()
    {
        return properties.isPublicAccess();
    }
}
