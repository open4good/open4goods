package org.open4goods.api.services.completion;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.AbstractCompletionService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.ResourceTag;
import org.open4goods.model.util.ProductModelCandidateHelper;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.wikidataservice.config.WikidataServiceProperties;
import org.open4goods.services.wikidataservice.model.WikidataEntity;
import org.open4goods.services.wikidataservice.service.WikidataLookupService;
import org.open4goods.services.wikidataservice.service.WikidataSearchService;
import org.open4goods.services.wikidataservice.util.WikidataConstants;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Completion service that enriches products with data from Wikidata.
 *
 * <p>Lookup strategy (first match wins):
 * <ol>
 *   <li>If {@code ExternalIds.wikidata} is already set, refresh by Q-id.</li>
 *   <li>GTIN search via SPARQL (P3962).</li>
 *   <li>Brand + model SPARQL fallback (when enabled by config).</li>
 * </ol>
 *
 * <p>On success, converts the {@link WikidataEntity} to a {@link DataFragment} and runs it
 * through the standard aggregator pipeline, so all existing normalisation and merging logic
 * applies without special-casing.
 */
public class WikidataCompletionService extends AbstractCompletionService {

    public static final String DATASOURCE_NAME = "wikidata.org";

    private static final Logger LOGGER = LoggerFactory.getLogger(WikidataCompletionService.class);

    private final WikidataSearchService searchService;
    private final WikidataLookupService lookupService;
    private final WikidataServiceProperties wikidataProperties;
    private final StandardAggregator aggregator;

