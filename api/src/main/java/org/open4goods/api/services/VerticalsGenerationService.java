package org.open4goods.api.services;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.VerticalsGenerationConfig;
import org.open4goods.api.model.AttributesStats;
import org.open4goods.api.model.VerticalAttributesStats;
import org.open4goods.api.model.VerticalCategoryMapping;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.NudgeToolScore;
import org.open4goods.model.vertical.ScoreRange;
import org.open4goods.model.vertical.SubsetCriteria;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

public class VerticalsGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalsGenerationService.class);
	private static final double TARGET_THRESHOLD_RATIO = 1.0 / 3.0;
	private static final double SCORE_MIN_VALUE = 0.0;
	private static final double SCORE_MAX_VALUE = 5.0;
	private static final double ACCEPTABLE_RATIO_DELTA = 0.05;
	private static final int MAX_THRESHOLD_ITERATIONS = 40;
	private static final String IMPACT_SCORE_NAME = "ECOSCORE";
	private VerticalsGenerationConfig config;
	private VerticalsConfigService  verticalConfigservice;
	private ProductRepository repository;
	private SerialisationService serialisationService;
	private ResourcePatternResolver resourceResolver;
	private IcecatService icecatService;



	private Map<String, VerticalCategoryMapping> sortedMappings = new LinkedHashMap<String, VerticalCategoryMapping>();
	private GoogleTaxonomyService googleTaxonomyService;
	private EvaluationService evalService;
	private PromptService genAiService;

	public VerticalsGenerationService(VerticalsGenerationConfig config, ProductRepository repository, SerialisationService serialisationService, GoogleTaxonomyService googleTaxonomyService, VerticalsConfigService verticalsConfigService, ResourcePatternResolver resourceResolver, EvaluationService evaluationService, IcecatService icecatService, PromptService genAiService ) {
		super();
		this.config = config;
		this.repository = repository;
		this.serialisationService = serialisationService;
		this.googleTaxonomyService = googleTaxonomyService;
		this.verticalConfigservice = verticalsConfigService;
		this.resourceResolver = resourceResolver;
		this.evalService = evaluationService;
		this.icecatService = icecatService;
		this.genAiService = genAiService;
	}

	/**
	 *
	 * @return the mappings
	 */
	public Map<String, VerticalCategoryMapping> getMappings() {
		return sortedMappings;
	}

	public VerticalConfig computeNudgeToolThresholds(String verticalId) {
		VerticalConfig verticalConfig = verticalConfigservice.getConfigById(verticalId);
		if (verticalConfig == null) {
			return null;
		}

		// Minimal config to return
		VerticalConfig result = new VerticalConfig();
		result.setId(verticalId);

		// Compute nudge tool scores
		if (verticalConfig.getNudgeToolConfig() != null) {
			List<NudgeToolScore> scores = verticalConfig.getNudgeToolConfig().getScores();
			if (scores != null && !scores.isEmpty()) {
				for (NudgeToolScore score : scores) {
					if (StringUtils.isNotBlank(score.getScoreName())) {
						NudgeToolScore newScore = new NudgeToolScore();
						newScore.setScoreName(score.getScoreName());
						// Copy other useful metadata
						newScore.setTitle(score.getTitle());
						newScore.setMdiIcon(score.getMdiIcon());
						newScore.setDisabled(score.getDisabled());
						newScore.setDescription(score.getDescription());
						
						// If fromPercent is set, use percentile-based config; otherwise calculate threshold
						if (score.getFromPercent() != null) {
							newScore.setFromPercent(score.getFromPercent());
							newScore.setToPercent(score.getToPercent());
							// scoreMinValue is not used when percentile-based filtering is active
						} else {
							double threshold = computeThresholdForScore(verticalId, score.getScoreName(), SubsetCriteriaOperator.GREATER_THAN);
							newScore.setScoreMinValue(threshold);
						}
						result.getNudgeToolConfig().getScores().add(newScore);
					}
				}
			}
		}

		// Compute impact score subsets
		ScoreThresholds thresholds = computeImpactScoreThresholds(verticalId);
		String lowerThreshold = formatScoreValue(thresholds.lower());
		String upperThreshold = formatScoreValue(thresholds.upper());

		// Build subsets (impact score only for now as requested)
		List<VerticalSubset> subsets = buildImpactScoreSubsetsList(lowerThreshold, upperThreshold);
		result.getNudgeToolConfig().setSubsets(subsets);
		result.setSubsets(subsets);

		return result;
	}

	private List<VerticalSubset> buildImpactScoreSubsetsList(String lowerThreshold, String upperThreshold) {
		List<VerticalSubset> subsets = new ArrayList<>();

		subsets.add(createImpactSubset("impact_high", "LOWER_THAN_OR_EQUAL", lowerThreshold));
		subsets.add(createImpactSubset("impact_medium", List.of(
				new SubsetCriteria(IMPACT_SCORE_FIELD, SubsetCriteriaOperator.GREATER_THAN, lowerThreshold),
				new SubsetCriteria(IMPACT_SCORE_FIELD, SubsetCriteriaOperator.LOWER_THAN_OR_EQUAL, upperThreshold)
		)));
		subsets.add(createImpactSubset("impact_low", "GREATER_THAN", upperThreshold));

		return subsets;
	}

	private static final String IMPACT_SCORE_FIELD = "scores.ECOSCORE.value";

	private VerticalSubset createImpactSubset(String id, String operator, String value) {
		return createImpactSubset(id, List.of(new SubsetCriteria(IMPACT_SCORE_FIELD, SubsetCriteriaOperator.valueOf(operator), value)));
	}

	private VerticalSubset createImpactSubset(String id, List<SubsetCriteria> criterias) {
		VerticalSubset subset = new VerticalSubset();
		subset.setId(id);
		subset.setGroup("impactscore");
		subset.setCriterias(new ArrayList<>(criterias));
		return subset;
	}



	/**
	 * Compute the attributes coverage stats for this vertical
	 * @param vertical
	 * @return
	 */
	public VerticalAttributesStats attributesStats(String vertical) {
		VerticalConfig vc = verticalConfigservice.getConfigById(vertical);
		VerticalAttributesStats ret = new VerticalAttributesStats() ;
		if (null != vc) {
			LOGGER.info("Attributes stats for vertical {} is running",vertical);
			repository.exportVerticalWithValidDate(vc, true).forEach(p -> {
				ret.process(p.getAttributes().getAll());
			});

			// Cleaning the values
			ret.clean();

			// Sorting the values
			ret.sort();
		}

		return ret;
	}




	/**
	 * Generate the yaml categories mapping fragment from sample products
	 * @param gtin
	 * @return
	 */
	public String generateCategoryMappingFragmentForGtin(Collection<String> gtins, Set<String> excludedDatasources) {

		Map<String, Set<String>> matchingCategories = new HashMap<String, Set<String>>();
		matchingCategories.put("all", new HashSet<String>());

		for (String gtin : gtins) {
			Product sample;
			try {
				if (NumberUtils.isDigits(gtin.trim())) {
					sample = repository.getById(Long.valueOf(gtin.trim()));
					sample.getCategoriesByDatasources().entrySet().forEach(e -> {

						if (excludedDatasources != null && excludedDatasources.contains(e.getKey())) {
							LOGGER.info("Skipping {}, in ignored list",e.getKey());
						} else {
							if (!matchingCategories.containsKey(e.getKey())) {
								matchingCategories.put(e.getKey(), new HashSet<String>());
							}

							matchingCategories.get(e.getKey()).add(e.getValue());
						}

					});
				}
			} catch (Exception e) {
				LOGGER.warn("Cannot generate matching categories data : {}", e);
			}
		}


		Map<String,Object> retMAp = new HashMap<String, Object>();
		retMAp.put("matchingCategories", matchingCategories);
		String ret = "";
		try {
			ret = serialisationService.toYaml(retMAp);
		} catch (SerialisationException e) {
			LOGGER.error("Serialisation exception",e);
		}
		ret = ret.replaceFirst("---", "");
		return ret.toString();
	}

	/**
	 * Generate a categories mapping yaml definition from the top n offerscount, allowing exclusion of provided datasources
	 * @param vc
	 * @param excludedDatasources
	 * @param minOfferscount
	 * @return
	 */
	public String generateMapping(VerticalConfig vc, Integer minOfferscount) {

		// Exporting products
		List<String> items =  repository.exportVerticalWithOffersCountGreater(vc, minOfferscount)
				.map(e->e.gtin())
				.toList();

		return generateCategoryMappingFragmentForGtin(items,vc.getGenerationExcludedFromCategoriesMatching());
	}


	/**
	 * Return a String containing a vertical config file, based on the "vertical.template" file
	 * @param id
	 * @param homeTitlefr
	 * @param googleTaxonomyId
	 * @param enabled
	 * @param matchingCategories
	 * @return
	 */
	public String verticalTemplate(String id, String googleTaxonomyId, String matchingCategories,String urlPrefix, String h1Prefix, String verticalHomeUrl, String verticalHomeTitle)  {
		String ret = "";
		try {
			Resource r = resourceResolver.getResource("classpath:/templates/vertical-definition.yml");
			String content = r.getContentAsString(Charset.defaultCharset());

			Map<String, Object> context = new HashMap<String, Object>();

			context.put("id",id );
			context.put("googleTaxonomyId", googleTaxonomyId);
			// Here is a tweak, we provide some sample products coma separated
			context.put("matchingCategories", generateCategoryMappingFragmentForGtin(Arrays.asList(matchingCategories.split(",")), null));
			context.put("urlPrefix", urlPrefix);
			context.put("h1Prefix", h1Prefix);
			context.put("verticalHomeUrl", verticalHomeUrl);
			context.put("verticalHomeTitle", verticalHomeTitle);

			ret = evalService.thymeleafEval(context, content);
		} catch (IOException e) {
			LOGGER.error("Error while generating vertical file",e);
		}

		return ret;

	}

	/**
	 * Generate a vertical to a local file
	 *
	 * @param googleTaxonomyId
	 * @param matchingCategories
	 * @param urlPrefix
	 * @param h1Prefix
	 * @param verticalHomeUrl
	 * @param verticalHomeTitle
	 */
	public void verticalTemplatetoFile(String googleTaxonomyId, String matchingCategories, String urlPrefix, String h1Prefix, String verticalHomeUrl, String verticalHomeTitle) {

		// TODO(p3, conf) : from conf
		try {
			String id = IdHelper.normalizeFileName(googleTaxonomyService.byId(Integer.valueOf(googleTaxonomyId)).getGoogleNames().i18n("en"));

			File f = new File("/opt/open4goods/tmp/");
			f.mkdirs();
			f = new File(f.getAbsolutePath() + "/" + id + ".yml");

			FileUtils.write(f, verticalTemplate(id, googleTaxonomyId, matchingCategories, urlPrefix, h1Prefix, verticalHomeUrl, verticalHomeTitle));
		} catch (IOException e) {
			LOGGER.error("Error while writing template file for gtaxonomy {} ", googleTaxonomyId, e);
		}
	}

	/**
	 * A hacky method that hard updates the categorys from predicted ones in the vertical yaml files
	 * @param verticalFolderPath
	 * @param minOffers
	 */
	public void updateAllVerticalFileWithCategories(String verticalFolderPath, Integer minOffers) {
		LOGGER.warn("Will update categories in vertical files. Be sure to review before publishing on github !");
		List<File> files = Arrays.asList(new File(verticalFolderPath).listFiles());
		files.stream().filter(e->e.getName().endsWith("yml")).forEach(file -> {
			updateVerticalFileWithCategories(minOffers, file.getAbsolutePath());
		});

	}

	/**
	 * Update a vertical file with  categorys from predicted ones in the vertical yaml files
	 * @param minOffers
	 * @param fileName
	 * @return The new content of the file
	 */
	public String updateVerticalFileWithCategories(Integer minOffers, String fileName) {
		File file = new File(fileName);
		try {
			VerticalConfig vc = verticalConfigservice.getConfigById(file.getName().substring(0, file.getName().length()-4));
			LOGGER.warn("Will update {}",file.getName());
			String originalContent = FileUtils.readFileToString(file, Charset.defaultCharset());

			int startIndex = originalContent.indexOf("matchingCategories:");

			int endIndex = originalContent.indexOf("\n\n", startIndex);
			if (endIndex == -1) {
				endIndex = originalContent.length();
			}

			String newContent = originalContent.substring(0, startIndex);
			newContent += generateMapping(vc, minOffers);
			newContent += originalContent.substring(endIndex);

			FileUtils.writeStringToFile(file, newContent, Charset.defaultCharset());
			return newContent;

		} catch (IOException e1) {
			LOGGER.error("Error while updaing vertical file {}",file,e1);
			return "Error: " + e1.getMessage();
		}
	}



	public String updateVerticalFileWithImpactScore(String fileName) {
		File file = new File(fileName);
		try {
			// Expected filename format: vertical-id.yml
			String verticalId = file.getName().substring(0, file.getName().length()-4);
			VerticalConfig vc = verticalConfigservice.getConfigById(verticalId);
			if (vc == null) {
				return "Error: Vertical not found for id " + verticalId;
			}

			LOGGER.warn("Will update {} with impactscore", file.getName());

			String newContent = generateEcoscoreYamlConfig(vc);
			if (newContent == null) {
				return "Error: Failed to generate impact score config";
			}

			// For separate files, we overwrite the entire content
			FileUtils.writeStringToFile(file, newContent, Charset.defaultCharset());
			return newContent;

		} catch (IOException e1) {
			LOGGER.error("Error while updaing vertical file {}", file, e1);
			return "Error: " + e1.getMessage();
		}

	}



	/**
	 * Update a vertical file with predicted attributes  in the vertical yaml files
	 * @param minOffers
	 * @param fileName
	 * @param minCoverage
	 * @param containing
	 * @return The new content of the file
	 */
	public String updateVerticalFileWithAttributes(String fileName, int minCoverage, String containing) {
		File file = new File(fileName);
		try {
			VerticalConfig vc = verticalConfigservice.getConfigById(file.getName().substring(0, file.getName().length()-4));
			LOGGER.warn("Will update {} for attributes",file.getName());
			String originalContent = FileUtils.readFileToString(file, Charset.defaultCharset());

			int startIndex = originalContent.indexOf("  configs:");
			int endIndex = startIndex + "  configs:".length();

			String newContent = originalContent.substring(0, startIndex);
			newContent += "  configs:\n";
			newContent += generateAttributesMapping(vc, minCoverage, containing);
			newContent += originalContent.substring(endIndex);

			FileUtils.writeStringToFile(file, newContent, Charset.defaultCharset());
			return newContent;

		} catch (IOException e1) {
			LOGGER.error("Error while updaing vertical file {}",file,e1);
			return "Error: " + e1.getMessage();
		}
	}

	/**
	 * Update a vertical file with new nudge tool thresholds and impact score subsets.
	 *
	 * @param fileName the vertical configuration file path
	 * @return the updated file content
	 */
	public String updateVerticalFileWithNudgeToolConfig(String fileName) {
		File file = new File(fileName);
		try {
			String verticalId = file.getName().substring(0, file.getName().length() - 4);
			VerticalConfig vc = verticalConfigservice.getConfigById(verticalId);
			if (vc == null) {
				return "Error: Vertical not found for id " + verticalId;
			}

			LOGGER.warn("Will update {} for nudge tool thresholds", file.getName());
			String originalContent = FileUtils.readFileToString(file, Charset.defaultCharset());

			String updatedContent = updateNudgeToolScoreThresholds(originalContent, vc);
			updatedContent = updateImpactScoreSubsets(updatedContent, verticalId);

			FileUtils.writeStringToFile(file, updatedContent, Charset.defaultCharset());
			return updatedContent;
		} catch (IOException e1) {
			LOGGER.error("Error while updating vertical file {}", file, e1);
			return "Error: " + e1.getMessage();
		}
	}

	private String updateNudgeToolScoreThresholds(String content, VerticalConfig verticalConfig) {
		if (!content.contains("nudgeToolConfig:")) {
			LOGGER.info("No nudgeToolConfig section found for {}", verticalConfig.getId());
			return content;
		}

		List<NudgeToolScore> scores = verticalConfig.getNudgeToolConfig().getScores();
		if (scores == null || scores.isEmpty()) {
			LOGGER.info("No nudge tool scores configured for {}", verticalConfig.getId());
			return content;
		}

		String updated = content;
		for (NudgeToolScore score : scores) {
			if (StringUtils.isBlank(score.getScoreName())) {
				continue;
			}
			double threshold = computeThresholdForScore(verticalConfig.getId(), score.getScoreName(), SubsetCriteriaOperator.GREATER_THAN);
			updated = replaceScoreMinValue(updated, score.getScoreName(), threshold);
		}

		return updated;
	}

	private String updateImpactScoreSubsets(String content, String verticalId) {
		ScoreThresholds thresholds = computeImpactScoreThresholds(verticalId);
		String lowerThreshold = formatScoreValue(thresholds.lower());
		String upperThreshold = formatScoreValue(thresholds.upper());
		String rootImpactSubsets = buildImpactScoreSubsetsBlock("  ", lowerThreshold, upperThreshold);
		String nudgeImpactSubsets = buildImpactScoreSubsetsBlock("    ", lowerThreshold, upperThreshold);

		String updated = replaceImpactScoreSubsets(content, "  ", rootImpactSubsets);
		updated = replaceImpactScoreSubsets(updated, "    ", nudgeImpactSubsets);
		return updated;
	}

	private ScoreThresholds computeImpactScoreThresholds(String verticalId) {
		long total = repository.countMainIndexHavingScoreWithFilters(IMPACT_SCORE_NAME, verticalId);
		if (total <= 0) {
			LOGGER.info("No products found for impact score thresholds in {}", verticalId);
			return new ScoreThresholds(2.0, 4.0);
		}

		ScoreRange range = repository.getScoreRange(IMPACT_SCORE_NAME, verticalId, 100);
		double lowerThreshold = computeThresholdForScore(verticalId, IMPACT_SCORE_NAME, SubsetCriteriaOperator.LOWER_THAN, range);
		double upperThreshold = computeThresholdForScore(verticalId, IMPACT_SCORE_NAME, SubsetCriteriaOperator.GREATER_THAN, range);
		if (lowerThreshold >= upperThreshold) {
			LOGGER.warn("Invalid impact score thresholds for {}: {} >= {}", verticalId, lowerThreshold, upperThreshold);
			return new ScoreThresholds(range.min() + (range.max() - range.min()) / 3.0, range.min() + 2 * (range.max() - range.min()) / 3.0);
		}
		return new ScoreThresholds(lowerThreshold, upperThreshold);
	}

	private double computeThresholdForScore(String verticalId, String scoreName, SubsetCriteriaOperator operator) {
		ScoreRange range = repository.getScoreRange(scoreName, verticalId, 100);
		return computeThresholdForScore(verticalId, scoreName, operator, range);
	}

	private double computeThresholdForScore(String verticalId, String scoreName, SubsetCriteriaOperator operator, ScoreRange range) {
		long total = repository.countMainIndexHavingScoreWithFilters(scoreName, verticalId);
		if (total <= 0) {
			LOGGER.info("No products for score {} in {}", scoreName, verticalId);
			return (range.min() + range.max()) / 2.0;
		}

		double low = range.min();
		double high = range.max();
		double bestThreshold = (low + high) / 2.0;
		double bestDelta = Double.MAX_VALUE;

		for (int i = 0; i < MAX_THRESHOLD_ITERATIONS; i++) {
			double mid = (low + high) / 2.0;
			long count = repository.countMainIndexHavingScoreThreshold(scoreName, verticalId, operator, mid);
			double ratio = count / (double) total;
			double delta = Math.abs(ratio - TARGET_THRESHOLD_RATIO);

			if (delta < bestDelta) {
				bestDelta = delta;
				bestThreshold = mid;
			}

			if (delta <= ACCEPTABLE_RATIO_DELTA) {
				break;
			}

			// Binary search logic depends on the operator
			// For LOWER_THAN/LOWER_THAN_OR_EQUAL: increasing threshold increases count/ratio
			// For GREATER_THAN/GREATER_THAN_OR_EQUAL: increasing threshold decreases count/ratio
			boolean isDirect = (operator == SubsetCriteriaOperator.LOWER_THAN || operator == SubsetCriteriaOperator.LOWER_THAN_OR_EQUAL);
			boolean tooMany = ratio > TARGET_THRESHOLD_RATIO;

			if (isDirect) {
				if (tooMany) high = mid;
				else low = mid;
			} else {
				if (tooMany) low = mid;
				else high = mid;
			}
		}

		return BigDecimal.valueOf(bestThreshold).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}


	private String replaceScoreMinValue(String content, String scoreName, double threshold) {
		String formatted = formatScoreValue(threshold);
		Pattern pattern = Pattern.compile("(?s)(-\\s*scoreName:\\s*\"?" + Pattern.quote(scoreName) + "\"?\\s*\\n\\s*scoreMinValue:\\s*)([0-9.]+)");
		Matcher matcher = pattern.matcher(content);
		if (!matcher.find()) {
			LOGGER.warn("scoreMinValue not found for score {}", scoreName);
			return content;
		}
		return matcher.replaceAll("$1" + formatted);
	}

	private String formatScoreValue(double value) {
		BigDecimal decimal = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
		return decimal.toPlainString();
	}

	private String buildImpactScoreSubsetsBlock(String indent, String lowerThreshold, String upperThreshold) {
		StringBuilder builder = new StringBuilder();
		builder.append(indent).append("- id: \"impact_high\"").append("\n");
		builder.append(indent).append("  group: \"impactscore\"").append("\n");
		builder.append(indent).append("  criterias:").append("\n");
		builder.append(indent).append("    - field: \"scores.ECOSCORE.value\"").append("\n");
		builder.append(indent).append("      operator: \"LOWER_THAN_OR_EQUAL\"").append("\n");
		builder.append(indent).append("      value: \"").append(lowerThreshold).append("\"").append("\n");
		builder.append(indent).append("  image: \"example-image.png\"").append("\n");
		builder.append(indent).append("  url:").append("\n");
		builder.append(indent).append("    en: \"high-impact\"").append("\n");
		builder.append(indent).append("    fr: \"petit-impact\"").append("\n");
		builder.append(indent).append("  caption:").append("\n");
		builder.append(indent).append("    en: \"< ").append(lowerThreshold).append("\"").append("\n");
		builder.append(indent).append("    fr: \"< ").append(lowerThreshold).append("\"").append("\n");
		builder.append(indent).append("  title:").append("\n");
		builder.append(indent).append("    en: \"High \"").append("\n");
		builder.append(indent).append("    fr: \"Elevé\"").append("\n");
		builder.append(indent).append("    description:").append("\n");
		builder.append(indent).append("      en: \"Products with an ImpactScore lower than ").append(lowerThreshold).append("/5\"").append("\n");
		builder.append(indent).append("      fr: \"Produits avec un ImpactScore inférieur à ").append(lowerThreshold).append("/5\"").append("\n");
		builder.append(indent).append("\n");
		builder.append(indent).append("- id: \"impact_medium\"").append("\n");
		builder.append(indent).append("  group: \"impactscore\"").append("\n");
		builder.append(indent).append("  criterias:").append("\n");
		builder.append(indent).append("    - field: \"scores.ECOSCORE.value\"").append("\n");
		builder.append(indent).append("      operator: \"GREATER_THAN\"").append("\n");
		builder.append(indent).append("      value: \"").append(lowerThreshold).append("\"").append("\n");
		builder.append(indent).append("    - field: \"scores.ECOSCORE.value\"").append("\n");
		builder.append(indent).append("      operator: \"LOWER_THAN_OR_EQUAL\"").append("\n");
		builder.append(indent).append("      value: \"").append(upperThreshold).append("\"").append("\n");
		builder.append(indent).append("  image: \"example-image.png\"").append("\n");
		builder.append(indent).append("  url:").append("\n");
		builder.append(indent).append("    en: \"medium-impact\"").append("\n");
		builder.append(indent).append("    fr: \"impact-moyen\"").append("\n");
		builder.append(indent).append("  caption:").append("\n");
		builder.append(indent).append("    en: \"> ").append(lowerThreshold).append(" < ").append(upperThreshold).append("\"").append("\n");
		builder.append(indent).append("    fr: \"> ").append(lowerThreshold).append(" < ").append(upperThreshold).append("\"").append("\n");
		builder.append(indent).append("  title:").append("\n");
		builder.append(indent).append("    en: \"Medium\"").append("\n");
		builder.append(indent).append("    fr: \"Moyen\"").append("\n");
		builder.append(indent).append("    description:").append("\n");
		builder.append(indent).append("      en: \"Products with an ImpactScore between ").append(lowerThreshold).append("/5 and ").append(upperThreshold).append("/5\"").append("\n");
		builder.append(indent).append("      fr: \"Produits avec un ImpactScore compris entre ").append(lowerThreshold).append("/5 et ").append(upperThreshold).append("/5\"").append("\n");
		builder.append(indent).append("\n");
		builder.append(indent).append("- id: \"impact_low\"").append("\n");
		builder.append(indent).append("  group: \"impactscore\"").append("\n");
		builder.append(indent).append("  criterias:").append("\n");
		builder.append(indent).append("    - field: \"scores.ECOSCORE.value\"").append("\n");
		builder.append(indent).append("      operator: \"GREATER_THAN\"").append("\n");
		builder.append(indent).append("      value: \"").append(upperThreshold).append("\"").append("\n");
		builder.append(indent).append("  image: \"example-image.png\"").append("\n");
		builder.append(indent).append("  url:").append("\n");
		builder.append(indent).append("    en: \"impact-moderate\"").append("\n");
		builder.append(indent).append("    fr: \"impact-modere\"").append("\n");
		builder.append(indent).append("  caption:").append("\n");
		builder.append(indent).append("    en: \"> ").append(upperThreshold).append("\"").append("\n");
		builder.append(indent).append("    fr: \"> ").append(upperThreshold).append("\"").append("\n");
		builder.append(indent).append("  title:").append("\n");
		builder.append(indent).append("    en: \"Faible\"").append("\n");
		builder.append(indent).append("    fr: \"Faible\"").append("\n");
		builder.append(indent).append("    description:").append("\n");
		builder.append(indent).append("      en: \"Products with an ImpactScore greater than ").append(upperThreshold).append("/5\"").append("\n");
		builder.append(indent).append("      fr: \"Produits avec un ImpactScore supérieur à ").append(upperThreshold).append("/5\"").append("\n");
		return builder.toString();
	}

	private String replaceImpactScoreSubsets(String content, String indent, String replacement) {
		String startMarker = indent + "- id: \"impact_high\"";
		int startIndex = content.indexOf(startMarker);
		if (startIndex == -1) {
			startMarker = indent + "- id: impact_high";
			startIndex = content.indexOf(startMarker);
		}
		if (startIndex == -1) {
			return content;
		}

		String endMarker = indent + "- id: \"impact_low\"";
		int impactLowIndex = content.indexOf(endMarker, startIndex);
		if (impactLowIndex == -1) {
			endMarker = indent + "- id: impact_low";
			impactLowIndex = content.indexOf(endMarker, startIndex);
		}
		if (impactLowIndex == -1) {
			return content;
		}

		int nextIndex = content.indexOf("\n" + indent + "- id:", impactLowIndex + 1);
		int endIndex = nextIndex == -1 ? content.length() : nextIndex + 1;

		StringBuilder updated = new StringBuilder();
		updated.append(content, 0, startIndex);
		if (!content.substring(0, startIndex).endsWith("\n")) {
			updated.append("\n");
		}
		updated.append(replacement);
		updated.append(content.substring(endIndex));
		return updated.toString();
	}

	private record ScoreThresholds(double lower, double upper) {
	}


	/**
	 * Generate the advised attributes for a vertical
	 * @param minCoverage
	 * @param vertical
	 * @return
	 */
	/**
	 * Retrieve a set of attribute keys (and their synonyms) that are already defined in other verticals.
	 * This helps in standardizing attributes across verticals.
	 * @param currentVerticalId The ID of the vertical currently being processed (to exclude it).
	 * @return A set of strings representing known attribute keys and synonyms.
	 */
	private Set<String> getKnownAttributesFromOtherVerticals(String currentVerticalId) {
		Set<String> knownAttributes = new HashSet<>();
		verticalConfigservice.getConfigsWithoutDefault().stream()
			.filter(vc -> !vc.getId().equals(currentVerticalId)) // Exclude current vertical
			.forEach(vc -> {
				if (vc.getAttributesConfig() != null && vc.getAttributesConfig().getConfigs() != null) {
					vc.getAttributesConfig().getConfigs().forEach(attr -> {
						knownAttributes.add(attr.getKey());
						if (attr.getSynonyms() != null) {
							attr.getSynonyms().values().forEach(syns -> knownAttributes.addAll(syns));
						}
					});
				}
			});
		return knownAttributes;
	}

	public String generateAttributesMapping(VerticalConfig verticalConfig, int minCoverage, String containing) {
		LOGGER.info("Generating attributes mapping for {}", verticalConfig);
		VerticalAttributesStats stats = attributesStats(verticalConfig.getId());

		Set<String> exclusions = new HashSet<String>();
		if (null != verticalConfig.getGenerationExcludedFromAttributesMatching()) {
			exclusions.addAll(verticalConfig.getGenerationExcludedFromAttributesMatching());
		}

		// Adding existing defined attributes matching
		verticalConfig.getAttributesConfig().getConfigs().stream().map(e->e.getSynonyms().values()).forEach(e -> {
			e.forEach(elem -> {
				elem.forEach(e1 -> {
					exclusions.add(e1);
				});
			});
		});

		Set<String> knownAttributes = getKnownAttributesFromOtherVerticals(verticalConfig.getId());

		int totalItems = stats.getTotalItems();

		StringBuilder ret = new StringBuilder();
		for (Entry<String, AttributesStats> cat : stats.getStats().entrySet()) {

			if (!exclusions.contains(cat.getKey())) {
				boolean isKnown = knownAttributes.contains(cat.getKey());
				int coveragePercent = Double.valueOf(cat.getValue().getHits() / Double.valueOf(totalItems) * 100.0).intValue();

				if (isKnown || coveragePercent > minCoverage) {
					LOGGER.info("Generating template for attribute : {} (Known: {}, Coverage: {}%)", cat.getKey(), isKnown, coveragePercent);
					// TODO(conf,p2) : numberofsamples from conf
					if (StringUtils.isEmpty(containing) || cat.getKey().toLowerCase().contains(containing.toLowerCase())) {
						ret.append(attributeConfigTemplate(cat, totalItems,10));
					}
				} else {
					LOGGER.info("Skipping {}, not enough coverage", cat.getKey());
				}
			}
		}
		return ret.toString();

	}

	/**
	 * Generate the available impact score criterias YAML fragment for a vertical.
	 * Criteria candidates are collected from other verticals (including _default),
	 * then filtered by coverage against products of the target vertical.
	 *
	 * @param verticalConfig the target vertical configuration
	 * @param minCoveragePercent minimum coverage percentage required
	 * @return YAML fragment with comments describing coverage
	 */
	public String generateAvailableImpactScoreCriteriasFragment(VerticalConfig verticalConfig, int minCoveragePercent) {
		Objects.requireNonNull(verticalConfig, "verticalConfig is required");
		String verticalId = verticalConfig.getId();
		Set<String> candidates = collectImpactScoreCandidates(verticalId);
		if (candidates.isEmpty()) {
			LOGGER.info("No impact score candidates found for {}", verticalId);
			return "availableImpactScoreCriterias:\n";
		}

		long total = repository.countMainIndexHavingVertical(verticalId);
		if (total <= 0) {
			LOGGER.info("No products found for vertical {}", verticalId);
			return "availableImpactScoreCriterias:\n";
		}

		StringBuilder builder = new StringBuilder("availableImpactScoreCriterias:\n");
		candidates.stream()
			.filter(StringUtils::isNotBlank)
			.sorted()
			.forEach(scoreKey -> {
				Long count = repository.countMainIndexHavingScore(scoreKey, verticalId);
				long hits = count == null ? 0L : count;
				int coveragePercent = Double.valueOf(hits / (double) total * 100.0).intValue();
				if (coveragePercent >= minCoveragePercent) {
					builder.append("  # coverage: ")
						.append(coveragePercent)
						.append("% (")
						.append(hits)
						.append("/")
						.append(total)
						.append(")\n");
					builder.append("  - ").append(scoreKey).append("\n");
				} else {
					LOGGER.info("Skipping impact score criteria {} with coverage {}%", scoreKey, coveragePercent);
				}
			});

		return builder.toString();
	}

	private Set<String> collectImpactScoreCandidates(String targetVerticalId) {
		Set<String> candidates = new HashSet<>();
		List<VerticalConfig> configs = new ArrayList<>(verticalConfigservice.getConfigsWithoutDefault());
		VerticalConfig defaultConfig = verticalConfigservice.getDefaultConfig();
		if (defaultConfig != null) {
			configs.add(defaultConfig);
		}

		for (VerticalConfig config : configs) {
			if (config == null) {
				continue;
			}
			if (StringUtils.equals(targetVerticalId, config.getId())) {
				continue;
			}
			addImpactScoreCandidatesFromConfig(config, candidates);
		}

		return candidates;
	}

	private void addImpactScoreCandidatesFromConfig(VerticalConfig config, Set<String> candidates) {
		AttributesConfig attributesConfig = config.getAttributesConfig();
		if (attributesConfig != null && attributesConfig.getConfigs() != null) {
			attributesConfig.getConfigs().stream()
				.filter(AttributeConfig::isAsScore)
				.map(AttributeConfig::getKey)
				.filter(StringUtils::isNotBlank)
				.forEach(candidates::add);
		}

		if (config.getAvailableImpactScoreCriterias() != null) {
			config.getAvailableImpactScoreCriterias().stream()
				.filter(StringUtils::isNotBlank)
				.forEach(candidates::add);
		}

		ImpactScoreConfig impactScoreConfig = config.getImpactScoreConfig();
		if (impactScoreConfig != null && impactScoreConfig.getCriteriasPonderation() != null) {
			impactScoreConfig.getCriteriasPonderation().keySet().stream()
				.filter(StringUtils::isNotBlank)
				.forEach(candidates::add);
		}
	}


	/**
	 * Generates the AI yaml config defining an ecoscore for a given category
	 * @param vConf
	 * @return
	 */
	public String generateEcoscoreYamlConfig (VerticalConfig vConf) {
		// Translate to YAML
		String ret = null;
		try {
			Map<String, Object> context = new HashMap<String, Object>();

			// TODO : Enforce, log

			context.put("AVAILABLE_CRITERIAS", getCriterias(vConf));
			context.put("VERTICAL_NAME", vConf.getI18n().get("fr").getVerticalHomeTitle());

			// Prompt
			PromptResponse<org.open4goods.model.ai.ImpactScoreAiResult> response = genAiService.objectPrompt("impactscore-generation", context, org.open4goods.model.ai.ImpactScoreAiResult.class);

			ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
			impactScoreConfig.setAiResult(response.getBody());

			// Legacy mapping
			Map<String, Double> criteriasPonderation = new HashMap<>();
			if (response.getBody() != null && response.getBody().getCriteriaWeights() != null) {
				for (org.open4goods.model.ai.ImpactScoreAiResult.CriteriaWeight cw : response.getBody().getCriteriaWeights()) {
					criteriasPonderation.put(cw.criterion, cw.weight);
				}
			}
			impactScoreConfig.setCriteriasPonderation(criteriasPonderation);


			// Completing
			impactScoreConfig.setYamlPrompt(serialisationService.toYaml(response.getPrompt()));
			impactScoreConfig.setAiJsonResponse(serialisationService.toJson(response.getBody(),true));

			// Directly serialize the config object, as it is now a standalone file
			ret = serialisationService.toYaml(impactScoreConfig).replace("---", "");


		} catch (Exception e) {
			LOGGER.error("Ecoscore Generation failed for {} ",vConf, e);
		}

		return ret;

	}

	/**
	 * Return known criterias with description for a vertical
	 * @param vConf
	 * @return
	 */
	private String getCriterias(VerticalConfig vConf) {
			Map<String, Long> criterias = repository.scoresCoverage(vConf);


			StringBuilder ret = new StringBuilder();

                        criterias.entrySet().forEach(score -> {
                                ret.append("  ").append(score.getKey()).append(" : ");

                                Optional.ofNullable(vConf.getAttributesConfig())
                                        .map(config -> config.getAttributeConfigByKey(score.getKey()))
                                        .map(AttributeConfig::getScoreDescription)
                                        .map(description -> description.get("fr"))
                                        .ifPresentOrElse(ret::append, () -> ret.append(score.getKey()));

                                ret.append("\n");
                        });

			return ret.toString();
		}

