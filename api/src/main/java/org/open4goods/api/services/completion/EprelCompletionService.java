package org.open4goods.api.services.completion;

import java.lang.reflect.Array;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.AbstractCompletionService;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.eprelservice.service.EprelSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complete products with Eprel Datas
 */
public class EprelCompletionService extends AbstractCompletionService {

	public static final String EPREL_DS_NAME = "eprel";
	// TODO : From conf, not every one days.
	private static final int REFRESH_IN_DAYS = 1;
	private static final int MIN_COMPACT_MODEL_CONTAINMENT_LENGTH = 7;
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{Alnum}]+");
	private EprelSearchService eprelSearchService;

	Logger logger = LoggerFactory.getLogger(EprelCompletionService.class);
	private StandardAggregator aggregator;

	public EprelCompletionService(VerticalsConfigService verticalConfigService, ProductRepository dataRepository, ApiProperties apiProperties, EprelSearchService eprelSearchService, AggregationFacadeService aggregationFacade) {

		// TODO(p3,conf) : Should set a specific log level here (not "agg(regation)"
		// one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());

		this.eprelSearchService = eprelSearchService;

		this.aggregator = aggregationFacade.getStandardAggregator("eprel-aggregation");
		;
		this.aggregator.beforeStart();
	}

	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		Long lastProcessed = data.getDatasourceCodes().get(getDatasourceName());
		if (null != lastProcessed && REFRESH_IN_DAYS * 1000 * 3600 * 24 < System.currentTimeMillis() - lastProcessed) {
			// TODO : do not process each time
			return true;
		} else {
			return true;
		}
	}

	@Override
	public String getDatasourceName() {
		// No datasource name for resource completion
		return EPREL_DS_NAME;
	}

	/**
	 * Process resources for one product
	 *
	 * @param data
	 * @param vertical
	 */

	@Override
	public void processProduct(VerticalConfig vertical, Product data) {

		List<String> models = modelCandidates(data);

		List<EprelProduct> results = eprelSearchService.search(data.gtin(), models, vertical.getEprelGroupNames());

		if (null == results || results.size() == 0) {
			logger.warn("No EPREL results when completing {}-{}", data.brand(), data.model());
			data.setEprelDatas(null);
			data.getExternalIds().setEprel(null);
			data.removeDatasourceData(getDatasourceName());
			return;
		}
		Optional<EprelProduct> selected = selectUniqueResult(results, data, models, vertical);
		if (selected.isEmpty()) {
			logger.warn("No safe unique EPREL result ({} candidates) when completing {}", results.size(), data);
			data.removeDatasourceData(getDatasourceName());
			return;
		}
		{
			logger.info("Completing product {} with EPREL datas", data);

			EprelProduct eprelData = resolveLatestVersion(selected.get(), vertical);

			data.setEprelDatas(eprelData);
			data.getExternalIds().setEprel(eprelData.getEprelRegistrationNumber());

			// Set attributes

			Set<DataFragment> fragments = getEprelAttributesFragments(data, vertical);
			// Apply aggregation
			for (DataFragment df : fragments) {
				try {
					aggregator.onDatafragment(df, data);
				} catch (AggregationSkipException e) {
					logger.error("Error occurs during icecat aggregation", e);
				}
			}

			// TODO : Filter per vertical

			// Setting the computed flag
			data.getDatasourceCodes().put(getDatasourceName(), System.currentTimeMillis());

			logger.info("product {} completed with EPREL datas ", data);

		}
	}

    /**
     * Selects exactly one EPREL candidate with conservative evidence.
     *
     * <p>GTIN matches are authoritative. Otherwise, multiple candidates are first
     * narrowed by brand, then by a whole-label model containment check. This avoids
     * accepting a broad prefix/wildcard EPREL hit unless the catalogue has a longer
     * model label that uniquely names the EPREL variant.
     *
     * @param results raw EPREL search results
     * @param product product being completed
     * @param modelCandidates product model and alternate model labels
     * @return selected candidate, or empty when selection is ambiguous
     */
    private Optional<EprelProduct> selectUniqueResult(List<EprelProduct> results, Product product,
            List<String> modelCandidates, VerticalConfig vertical) {
        List<EprelProduct> gtinMatches = results.stream()
                .filter(candidate -> hasSameGtin(product.gtin(), candidate))
                .toList();
        if (gtinMatches.size() == 1) {
            return Optional.of(gtinMatches.getFirst());
        }
        if (gtinMatches.size() > 1) {
            logger.info("EPREL GTIN matched {} candidates for {}", gtinMatches.size(), product);
            return selectDeterministicBest(gtinMatches, modelCandidates, vertical, product, "GTIN");
        }

        if (results.size() == 1) {
            EprelProduct onlyResult = results.getFirst();
            if (hasCompatibleBrand(product.brand(), onlyResult) && hasModelEvidence(onlyResult, modelCandidates)) {
                return Optional.of(onlyResult);
            }
            logger.warn("Rejecting single EPREL result {} for {}: missing brand or model evidence",
                    onlyResult.getEprelRegistrationNumber(), product);
            return Optional.empty();
        }

        List<EprelProduct> narrowed = eprelSearchService.filterByBrand(results, product.brand());
        if (narrowed.size() == 1) {
            logger.info("Brand filter reduced {} EPREL results to 1 for {}-{}", results.size(), product.brand(),
                    product.model());
            return Optional.of(narrowed.getFirst());
        }

        List<EprelProduct> modelMatches = narrowed.stream()
                .filter(candidate -> hasModelEvidence(candidate, modelCandidates))
                .toList();
        if (modelMatches.size() == 1)
        {
            logger.info("Model label filter reduced {} EPREL results to 1 for {}-{}", results.size(), product.brand(),
                    product.model());
            return Optional.of(modelMatches.getFirst());
        }
        if (modelMatches.size() > 1)
        {
            int highestScore = modelMatches.stream()
                    .mapToInt(c -> getModelEvidenceScore(c, modelCandidates))
                    .max()
                    .orElse(0);

            List<EprelProduct> bestMatches = modelMatches.stream()
                    .filter(c -> getModelEvidenceScore(c, modelCandidates) == highestScore)
                    .toList();

            if (bestMatches.size() == 1)
            {
                logger.info("Model label filter (best score) reduced {} EPREL results to 1 for {}-{}",
                        results.size(), product.brand(), product.model());
                return Optional.of(bestMatches.getFirst());
            }
            return selectDeterministicBest(bestMatches, modelCandidates, vertical, product, "model label");
        }
        return selectDeterministicBest(narrowed.isEmpty() ? results : narrowed, modelCandidates, vertical, product,
                "fallback");
    }

    private Optional<EprelProduct> selectDeterministicBest(List<EprelProduct> candidates,
            List<String> modelCandidates, VerticalConfig vertical, Product product, String reason)
    {
        if (candidates == null || candidates.isEmpty())
        {
            return Optional.empty();
        }
        List<EprelProduct> eligible = candidates.stream()
                .filter(candidate -> hasCompatibleBrand(product.brand(), candidate))
                .filter(candidate -> hasModelEvidence(candidate, modelCandidates) || hasSameGtin(product.gtin(), candidate))
                .sorted((left, right) -> compareDeterministicCandidate(right, left, modelCandidates, vertical))
                .toList();
        if (eligible.isEmpty())
        {
            return Optional.empty();
        }
        EprelProduct selected = eligible.getFirst();
        logger.info("Deterministically selected EPREL result {} for {} from {} {} candidates",
                selected.getEprelRegistrationNumber(), product, eligible.size(), reason);
        return Optional.of(selected);
    }

    private int compareDeterministicCandidate(EprelProduct left, EprelProduct right,
            List<String> modelCandidates, VerticalConfig vertical)
    {
        int comparison = Integer.compare(getModelEvidenceScore(left, modelCandidates),
                getModelEvidenceScore(right, modelCandidates));
        if (comparison != 0)
        {
            return comparison;
        }
        comparison = Boolean.compare(Boolean.TRUE.equals(left.getLastVersion()), Boolean.TRUE.equals(right.getLastVersion()));
        if (comparison != 0)
        {
            return comparison;
        }
        comparison = nullSafeCompare(left.getVersionNumber(), right.getVersionNumber());
        if (comparison != 0)
        {
            return comparison;
        }
        comparison = nullSafeCompare(left.getVersionId(), right.getVersionId());
        if (comparison != 0)
        {
            return comparison;
        }
        comparison = nullSafeCompare(left.getProductModelCoreId(), right.getProductModelCoreId());
        if (comparison != 0)
        {
            return comparison;
        }
        comparison = Long.compare(candidateFreshness(left), candidateFreshness(right));
        if (comparison != 0)
        {
            return comparison;
        }
        return Integer.compare(categoryPreference(right, vertical), categoryPreference(left, vertical));
    }

    private int categoryPreference(EprelProduct candidate, VerticalConfig vertical)
    {
        if (candidate == null || vertical == null || vertical.getEprelGroupNames() == null)
        {
            return Integer.MAX_VALUE;
        }
        int index = vertical.getEprelGroupNames().indexOf(candidate.getProductGroup());
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private long candidateFreshness(EprelProduct candidate)
    {
        if (candidate == null)
        {
            return Long.MIN_VALUE;
        }
        return java.util.stream.Stream.of(candidate.getImportedOn(), candidate.getPublishedOnDateTs(),
                candidate.getPublishedOnDate(), candidate.getOnMarketStartDateTs(), candidate.getOnMarketStartDate(),
                candidate.getFirstPublicationDateTs(), candidate.getFirstPublicationDate())
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(Long.MIN_VALUE);
    }

    private <T extends Comparable<T>> int nullSafeCompare(T left, T right)
    {
        if (left == null && right == null)
        {
            return 0;
        }
        if (left == null)
        {
            return -1;
        }
        if (right == null)
        {
            return 1;
        }
        return left.compareTo(right);
    }

    /**
     * Returns product model labels in priority order without duplicates.
     *
     * @param product source product
     * @return model candidates
     */
    private List<String> modelCandidates(Product product)
    {
        Set<String> candidates = new LinkedHashSet<>();
        if (product.model() != null)
        {
            candidates.add(product.model());
        }
        if (product.getAkaModels() != null)
        {
            product.getAkaModels().stream()
                    .filter(Objects::nonNull)
                    .forEach(candidates::add);
        }

        List<String> baseCandidates = new ArrayList<>(candidates);
        for (String base : baseCandidates)
        {
            String hyphenated = base.replace(' ', '-').replaceAll("-+", "-");
            if (!hyphenated.equalsIgnoreCase(base))
            {
                candidates.add(hyphenated);
            }
            String spaced = NON_ALNUM.matcher(base).replaceAll(" ").replaceAll(" +", " ").trim();
            if (!spaced.equalsIgnoreCase(base) && !spaced.isEmpty())
            {
                candidates.add(spaced);
            }
            String compact = NON_ALNUM.matcher(base).replaceAll("");
            if (!compact.equalsIgnoreCase(base) && !compact.isEmpty())
            {
                candidates.add(compact);
            }
        }

        return new ArrayList<>(candidates);
    }

    /**
     * Checks whether EPREL and product GTINs are the same numeric value.
     *
     * @param productGtin product GTIN
     * @param candidate EPREL candidate
     * @return true when both sides expose the same GTIN
     */
    private boolean hasSameGtin(String productGtin, EprelProduct candidate) {
        Long productNumericGtin = numericValue(productGtin);
        if (productNumericGtin == null || candidate == null) {
            return false;
        }
        if (productNumericGtin.equals(candidate.getNumericGtin())) {
            return true;
        }
        return productNumericGtin.equals(numericValue(candidate.getGtinIdentifier()));
    }

    /**
     * Checks whether the catalogue brand is compatible with the EPREL supplier.
     *
     * @param brand catalogue brand
     * @param candidate EPREL candidate
     * @return true when the brand is blank or matches the EPREL supplier
     */
    private boolean hasCompatibleBrand(String brand, EprelProduct candidate) {
        String normalizedBrand = normalizePhrase(brand);
        String normalizedSupplier = normalizePhrase(candidate == null ? null : candidate.getSupplierOrTrademark());
        if (normalizedBrand == null) {
            return true;
        }
        if (normalizedSupplier == null) {
            return false;
        }
        return containsWholePhrase(normalizedSupplier, normalizedBrand)
                || containsWholePhrase(normalizedBrand, normalizedSupplier);
    }

    /**
     * Checks whether an EPREL model is explicitly named by one product model label.
     *
     * @param candidate EPREL candidate
     * @param modelCandidates product model labels
     * @return true when model identifiers match exactly or by safe containment
     */
    private boolean hasModelEvidence(EprelProduct candidate, List<String> modelCandidates)
    {
        return getModelEvidenceScore(candidate, modelCandidates) > 0;
    }

    private int getModelEvidenceScore(EprelProduct candidate, List<String> modelCandidates)
    {
        String eprelModel = candidate == null ? null : candidate.getModelIdentifier();
        String normalizedEprelModel = normalizePhrase(eprelModel);
        String compactEprelModel = compactModel(eprelModel);
        if (normalizedEprelModel == null || compactEprelModel == null)
        {
            return 0;
        }

        int maxScore = 0;
        for (String modelCandidate : modelCandidates)
        {
            String normalizedCandidate = normalizePhrase(modelCandidate);
            String compactCandidate = compactModel(modelCandidate);
            if (normalizedCandidate == null || compactCandidate == null)
            {
                continue;
            }
            if (normalizedEprelModel.equals(normalizedCandidate)
                    || compactEprelModel.equals(compactCandidate))
            {
                maxScore = Math.max(maxScore, 3);
            }
            else if (containsWholePhrase(normalizedCandidate, normalizedEprelModel)
                    || (compactEprelModel.length() >= MIN_COMPACT_MODEL_CONTAINMENT_LENGTH
                            && compactCandidate.contains(compactEprelModel)))
            {
                maxScore = Math.max(maxScore, 2);
            }
            else if (containsWholePhrase(normalizedEprelModel, normalizedCandidate)
                    || (compactCandidate.length() >= MIN_COMPACT_MODEL_CONTAINMENT_LENGTH
                            && compactEprelModel.contains(compactCandidate)))
            {
                maxScore = Math.max(maxScore, 1);
            }
        }
        return maxScore;
    }

    private boolean containsWholePhrase(String container, String contained) {
        return (" " + container + " ").contains(" " + contained + " ");
    }

    private Long numericValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizePhrase(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String ascii = DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        String normalized = NON_ALNUM.matcher(ascii.toLowerCase()).replaceAll(" ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String compactModel(String value) {
        String normalized = normalizePhrase(value);
        if (normalized == null) {
            return null;
        }
        String compact = normalized.replace(" ", "");
        return compact.isEmpty() ? null : compact;
    }

    /**
     * Resolves the most recent EPREL version when the current entry is not the latest.
     *
     * @param eprelData current EPREL payload
     * @param vertical vertical configuration used to filter EPREL categories
     * @return most recent EPREL payload when available
     */
    private EprelProduct resolveLatestVersion(EprelProduct eprelData, VerticalConfig vertical) {
        if (eprelData == null || Boolean.TRUE.equals(eprelData.getLastVersion())) {
            return eprelData;
        }
        Long modelCoreId = eprelData.getProductModelCoreId();
        if (modelCoreId == null) {
            logger.info("EPREL model {} flagged as non-latest without model core id", eprelData.getEprelRegistrationNumber());
            return eprelData;
        }
        List<EprelProduct> versions = eprelSearchService.searchByProductModelCoreId(
                modelCoreId, vertical.getEprelGroupNames());
        Optional<EprelProduct> latest = versions.stream()
                .filter(Objects::nonNull)
                .max((left, right) -> compareVersionId(left, right));
        if (latest.isPresent() && latest.get() != eprelData) {
            logger.info("Using EPREL latest version {} instead of {}", latest.get().getEprelRegistrationNumber(),
                    eprelData.getEprelRegistrationNumber());
            return latest.get();
        }
        return eprelData;
    }

    /**
     * Compares EPREL versions using version identifiers when available.
     *
     * @param left first EPREL product
     * @param right second EPREL product
     * @return comparison result
     */
    private int compareVersionId(EprelProduct left, EprelProduct right) {
        Long leftVersion = left == null ? null : left.getVersionId();
        Long rightVersion = right == null ? null : right.getVersionId();
        if (leftVersion == null && rightVersion == null) {
            return 0;
        }
        if (leftVersion == null) {
            return -1;
        }
        if (rightVersion == null) {
            return 1;
        }
        return leftVersion.compareTo(rightVersion);
    }

	private Set<DataFragment> getEprelAttributesFragments(Product data, VerticalConfig vertical) {
		Set<DataFragment> fragment = new HashSet<>();

		if (null != data.getEprelDatas()) {

			Map<String, Object> chars = data.getEprelDatas().getCategorySpecificAttributes();

			DataFragment df = initDataFragment(data);
			addCategoryAttributes(df, chars, "");
			addCoreEprelAttributes(df, data.getEprelDatas());
			fragment.add(df);

		}

		return fragment;
	}

	private void addCategoryAttributes(DataFragment df, Map<String, Object> attributes, String prefix) {
		if (attributes == null) {
			return;
		}
		for (Entry<String, Object> caracteristic : attributes.entrySet()) {
			String attributeKey = prefix.isEmpty() ? caracteristic.getKey() : prefix + "-" + caracteristic.getKey();
			addAttributeValue(df, attributeKey, caracteristic.getValue());
		}
	}

	private void addAttributeValue(DataFragment df, String attributeKey, Object value) {
		if (value == null) {
			return;
		}
		if (value instanceof Map<?, ?> mapValue) {
			for (Entry<?, ?> entry : mapValue.entrySet()) {
				Object entryKey = entry.getKey();
				if (entryKey != null) {
					String childKey = attributeKey + "-" + entryKey.toString();
					addAttributeValue(df, childKey, entry.getValue());
				}
			}
		} else if (value instanceof Collection<?> collectionValue) {
			int index = 0;
			for (Object element : collectionValue) {
				addAttributeValue(df, attributeKey + "[" + index + "]", element);
				index++;
			}
		} else if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			for (int index = 0; index < length; index++) {
				addAttributeValue(df, attributeKey + "[" + index + "]", Array.get(value, index));
			}
		} else {
			df.addAttribute(attributeKey, value.toString(), "fr", null);
		}
	}

	private void addCoreEprelAttributes(DataFragment dataFragment, EprelProduct eprelProduct) {
		addAttributeValue(dataFragment, "energyClass", eprelProduct.getEnergyClass());
		addAttributeValue(dataFragment, "energyClassImage", eprelProduct.getEnergyClassImage());
	}

	private DataFragment initDataFragment(Product data) {
		DataFragment df = new DataFragment();
		df.setDatasourceName(EPREL_DS_NAME);
		df.setDatasourceConfigName(EPREL_DS_NAME);
		df.setLastIndexationDate(System.currentTimeMillis());
		df.setCreationDate(System.currentTimeMillis());
		df.addReferentielAttribute(ReferentielKey.GTIN, String.valueOf(data.getId()));
		return df;
	}

}
