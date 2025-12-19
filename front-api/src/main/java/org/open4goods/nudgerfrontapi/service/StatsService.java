package org.open4goods.nudgerfrontapi.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.model.stats.AffiliationPartnersStats;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Service exposing aggregated statistics for the frontend API.
 * <p>
 * Statistics are computed from the vertical YAML files shipped with the distribution. The service keeps
 * the resource handling and defensive checks centralised so controllers simply forward the domain language.
 * </p>
 */
@Service
public class StatsService {

    private static final String CLASSPATH_VERTICALS = "classpath:/verticals/*.yml";
    private static final String DEFAULT_CONFIG_RESOURCE = "classpath:/verticals/_default.yml";
    private static final String DEFAULT_CONFIG_FILENAME = "_default.yml";

    private final SerialisationService serialisationService;
    private final ResourcePatternResolver resourceResolver;
    private final AffiliationPartnerService affiliationPartnerService;

    public StatsService(SerialisationService serialisationService,
                        ResourcePatternResolver resourceResolver,
                        AffiliationPartnerService affiliationPartnerService) {
        this.serialisationService = serialisationService;
        this.resourceResolver = resourceResolver;
        this.affiliationPartnerService = affiliationPartnerService;
    }

    /**
     * Compute statistics about categories mappings.
     *
     * @param domainLanguage currently unused but retained for future localisation of statistics labels
     * @return DTO describing the category statistics used by the frontend.
     */
    public CategoriesStatsDto categories(DomainLanguage domainLanguage) {
        VerticalConfig defaultConfig = loadDefaultConfig();
        Resource[] resources = loadVerticalResources();
        AffiliationPartnersStats partnersStats = computeAffiliationPartnersStats();

        long enabledCount = Arrays.stream(resources)
                .filter(resource -> !Objects.equals(resource.getFilename(), DEFAULT_CONFIG_FILENAME))
                // Copy the default config before merging custom values to keep defaults intact.
                .map(resource -> loadVerticalConfig(resource, defaultConfig))
                .filter(Objects::nonNull)
                .filter(VerticalConfig::isEnabled)
                .count();

        return new CategoriesStatsDto(Math.toIntExact(enabledCount), partnersStats.count());
    }

    /**
     * Compute statistics about affiliation partners so the homepage can display partner counts alongside categories stats.
     *
     * @return immutable stats wrapper describing the partner catalogue
     */
    private AffiliationPartnersStats computeAffiliationPartnersStats() {
        List<AffiliationPartner> partners = affiliationPartnerService.getPartners();
        int partnersCount = partners == null ? 0 : partners.size();

        return new AffiliationPartnersStats(partnersCount);
    }

    /**
     * Load the default vertical configuration which acts as the base for every other vertical.
     *
     * @return parsed {@link VerticalConfig}
     */
    private VerticalConfig loadDefaultConfig() {
        Resource resource = resourceResolver.getResource(DEFAULT_CONFIG_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            return serialisationService.fromYaml(inputStream, VerticalConfig.class);
        } catch (IOException | SerialisationException e) {
            throw new IllegalStateException("Cannot load default vertical configuration", e);
        }
    }

    /**
     * List all vertical configuration resources available on the classpath.
     *
     * @return resources matching the vertical glob pattern
     */
    private Resource[] loadVerticalResources() {
        try {
            return resourceResolver.getResources(CLASSPATH_VERTICALS);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list vertical configuration files", e);
        }
    }

    /**
     * Load a single vertical configuration by applying overrides on top of the default configuration.
     *
     * @param resource      YAML resource describing a vertical
     * @param defaultConfig base configuration used as a template
     * @return fully merged configuration
     */
    private VerticalConfig loadVerticalConfig(Resource resource, VerticalConfig defaultConfig) {
        try (InputStream inputStream = resource.getInputStream()) {
            VerticalConfig base = cloneDefault(defaultConfig);
            ObjectReader reader = serialisationService.getYamlMapper().readerForUpdating(base);
            return reader.readValue(inputStream);
        } catch (IOException | SerialisationException e) {
            throw new IllegalStateException("Cannot load vertical configuration " + resource.getFilename(), e);
        }
    }

    /**
     * Create a deep copy of the default configuration so per-vertical overrides do not mutate the shared instance.
     *
     * @param defaultConfig reference configuration loaded from {@code _default.yml}
     * @return cloned configuration
     * @throws SerialisationException when the YAML serialisation round trip fails
     */
    private VerticalConfig cloneDefault(VerticalConfig defaultConfig) throws SerialisationException {
        String yaml = serialisationService.toYaml(defaultConfig);
        return serialisationService.fromYaml(yaml, VerticalConfig.class);
    }
}