    public WikidataCompletionService(
            ProductRepository dataRepository,
            VerticalsConfigService verticalConfigService,
            ApiProperties apiProperties,
            DataSourceConfigService dataSourceConfigService,
            AggregationFacadeService aggregationFacadeService,
            WikidataSearchService searchService,
            WikidataLookupService lookupService,
            WikidataServiceProperties wikidataProperties) {
        super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());
        this.searchService = searchService;
        this.lookupService = lookupService;
        this.wikidataProperties = wikidataProperties;
        this.aggregator = aggregationFacadeService.getStandardAggregator("wikidata-aggregation");
        this.aggregator.beforeStart();
    }

    @Override
    public String getDatasourceName() {
        return DATASOURCE_NAME;
    }

    @Override
    public boolean shouldProcess(VerticalConfig vertical, Product data) {
        Long lastProcessed = data.getDatasourceCodes().get(getDatasourceName());
        if (lastProcessed == null) {
            return true;
        }
        long refreshMs = Duration.ofDays(wikidataProperties.getRefreshInDays()).toMillis();
        return System.currentTimeMillis() - lastProcessed >= refreshMs;
    }

    @Override
    public void processProduct(VerticalConfig vertical, Product data) {
        LOGGER.info("Wikidata completion for {}", data.getId());

        Optional<WikidataEntity> entityOpt = resolve(data);

        if (entityOpt.isEmpty()) {
            LOGGER.info("No Wikidata entity found for {}", data.getId());
            return;
        }

        WikidataEntity entity = entityOpt.get();
        data.getExternalIds().setWikidata(entity.getQId());

        DataFragment df = convert(entity, data);
        try {
            aggregator.onDatafragment(df, data);
        } catch (AggregationSkipException e) {
            LOGGER.error("Wikidata aggregation skipped for {}", data.getId(), e);
        }

        data.getDatasourceCodes().put(getDatasourceName(), System.currentTimeMillis());

        politeSleep();
    }

    private Optional<WikidataEntity> resolve(Product data) {
        String knownQid = data.getExternalIds().getWikidata();
        if (!StringUtils.isBlank(knownQid)) {
            LOGGER.debug("Refreshing Wikidata Q-id {} for product {}", knownQid, data.getId());
            return lookupService.fetchByQid(knownQid);
        }

        LOGGER.debug("Searching Wikidata by GTIN {} for product {}", data.gtin(), data.getId());
        Optional<WikidataEntity> byGtin = searchService.searchByGtin(data.gtin());
        if (byGtin.isPresent()) {
            return byGtin;
        }

        if (wikidataProperties.isBrandModelFallbackEnabled()) {
            List<String> candidates = modelCandidates(data);
            if (!candidates.isEmpty()) {
                LOGGER.debug("Searching Wikidata by brand+model for product {}", data.getId());
                return searchService.searchByBrandModel(data.brand(), candidates);
            }
        }

        return Optional.empty();
    }

    private DataFragment convert(WikidataEntity entity, Product data) {
        DataFragment df = initDataFragment(data);

        completeNames(entity, df);
        completeReferentielAttributes(entity, df);
        completeResources(entity, df, data);
        completeAttributes(entity, df);

        return df;
    }

    private void completeNames(WikidataEntity entity, DataFragment df) {
        for (String langValue : entity.getLabels()) {
            String value = extractValue(langValue);
            if (value != null) {
                df.addName(value);
            }
        }
        for (String langValue : entity.getAliases()) {
            String value = extractValue(langValue);
            if (value != null) {
                df.addName(value);
            }
        }
    }

    private void completeReferentielAttributes(WikidataEntity entity, DataFragment df) {
        for (String brandLabel : entity.getBrandLabels()) {
            if (!StringUtils.isBlank(brandLabel)) {
                df.addReferentielAttribute(ReferentielKey.BRAND, brandLabel);
            }
        }
        if (!StringUtils.isBlank(entity.getReleaseYear())) {
            df.addAttribute("YEAR", entity.getReleaseYear(), WikidataConstants.LANG_DEFAULT, null);
        }
    }

    private void completeResources(WikidataEntity entity, DataFragment df, Product data) {
        for (String imageName : entity.getImages()) {
            if (!StringUtils.isBlank(imageName)) {
                String imageUrl = WikidataConstants.COMMONS_FILE_URL + imageName.replace(" ", "_");
                addResourceSafely(df, data, imageUrl, ResourceTag.PRIMARY.toString());
            }
        }

        for (String videoName : entity.getVideos()) {
            if (!StringUtils.isBlank(videoName)) {
                String videoUrl = WikidataConstants.COMMONS_FILE_URL + videoName.replace(" ", "_");
                addResourceSafely(df, data, videoUrl, "video");
            }
        }

        if (!StringUtils.isBlank(entity.getWebsite())) {
            addResourceSafely(df, data, entity.getWebsite(), ResourceTag.PRODUCT_SHEET.toString());
        }

        for (String langUrl : entity.getWikipediaUrls()) {
            String url = extractValue(langUrl);
            String lang = extractLang(langUrl);
            if (!StringUtils.isBlank(url)) {
                addResourceSafely(df, data, url, "wikipedia-" + lang);
            }
        }
    }

    private void completeAttributes(WikidataEntity entity, DataFragment df) {
        for (java.util.Map.Entry<String, String> claim : entity.getNumericClaims().entrySet()) {
            if (!StringUtils.isBlank(claim.getValue())) {
                df.addAttribute(claim.getKey(), claim.getValue(), WikidataConstants.LANG_DEFAULT, claim.getKey());
            }
        }
    }

    private void addResourceSafely(DataFragment df, Product data, String url, String tag) {
        try {
            df.addResource(url, Sets.newHashSet(tag, "wikidata"));
        } catch (ValidationException e) {
            LOGGER.warn("Cannot add Wikidata resource {} for {}: {}", url, data.getId(), e.getMessage());
        }
    }

    private DataFragment initDataFragment(Product data) {
        DataFragment df = new DataFragment();
        df.setDatasourceName(DATASOURCE_NAME);
        df.setDatasourceConfigName(DATASOURCE_NAME);
        df.setLastIndexationDate(System.currentTimeMillis());
        df.setCreationDate(System.currentTimeMillis());
        df.addReferentielAttribute(ReferentielKey.GTIN, String.valueOf(data.getId()));
        return df;
    }

    /**
     * Builds an ordered list of model identifier candidates for the brand+model fallback.
     * Mirrors the logic in {@link EprelCompletionService} to produce consistent candidates.
     */
    private List<String> modelCandidates(Product product) {
        return ProductModelCandidateHelper.expandedCandidates(product);
    }

    private String extractValue(String langColonValue) {
        if (langColonValue == null) {
            return null;
        }
        int colon = langColonValue.indexOf(':');
        if (colon < 0 || colon == langColonValue.length() - 1) {
            return null;
        }
        return langColonValue.substring(colon + 1);
    }

    private String extractLang(String langColonValue) {
        if (langColonValue == null) {
            return WikidataConstants.LANG_DEFAULT;
        }
        int colon = langColonValue.indexOf(':');
        if (colon <= 0) {
            return WikidataConstants.LANG_DEFAULT;
        }
        return langColonValue.substring(0, colon);
    }

    private void politeSleep() {
        int delay = wikidataProperties.getPolitenessDelayMs();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Wikidata politeness sleep interrupted");
        }
    }
}