//	/**
//	 *
//	 * @return an attribute definition, from template
//	 */
	public String attributeConfigTemplate(Entry<String, AttributesStats> category, Integer totalItems, Integer maxSample)  {
		String ret = "";
		try {
			Resource r = resourceResolver.getResource("classpath:/templates/attribute-definition.yml");
			String content = r.getContentAsString(Charset.defaultCharset());

			Map<String, Object> context = new HashMap<String, Object>();

			context.put("name",category.getKey());
			context.put("date", DateFormat.getInstance().format(new Date()));
			context.put("coveragePercent",  Double.valueOf(category.getValue().getHits() / Double.valueOf(totalItems) * 100.0).intValue());
			context.put("attrHits",category.getValue().getHits());
			context.put("totalHits",totalItems);

			context.put("faicon","");

//			context.put("",);
//			context.put("",);
//			context.put("",);


			Set<Integer> icecatIds = icecatService.resolveFeatureName(category.getKey());
			String default_name = "!!COMPLETE_HERE!!";
			String fr_name = "!!COMPLETE_HERE!!";
			String icecatIdsTpl = StringUtils.join(icecatIds,"\n  -") ;

			if (null != icecatIds && icecatIds.size() > 0) {

				if (icecatIds.size() == 1) {
					default_name=icecatService.getFeatureName(icecatIds.stream().findAny().orElse(null), "default");
					fr_name=icecatService.getFeatureName(icecatIds.stream().findAny().orElse(null), "fr");
					context.put("icecatIds" , icecatIds.stream().findAny().orElse(null));

				} else {
					LOGGER.warn("Multiple possibilities to name attribute {} : {}", category.getKey(), icecatIds);
					default_name=icecatService.getFeatureName(icecatIds.stream().findAny().orElse(null), "default");
					fr_name=icecatService.getFeatureName(icecatIds.stream().findAny().orElse(null), "fr");
					context.put("icecatIds","!!!!" + icecatIds.toString());

				}

			} else {
				context.put("icecatIds", "TODO");
			}

			context.put("default_name",default_name);
			context.put("fr_name",fr_name);

			// Type
			boolean onlyNumeric = category.getValue().getValues().keySet().stream().filter(e -> !NumberUtils.isNumber(e)).toList().size()==0;
			if (onlyNumeric) {
				context.put("type","NUMERIC");
			} else {
				context.put("type","STRING");
			}

			StringBuilder attrsSamples = new StringBuilder();

			// Attribute sample values
			category.getValue().getValues().entrySet().stream().limit(maxSample).forEach(val -> {
				attrsSamples.append("#     - ").append(val.getKey()).append(" (").append(val.getValue()).append(" items)\n");
			});

			if (category.getValue().getValues().size()>maxSample) {
				attrsSamples.append("#     + ").append(category.getValue().getValues().size() - maxSample).append(" more attributes...\n");
			}

			context.put("attributesSamples",attrsSamples.toString());


			//			synonyms
			// Attribute sample values

			Set<String> datasources = new HashSet<String>();
			category.getValue().getDatasourceNames().forEach(val -> {
				datasources.add(val);
			});


			StringBuilder mapping = new StringBuilder("");

			datasources.forEach(e->{
				mapping.append("      "). append(e).append(":\n");
				mapping.append("        - \""). append(category.getKey()).append("\"\n");

			});
			context.put("synonyms",mapping.toString());

//
			ret = evalService.thymeleafEval(context, content);
		} catch (IOException e) {
			LOGGER.error("Error while generating vertical file",e);
		}


		String[] lines = ret.split("\n");


		// Commenting
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append("\n");
			if (!line.trim().startsWith("#")) {
				sb.append("#");
			}
			sb.append(line);
		}

		return sb.toString();

	}
//


}
