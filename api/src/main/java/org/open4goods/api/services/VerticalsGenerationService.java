package org.open4goods.api.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.VerticalsGenerationConfig;
import org.open4goods.api.model.AttributesStats;
import org.open4goods.api.model.VerticalAttributesStats;
import org.open4goods.api.model.VerticalCategoryMapping;
import org.open4goods.commons.services.GoogleTaxonomyService;
import org.open4goods.commons.services.IcecatService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

public class VerticalsGenerationService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalsGenerationService.class);
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
	
//	public void fullFromDb() throws IOException {
//
//		importMappingFile();
//		LOGGER.info("loaded . {} mappings",	sortedMappings.size());
//
//		removeByLowHits();
//		LOGGER.info("threshold-low-hits. {} mappings",	sortedMappings.size());
//		
//		removeByAssociatedcategoryThreshold();
//		LOGGER.info("threshold-clean . {} mappings",	sortedMappings.size());
//
//		orderMappings();
//		
//		removeCrossReferencedMappings();
//		LOGGER.info("cross-reference-clean . {} mappings",	sortedMappings.size());
//
//		orderMappings();
//		exportMappingFile();
//		
//	}
//	
//	/**
//	 * Main method, that deduce verticals and load it in memory
//	 */
//	public void loadCategoriesMappingFromDatabase () {
//		
//		
//		/////////////////////////////////////
//		// 1 - Select all products
//		// TODO : Filter by mandatory attributes
//		/////////////////////////////////////
//		
//		// TODO : Filter, limit not working
//		Stream<Product> products = repository.exportForCategoriesMapping(config.getMustExistsFields(), config.getLimit()).parallel();
//		
//		if (config.getLimit() != null) {
//			products=products.limit(config.getLimit());
//		}
//		
//		//////////////////////////////////////////////////////////////////////////
//		// 2 - Iterate on each product, to constitute the mapped attributes map 
//		//   >> 
//		//////////////////////////////////////////////////////////////////////////
//		AtomicInteger counter = new AtomicInteger();
//		products.forEach(product -> {
//			int count = counter.incrementAndGet();
//			if (count % 1000 == 0) {
//				LOGGER.warn ("Handled {} items",count);
//			}
//			
//			Map<String, String> categories = product.getCategoriesByDatasources();
//			categories.entrySet().forEach(category -> {
//				// TODO : Exclude some datasources from conf
//				
//				// Retrieving existing mapping for a given categoryPath or creating
//				VerticalCategoryMapping existingMapping = sortedMappings.get(category.getValue());
//				if (null == existingMapping) {
//					existingMapping = new VerticalCategoryMapping();
//					sortedMappings.put(category.getValue(), existingMapping);
//				}
//				// Updating stats for this 
//				existingMapping.updateStats(category.getValue(), categories.values());
//			});
//		});
//
//		LOGGER.info("{} category associated mappings constructed",sortedMappings.size());
//		
//	}
//
//
//
//	/**
//	 * Order the mappings by total hits
//	 */
//	private void orderMappings() {
//		this.sortedMappings  = sortedMappings.entrySet().stream()
//				.map(e -> {	
//					// Sorting the matchings by weight
//					e.getValue().setAssociatedCategories(getSortedAssociatedCategories(e.getValue().getAssociatedCategories()));
//					return e;
//				})
//			    .sorted(Comparator.comparing(entry -> entry.getValue().getTotalHits(), Comparator.reverseOrder()))
//		        .collect(Collectors.toMap(
//			            Map.Entry::getKey,
//			            Map.Entry::getValue,
//			            (e1, e2) -> e1, // in case of duplicate keys, keep the existing one
//			            LinkedHashMap::new // maintain insertion order for sorted entries
//			        ));
//	}
//	
//	/**
//	 * Deduplicates associated categories in each VerticalCategoryMapping object 
//	 * by removing entries with a value below a calculated threshold
//	 * 
//	 * @param mappings Map of VerticalCategoryMapping objects keyed by String
//	 */
//	public void removeByAssociatedcategoryThreshold() {
//
//	    new HashMap<>(sortedMappings).entrySet().stream().forEach(entryKeyVal -> {
//	    	VerticalCategoryMapping currentValue = entryKeyVal.getValue();
//	        // Set threshold to X% of the total hits
//	        double threshold = currentValue.getTotalHits() * config.getAssociatedCategoriesEvictionPercent();
//	        
//	        // Use an iterator to safely remove entries below the threshold
//	        Iterator<Map.Entry<String, Long>> iterator = currentValue.getAssociatedCategories().entrySet().iterator();
//	        while (iterator.hasNext()) {
//	            Map.Entry<String, Long> entry = iterator.next();
//	            if (entry.getValue() < threshold) {
//	                iterator.remove(); // Safely remove entry from the map
//	            }
//	        }
//	    });
//	}
//
//	/**
//	 * Deduplicates associated categories in each VerticalCategoryMapping object 
//	 * by removing entries with a value below a calculated threshold
//	 * 
//	 * @param mappings Map of VerticalCategoryMapping objects keyed by String
//	 */
//	public void removeByLowHits() {
//
//	   sortedMappings = sortedMappings.entrySet().stream()
//			   	.filter(e -> e.getValue().getTotalHits() > config.getMinimumTotalHits())
//			   	.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
//	}
//
//	
//	
//	/**
//	 * Remove the cross linked mappings
//	 * NOTE : heavy load, a one to one comparison on lot of keys
//	 * @param mappings
//	 * @param currentValue 
//	 */
//	public void removeCrossReferencedMappings() {
//		sortedMappings.entrySet().forEach(actual -> {
//
//			if (!actual.getValue().getToDelete()) {
//
//				actual.getValue().setKeep(true);
//				// We are on an item.
//				sortedMappings.entrySet().stream()
//						// We exclude previously handled item
//						.filter(e -> !e.getValue().getKeep())
//						// Find all in the order mapping that collapse definition with the actual one.
//						// We mark them as force
//						.filter(e -> hasOverlap(actual, e))
//						.forEach(e -> e.getValue().setToDelete(true));
//			}
//
//		});
//
//		// Cleaning
//		sortedMappings = sortedMappings.entrySet().stream().filter(e -> e.getValue().getKeep()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//	}
//	
//	/**
//	 * Return true if a target VerticalCategoryMapping has name or associated categories that matches the actual VerticalCategoryMapping
//	 * @param actual
//	 * @param target
//	 * @return
//	 */
//	private boolean hasOverlap(Entry<String, VerticalCategoryMapping> actual, Entry<String, VerticalCategoryMapping> target) {
//		
//		boolean ret = target.getValue().getAssociatedCategories().containsKey(actual.getKey()) 
////				||  target.getValue().getAssociatedCategories().keySet().containsAll(actual.getValue().getAssociatedCategories().keySet())
//				;
//		
//		
//		return ret;
//				
//	}
//
//	/**
//	 * Sort the map of associated categories
//	 * @param map
//	 * @return
//	 */
//	public Map<String, Long> getSortedAssociatedCategories(Map<String, Long> map) {
//	    return map.entrySet().stream()
//	        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
//	        .collect(Collectors.toMap(
//	            Map.Entry::getKey,
//	            Map.Entry::getValue,
//	            (e1, e2) -> e1, // in case of duplicate keys, keep the existing one
//	            LinkedHashMap::new // maintain insertion order for sorted entries
//	        ));
//	}
//	/**
//	 * Export mapping file to disk
//	 * @throws IOException
//	 */
//	public void exportMappingFile() throws IOException {
//		if (sortedMappings.size() > 0) {
//			org.apache.commons.io.FileUtils.write(new File(config.getMappingFilePath()), serialisationService.toJson(sortedMappings,true));
//		}
//	}
//
//
//	/**
//	 * Import categories mapping from file
//	 * @throws IOException
//	 */
//	public void importMappingFile() throws IOException {
//	    String fileContent = org.apache.commons.io.FileUtils.readFileToString(
//	        new File(config.getMappingFilePath()), 
//	        Charset.defaultCharset()
//	    );
//
//	    // Deserialize with proper typing
//	    sortedMappings = serialisationService.getJsonMapper().readValue(
//	        fileContent, 
//	        new TypeReference<LinkedHashMap<String, VerticalCategoryMapping>>() {}
//	    );
//	}
//
//	
	/**
	 * 
	 * @return the mappings
	 */
	public Map<String, VerticalCategoryMapping> getMappings() {
		return sortedMappings;
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
	 */
	public void updateVerticalFileWithCategories(Integer minOffers, String fileName) {
		File file = new File(fileName);
		try {
			VerticalConfig vc = verticalConfigservice.getConfigById(file.getName().substring(0, file.getName().length()-4));
			LOGGER.warn("Will update {}",file.getName());
			String originalContent = FileUtils.readFileToString(file);
			
			int startIndex = originalContent.indexOf("matchingCategories:");
			int endIndex = originalContent.indexOf("\n\n", startIndex);
			
			String newContent = originalContent.substring(0, startIndex);
			newContent += generateMapping(vc, minOffers);
			newContent += originalContent.substring(endIndex);
			
			FileUtils.writeStringToFile(file, newContent, Charset.defaultCharset());
			
		} catch (IOException e1) {
			LOGGER.error("Error while updaing vertical file {}",file,e1);
		}
	}
	
	

	public void updateVerticalFileWithImpactScore(String fileName) {
		File file = new File(fileName);
		try {
			VerticalConfig vc = verticalConfigservice.getConfigById(file.getName().substring(0, file.getName().length()-4));
			LOGGER.warn("Will update {} with impactscore",file.getName());
			String originalContent = FileUtils.readFileToString(file);
			
			int startIndex = originalContent.indexOf("impactScoreConfig:");
			int endIndex = originalContent.indexOf("\n\n", startIndex);
			
			if (-1 == startIndex) {
				startIndex = originalContent.length();
				endIndex=originalContent.length();
			}
				
			if (endIndex == -1) {
				endIndex=originalContent.length();
			}
			
			
			String newContent = originalContent.substring(0, startIndex);
			newContent += generateEcoscoreYamlConfig(vc);
			newContent += originalContent.substring(endIndex);
			
			FileUtils.writeStringToFile(file, newContent, Charset.defaultCharset());
			
		} catch (IOException e1) {
			LOGGER.error("Error while updaing vertical file {}",file,e1);
		}
		
	}
	
	
	
	/**
	 * Update a vertical file with predicted attributes  in the vertical yaml files
	 * @param minOffers
	 * @param fileName
	 * @param minCoverage 
	 * @param containing 
	 */
	public void updateVerticalFileWithAttributes(String fileName, int minCoverage, String containing) {
		File file = new File(fileName);
		try {
			VerticalConfig vc = verticalConfigservice.getConfigById(file.getName().substring(0, file.getName().length()-4));
			LOGGER.warn("Will update {} for attributes",file.getName());
			String originalContent = FileUtils.readFileToString(file);
			
			int startIndex = originalContent.indexOf("  configs:");
			int endIndex = startIndex + "  configs:".length();
			
			String newContent = originalContent.substring(0, startIndex);
			newContent += "  configs:\n";
			newContent += generateAttributesMapping(vc, minCoverage, containing);
			newContent += originalContent.substring(endIndex);
			
			FileUtils.writeStringToFile(file, newContent, Charset.defaultCharset());
			
		} catch (IOException e1) {
			LOGGER.error("Error while updaing vertical file {}",file,e1);
		}
	}
	
	
	/**
	 * Generate the advised attributes for a vertical
	 * @param minCoverage 
	 * @param vertical
	 * @return
	 */
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
		
		
		
		int totalItems = stats.getTotalItems();
		
		String ret = "";
		for (Entry<String, AttributesStats> cat : stats.getStats().entrySet()) {
			
			if (!exclusions.contains(cat.getKey())) {
				if (Double.valueOf(cat.getValue().getHits() / Double.valueOf(totalItems) * 100.0).intValue() > minCoverage) {
					LOGGER.info("Generating template for attribute : {}", cat.getKey());
					// TODO(conf,p2) : numberofsamples from conf
					if (StringUtils.isEmpty(containing) || cat.getKey().toLowerCase().contains(containing.toLowerCase())) {
						ret += attributeConfigTemplate(cat, totalItems,10);											
					}
				} else {
					LOGGER.info("Skipping {}, not enough coverage", cat.getKey());
				}
			}
		}
		return ret;
		
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
			
			context.put("AVAILABLE_CRITERIAS", getCriterias(vConf));
			context.put("VERTICAL_NAME", vConf.getI18n().get("fr").getVerticalHomeTitle());
			
			// Prompt 
			PromptResponse<Map<String, Object>> response = genAiService.jsonPrompt("impactscore-generation", context);
			
			// TODO(p2, safety) : To strictYaml
			String rawRet = serialisationService.toYaml(response.getBody());
			ImpactScoreConfig impactScoreConfig = serialisationService.fromYaml(rawRet, ImpactScoreConfig.class);
			
			// Completing
			impactScoreConfig.setYamlPrompt(serialisationService.toYaml(response.getPrompt()));
			impactScoreConfig.setAiJsonResponse(serialisationService.toJson(response.getBody(),true));
			
			
			// Setting prompt and response
			
			rawRet = rawRet.replace("---", "");
			
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("impactScoreConfig", impactScoreConfig);
			
//			StringBuilder buffer = new StringBuilder("impactScoreConfig:\n");
//			buffer.append("# Generated with AI on " ).append(new Date().toLocaleString()).append("\n");
//			Arrays.asList(rawRet.split("\n")).forEach(line -> {
//				buffer.append("  ").append(line).append("\n");
//				
//			});
			
			ret = serialisationService.toYaml(map).replace("---", "");
			
			
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
				ret.append("  ").append(score.getKey()).append(" : " );
				// NOTE : Spank me if NPE...
				ret.append(vConf.getAvailableImpactScoreCriterias().get(score.getKey()).getDescription().get("fr"));
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
				
			}
			
			// Adding icecatIds
			if (null != icecatIds) {
				
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
