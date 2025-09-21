package org.open4goods.nudgerfrontapi.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Service exposing aggregated statistics for the frontend API.
 */
@Service
public class StatsService {

    private static final String CLASSPATH_VERTICALS = "classpath:/verticals/*.yml";
    private static final String DEFAULT_CONFIG_RESOURCE = "classpath:/verticals/_default.yml";
    private static final String DEFAULT_CONFIG_FILENAME = "_default.yml";

    private final SerialisationService serialisationService;
    private final ResourcePatternResolver resourceResolver;

    public StatsService(SerialisationService serialisationService,
                        ResourcePatternResolver resourceResolver) {
        this.serialisationService = serialisationService;
        this.resourceResolver = resourceResolver;
    }

    /**
     * Compute statistics about categories mappings.
     *
     * @return DTO describing the category statistics used by the frontend.
     */
    public CategoriesStatsDto categories() {
        VerticalConfig defaultConfig = loadDefaultConfig();
        Resource[] resources = loadVerticalResources();

        long enabledCount = Arrays.stream(resources)
                .filter(resource -> !Objects.equals(resource.getFilename(), DEFAULT_CONFIG_FILENAME))
                .map(resource -> loadVerticalConfig(resource, defaultConfig))
                .filter(Objects::nonNull)
                .filter(VerticalConfig::isEnabled)
                .count();

        return new CategoriesStatsDto(Math.toIntExact(enabledCount));
    }

    private VerticalConfig loadDefaultConfig() {
        Resource resource = resourceResolver.getResource(DEFAULT_CONFIG_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            return serialisationService.fromYaml(inputStream, VerticalConfig.class);
        } catch (IOException | SerialisationException e) {
            throw new IllegalStateException("Cannot load default vertical configuration", e);
        }
    }

    private Resource[] loadVerticalResources() {
        try {
            return resourceResolver.getResources(CLASSPATH_VERTICALS);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list vertical configuration files", e);
        }
    }

    private VerticalConfig loadVerticalConfig(Resource resource, VerticalConfig defaultConfig) {
        try (InputStream inputStream = resource.getInputStream()) {
            VerticalConfig base = cloneDefault(defaultConfig);
            ObjectReader reader = serialisationService.getYamlMapper().readerForUpdating(base);
            return reader.readValue(inputStream);
        } catch (IOException | SerialisationException e) {
            throw new IllegalStateException("Cannot load vertical configuration " + resource.getFilename(), e);
        }
    }

    private VerticalConfig cloneDefault(VerticalConfig defaultConfig) throws SerialisationException {
        String yaml = serialisationService.toYaml(defaultConfig);
        return serialisationService.fromYaml(yaml, VerticalConfig.class);
    }
}
