package org.open4goods.nudgerfrontapi.service;

import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.nudgerfrontapi.config.properties.AffiliationPartnersProperties;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.partner.AffiliationPartnerDto;
import org.open4goods.services.contribution.model.ContributionVote;
import org.open4goods.services.contribution.service.ContributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Service responsible for retrieving affiliation partners from the back-office API and caching them in memory.
 */
@Service
public class AffiliationPartnerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AffiliationPartnerService.class);
    private static final ParameterizedTypeReference<List<AffiliationPartner>> PARTNERS_TYPE =
            new ParameterizedTypeReference<>() { };

    private final RestClient restClient;
    private final AffiliationPartnersProperties properties;
    private final ApiProperties apiProperties;
    private final AtomicReference<List<AffiliationPartner>> partners = new AtomicReference<>(List.of());
    private final AffiliationService affiliationService;

    public AffiliationPartnerService(RestClient.Builder restClientBuilder, AffiliationPartnersProperties properties,
            ApiProperties apiProperties, AffiliationService affiliationService) {
        this.properties = properties;
        this.apiProperties = apiProperties;
        this.affiliationService = affiliationService;
        this.restClient = restClientBuilder.baseUrl(properties.getApiBaseUrl())
                .defaultHeader(UrlConstants.APIKEY_PARAMETER, properties.getApiKey())
                .build();
    }

    /**
     * Returns the latest affiliation partners snapshot.
     *
     * @return immutable list of affiliation partners
     */
    public List<AffiliationPartner> getPartners() {
        return partners.get();
    }

    /**
     * Returns affiliation partners mapped to DTOs enriched with asset URLs.
     *
     * @return immutable list of partner DTOs
     */
    public List<AffiliationPartnerDto> getPartnerDtos() {
        return partners.get().stream()
                .map(this::mapToDto)
                .toList();
    }

    @PostConstruct
    public void preloadPartners() {
        refreshPartners();
    }

    /**
     * Refreshes the cached list of affiliation partners from the back-office API.
     * Retains the previous value when the HTTP call fails.
     */
    @Scheduled(fixedDelayString = "PT1H")
    public void refreshPartners() {
        try {
            List<AffiliationPartner> fetched = restClient.get().uri(properties.getPartnersPath()).retrieve()
                    .body(PARTNERS_TYPE);
            List<AffiliationPartner> immutablePartners = fetched == null ? List.of() : List.copyOf(fetched);
            partners.set(immutablePartners);
            LOGGER.info("Loaded {} affiliation partners from backend.", immutablePartners.size());
        }
        catch (Exception exception) {
            LOGGER.warn("Failed to refresh affiliation partners. Preserving {} cached partners. Error: {}",
                    partners.get().size(), exception.getMessage(), exception);
        }
    }

    private AffiliationPartnerDto mapToDto(AffiliationPartner partner) {
        String logoUrl = buildAssetUrl("/logo/", partner.getName());
        String faviconUrl = buildAssetUrl("/favicon?url=", partner.getName());
        List<String> countryCodes = partner.getCountryCodes() == null
                ? List.of()
                : partner.getCountryCodes().stream()
                        .sorted(Comparator.naturalOrder())
                        .toList();



        return new AffiliationPartnerDto(
                partner.getId(),
                partner.getName(),
                affiliationService.encryptAffiliationLink(partner.getName(), partner.getAffiliationLink()),
                partner.getPortalUrl(),
                logoUrl,
                faviconUrl,
                countryCodes
        );
    }

    private String buildAssetUrl(String pathSuffix, String partnerName) {
        if (!StringUtils.hasText(pathSuffix) || !StringUtils.hasText(partnerName)
                || !StringUtils.hasText(apiProperties.getResourceRootPath())) {
            return null;
        }
        String base = apiProperties.getResourceRootPath();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + pathSuffix + partnerName;
    }
}
