package org.open4goods.services.feedservice.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationCapability;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.scheduling.annotation.Scheduled;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Feed service implementation for Webgains.
 * 
 * @author open4goods
 */
public class WebgainsFeedService extends AbstractFeedService
{
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebgainsFeedService(FeedConfiguration feedConfig,
                               RemoteFileCachingService remoteFileCachingService,
                               DataSourceConfigService dataSourceConfigService,
                               SerialisationService serialisationService,
                               String apiKey)
    {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Scheduled(cron = "${feed.webgains.cron:-}")
    public void scheduledLoad()
    {
        if (!isEnabled())
        {
            logger.info("Webgains feed service is disabled. Skipping scheduled load.");
            return;
        }
        logger.info("Scheduled refresh of Webgains datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception
    {
        logger.info("Webgains loadDatasources called.");
        return Collections.emptySet();
    }

    @Override
    public String getProviderName()
    {
        return "Webgains";
    }

    @Override
    public Set<AffiliationCapability> getCapabilities()
    {
        return Set.of(AffiliationCapability.FEEDS, AffiliationCapability.PROMOTIONS);
    }

    @Override
    protected Collection<AffiliationPromotion> loadPromotionsInternal() throws Exception
    {
        if (!isEnabled() || isBlank(apiKey))
        {
            logger.info("Webgains feed service is disabled or API key is blank. Returning empty promotions.");
            return Collections.emptyList();
        }
        return parseWebgainsPromotions(retrieveWebgainsPromotions());
    }

    /**
     * Retrieves active Webgains offers and vouchers from the publisher API.
     *
     * @return raw Webgains promotions payload
     * @throws Exception if retrieval or parsing fails
     */
    public JsonNode retrieveWebgainsPromotions() throws Exception
    {
        String endpoint = feedConfig.getWebgains().getOffersAndVouchersEndpoint();
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
        {
            throw new IllegalStateException("Webgains API call failed for " + endpoint
                    + " with HTTP status " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    Collection<AffiliationPromotion> parseWebgainsPromotions(JsonNode root)
    {
        JsonNode promotionsNode = arrayNode(root, "offers", "vouchers", "promotions", "results", "data", "items");
        if (promotionsNode == null)
        {
            return Collections.emptyList();
        }

        List<AffiliationPromotion> list = new ArrayList<>();
        for (JsonNode node : promotionsNode)
        {
            JsonNode program = firstExisting(node, "program", "campaign", "advertiser", "merchant");
            AffiliationPromotion promotion = new AffiliationPromotion();
            promotion.setProviderName(getProviderName());
            promotion.setProgramId(firstNonBlank(
                    textOrNull(program, "id"),
                    textOrNull(program, "programId"),
                    textOrNull(node, "programId"),
                    textOrNull(node, "campaignId"),
                    textOrNull(node, "advertiserId")));
            promotion.setAdvertiserName(firstNonBlank(
                    textOrNull(program, "name"),
                    textOrNull(program, "advertiserName"),
                    textOrNull(node, "advertiserName"),
                    textOrNull(node, "merchantName")));
            promotion.setTitle(firstNonBlank(textOrNull(node, "title"), textOrNull(node, "name")));
            promotion.setDescription(firstNonBlank(textOrNull(node, "description"), textOrNull(node, "summary")));
            promotion.setVoucherCode(firstNonBlank(textOrNull(node, "voucherCode"), textOrNull(node, "voucher_code"), textOrNull(node, "code")));
            promotion.setDiscountType(firstNonBlank(textOrNull(node, "type"), textOrNull(node, "discountType"), textOrNull(node, "offerType")));
            promotion.setDiscountValue(firstBigDecimal(node, "discountValue", "discount_value", "value"));
            promotion.setStartDate(parseLocalDate(firstNonBlank(textOrNull(node, "startDate"), textOrNull(node, "start_date"), textOrNull(node, "validFrom"))));
            promotion.setEndDate(parseLocalDate(firstNonBlank(textOrNull(node, "endDate"), textOrNull(node, "end_date"), textOrNull(node, "validTo"))));
            promotion.setLandingUrl(firstNonBlank(textOrNull(node, "landingUrl"), textOrNull(node, "landing_url"), textOrNull(node, "url")));
            promotion.setTrackingUrl(firstNonBlank(textOrNull(node, "trackingUrl"), textOrNull(node, "tracking_url"), textOrNull(node, "clickUrl"), textOrNull(node, "click_url")));
            promotion.setConditions(firstNonBlank(textOrNull(node, "conditions"), textOrNull(node, "terms"), textOrNull(node, "termsAndConditions")));

            Set<String> countryCodes = countryCodes(node);
            if (!countryCodes.isEmpty())
            {
                promotion.setCountryCodes(countryCodes);
            }
            list.add(promotion);
        }
        return list;
    }

    private boolean isEnabled()
    {
        return feedConfig.getWebgains() != null && feedConfig.getWebgains().isEnabled();
    }

    private JsonNode arrayNode(JsonNode root, String... fields)
    {
        if (root == null || root.isMissingNode() || root.isNull())
        {
            return null;
        }
        if (root.isArray())
        {
            return root;
        }
        for (String field : fields)
        {
            JsonNode candidate = root.path(field);
            if (!candidate.isMissingNode() && candidate.isArray())
            {
                return candidate;
            }
        }
        for (String field : fields)
        {
            JsonNode candidate = root.path(field);
            if (!candidate.isMissingNode() && !candidate.isNull())
            {
                JsonNode nested = arrayNode(candidate, fields);
                if (nested != null)
                {
                    return nested;
                }
            }
        }
        return null;
    }

    private JsonNode firstExisting(JsonNode node, String... fields)
    {
        if (node == null)
        {
            return null;
        }
        for (String field : fields)
        {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull())
            {
                return value;
            }
        }
        return null;
    }

    private Set<String> countryCodes(JsonNode node)
    {
        Set<String> result = new LinkedHashSet<>();
        addCountryCode(result, textOrNull(node, "countryCode"));
        addCountryCode(result, textOrNull(node, "country_code"));
        addCountryCode(result, textOrNull(node.path("country"), "code"));
        JsonNode countries = firstExisting(node, "countries", "regions", "countryCodes");
        if (countries != null && countries.isArray())
        {
            for (JsonNode country : countries)
            {
                addCountryCode(result, country.isTextual() ? country.asText() : firstNonBlank(textOrNull(country, "code"), textOrNull(country, "countryCode")));
            }
        }
        return result;
    }

    private void addCountryCode(Set<String> countryCodes, String code)
    {
        if (!isBlank(code))
        {
            countryCodes.add(code.trim().toUpperCase());
        }
    }

    private LocalDate parseLocalDate(String value)
    {
        if (isBlank(value))
        {
            return null;
        }
        try
        {
            return LocalDate.parse(value.trim().substring(0, 10));
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private BigDecimal firstBigDecimal(JsonNode node, String... fields)
    {
        for (String field : fields)
        {
            BigDecimal parsed = parseBigDecimal(node.path(field));
            if (parsed != null)
            {
                return parsed;
            }
        }
        return null;
    }

    private BigDecimal parseBigDecimal(JsonNode node)
    {
        if (node == null || node.isMissingNode() || node.isNull())
        {
            return null;
        }
        String value = node.path("amount").isMissingNode() ? node.asText() : node.path("amount").asText();
        if (isBlank(value))
        {
            return null;
        }
        try
        {
            return new BigDecimal(value.trim());
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field)
    {
        if (node == null || field == null)
        {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull())
        {
            return null;
        }
        String s = v.asText();
        return isBlank(s) ? null : s;
    }

    private String firstNonBlank(String... values)
    {
        if (values == null)
        {
            return null;
        }
        for (String value : values)
        {
            if (!isBlank(value))
            {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value)
    {
        return org.open4goods.commons.util.HttpUtils.isBlank(value);
    }
}
