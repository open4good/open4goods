package org.open4goods.api.controller.api;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;

/**
 * Resolution of a vertical identifier taken from a path, shared by the controllers (GOU-2306).
 */
final class KnownVertical {

    private KnownVertical() {
    }

    /**
     * @param verticalsConfigService source of the vertical configurations
     * @param vertical vertical identifier from the path
     * @return the configuration of that vertical
     * @throws ResourceNotFoundException when no vertical has this identifier (404, GOU-2280)
     */
    static VerticalConfig of(VerticalsConfigService verticalsConfigService, String vertical)
            throws ResourceNotFoundException {
        VerticalConfig config = verticalsConfigService.getConfigById(vertical);
        if (config == null) {
            throw new ResourceNotFoundException("Unknown vertical: " + vertical);
        }
        return config;
    }
}
