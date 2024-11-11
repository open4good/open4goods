package org.open4goods.api.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.VerticalsGenerationConfig;
import org.open4goods.api.model.VerticalCategoryMapping;
import org.open4goods.commons.config.yml.ui.ProductI18nElements;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.GoogleTaxonomyService;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.services.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.util.logging.Log;

public class VerticalsGenerationService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalsGenerationService.class);
	private VerticalsGenerationConfig config;
	private ProductRepository repository;
	private SerialisationService serialisationService;

	

	private Map<String, VerticalCategoryMapping> sortedMappings = new LinkedHashMap<String, VerticalCategoryMapping>();
	private AiService aiService;
	private GoogleTaxonomyService googleTaxonomyService;
	
	public VerticalsGenerationService(VerticalsGenerationConfig config, ProductRepository repository, SerialisationService serialisationService, AiService aiService, GoogleTaxonomyService googleTaxonomyService) {
		super();
		this.config = config;
		this.repository = repository;
		this.serialisationService = serialisationService;
		this.aiService = aiService;
		this.googleTaxonomyService = googleTaxonomyService;
	}
	
	
	
	public void fullFromDb() throws IOException {
		

		loadCategoriesMappingFromDatabase();
		LOGGER.info("loaded . {} mappings",	sortedMappings.size());
		

		removeByLowHits();
		LOGGER.info("threshold-low-hits. {} mappings",	sortedMappings.size());
		
		
		removeByAssociatedcategoryThreshold();
		LOGGER.info("threshold-clean . {} mappings",	sortedMappings.size());


		orderMappings();
		
		removeCrossReferencedMappings();
		LOGGER.info("cross-reference-clean . {} mappings",	sortedMappings.size());
		

		orderMappings();
		exportMappingFile();
		
	}
	
	/**
	 * Main method, that deduce verticals and load it in memory
	 */
	public void loadCategoriesMappingFromDatabase () {
		
		
		/////////////////////////////////////
		// 1 - Select all products
		// TODO : Filter by mandatory attributes
		/////////////////////////////////////
		
		// TODO : Filter, limit not working
		Stream<Product> products = repository.exportForCategoriesMapping(config.getMustExistsFields(), config.getLimit()).parallel();
		
		if (config.getLimit() != null) {
			products=products.limit(config.getLimit());
		}
		
		//////////////////////////////////////////////////////////////////////////
		// 2 - Iterate on each product, to constitute the mapped attributes map 
		//   >> 
		//////////////////////////////////////////////////////////////////////////
		AtomicInteger counter = new AtomicInteger();
		products.forEach(product -> {
			int count = counter.incrementAndGet();
			if (count % 1000 == 0) {
				LOGGER.warn ("Handled {} items",count);
			}
			
			Map<String, String> categories = product.getCategoriesByDatasources();
			categories.entrySet().forEach(category -> {
				// TODO : Exclude some datasources from conf
				
				// Retrieving existing mapping for a given categoryPath or creating
				VerticalCategoryMapping existingMapping = sortedMappings.get(category.getValue());
				if (null == existingMapping) {
					existingMapping = new VerticalCategoryMapping();
					sortedMappings.put(category.getValue(), existingMapping);
				}
				// Updating stats for this 
				existingMapping.updateStats(category.getValue(), categories.values());
			});
		});

		LOGGER.info("{} category associated mappings constructed",sortedMappings.size());
		
	}



	/**
	 * Order the mappings by total hits
	 */
	private void orderMappings() {
		this.sortedMappings  = sortedMappings.entrySet().stream()
				.map(e -> {	
					// Sorting the matchings by weight
					e.getValue().setAssociatedCategories(getSortedAssociatedCategories(e.getValue().getAssociatedCategories()));
					return e;
				})
			    .sorted(Comparator.comparing(entry -> entry.getValue().getTotalHits(), Comparator.reverseOrder()))
		        .collect(Collectors.toMap(
			            Map.Entry::getKey,
			            Map.Entry::getValue,
			            (e1, e2) -> e1, // in case of duplicate keys, keep the existing one
			            LinkedHashMap::new // maintain insertion order for sorted entries
			        ));
	}
	
	/**
	 * Deduplicates associated categories in each VerticalCategoryMapping object 
	 * by removing entries with a value below a calculated threshold
	 * 
	 * @param mappings Map of VerticalCategoryMapping objects keyed by String
	 */
	public void removeByAssociatedcategoryThreshold() {

	    new HashMap<>(sortedMappings).entrySet().stream().forEach(entryKeyVal -> {
	    	VerticalCategoryMapping currentValue = entryKeyVal.getValue();
	        // Set threshold to X% of the total hits
	        double threshold = currentValue.getTotalHits() * config.getAssociatedCatgoriesEvictionPercent();
	        
	        // Use an iterator to safely remove entries below the threshold
	        Iterator<Map.Entry<String, Long>> iterator = currentValue.getAssociatedCategories().entrySet().iterator();
	        while (iterator.hasNext()) {
	            Map.Entry<String, Long> entry = iterator.next();
	            if (entry.getValue() < threshold) {
	                iterator.remove(); // Safely remove entry from the map
	            }
	            
	        }
	    });
	}

	/**
	 * Deduplicates associated categories in each VerticalCategoryMapping object 
	 * by removing entries with a value below a calculated threshold
	 * 
	 * @param mappings Map of VerticalCategoryMapping objects keyed by String
	 */
	public void removeByLowHits() {

	   sortedMappings = sortedMappings.entrySet().stream()
			   	.filter(e -> e.getValue().getTotalHits() > config.getMinimumTotalHits())
			   	.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
	}

	
	
	/**
	 * Remove the cross linked mappings
	 * NOTE : heavy load, a one to one comparison on lot of keys
	 * @param mappings
	 * @param currentValue 
	 */
	public void removeCrossReferencedMappings() {
		sortedMappings.entrySet().forEach(actual -> {

			if (!actual.getValue().getToDelete()) {

				actual.getValue().setKeep(true);
				// We are on an item.
				sortedMappings.entrySet().stream()
						// We exclude previously handled item
						.filter(e -> !e.getValue().getKeep())
						// Find all in the order mapping that collapse definition with the actual one.
						// We mark them as force
						.filter(e -> hasOverlap(actual, e))
						.forEach(e -> e.getValue().setToDelete(true));
			}

		});

		// Cleaning
		sortedMappings = sortedMappings.entrySet().stream().filter(e -> e.getValue().getKeep()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

	}
	
	/**
	 * Return true if a target VerticalCategoryMapping has name or associated categories that matches the actual VerticalCategoryMapping
	 * @param actual
	 * @param target
	 * @return
	 */
	private boolean hasOverlap(Entry<String, VerticalCategoryMapping> actual, Entry<String, VerticalCategoryMapping> target) {
		
		boolean ret = target.getValue().getAssociatedCategories().containsKey(actual.getKey()) 
//				||  target.getValue().getAssociatedCategories().keySet().containsAll(actual.getValue().getAssociatedCategories().keySet())
				;
		
		
		return ret;
				
	}

	/**
	 * Sort the map of associated categories
	 * @param map
	 * @return
	 */
	public Map<String, Long> getSortedAssociatedCategories(Map<String, Long> map) {
	    return map.entrySet().stream()
	        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
	        .collect(Collectors.toMap(
	            Map.Entry::getKey,
	            Map.Entry::getValue,
	            (e1, e2) -> e1, // in case of duplicate keys, keep the existing one
	            LinkedHashMap::new // maintain insertion order for sorted entries
	        ));
	}
	/**
	 * Export mapping file to disk
	 * @throws IOException
	 */
	public void exportMappingFile() throws IOException {
		if (sortedMappings.size() > 0) {
			org.apache.commons.io.FileUtils.write(new File(config.getMappingFilePath()), serialisationService.toJson(sortedMappings,true));
		}
	}

	/**
	 * Load the mapping file from disk
	 * @throws IOException
	 */
	public void importMappingFile() throws IOException {
		sortedMappings= serialisationService.fromJson(org.apache.commons.io.FileUtils.readFileToString(new File(config.getMappingFilePath()), Charset.defaultCharset()), sortedMappings.getClass());
	}
	
	
	/**
	 * 
	 * @return the mappings
	 */
	public Map<String, VerticalCategoryMapping> getMappings() {
		return sortedMappings;
	}
	
	
	public List<VerticalConfig> generateVerticals() {
		List<VerticalConfig> ret = new ArrayList<VerticalConfig>();
		
		sortedMappings.entrySet().stream()
			.filter(e->e.getValue().getAssociatedCategories().size() > 3)
			.forEach(cat -> {				
				ret.add(generateVertical(cat));				
			});
		return ret;
	}



	/**
	 * Generate a vertical stub, using our matching categories detected and adding informations through AI
	 * @param cat
	 * @return
	 */
	private VerticalConfig generateVertical(Entry<String, VerticalCategoryMapping> cat) {
		
		VerticalConfig v = new VerticalConfig();
		v.getMatchingCategories().add(cat.getKey());
		v.getMatchingCategories().addAll(cat.getValue().getAssociatedCategories().keySet());
		
		Map<String, String> datas = aiDatas(v);
		
		Integer resolvedTaxonomy = resolveGoogleTaxonomy(datas.get("googleTaxonomy"));
		if (null != resolvedTaxonomy) {
			v.setGoogleTaxonomyId(resolvedTaxonomy);
			LOGGER.warn("solved taxonomy for {} - {} ({})", cat.getKey(), datas,googleTaxonomyService.getTaxonomyName(resolvedTaxonomy) );
			
			String englishName = datas.get("englishName");
			String frenchName = datas.get("frenchName");
			
			if (!StringUtils.isEmpty(englishName)) {
				v.setId(IdHelper.brandName(frenchName).toLowerCase());
				
				ProductI18nElements fr = new ProductI18nElements();
				fr.setVerticalHomeUrl(frenchName);
				// TODO(p1, features) : Complete with other datas
				
				v.getI18n().put("fr",fr );
			
			}
			
		} else {
			LOGGER.warn("Unsolved taxonomy for {} - {}", cat.getKey(), datas);
		}
		
		return v;
	}



	/**
	 * A prompt used to enrich the VerticalConfig
	 * @param v
	 * @return
	 */
	private Map<String, String> aiDatas(VerticalConfig v) {
		// Building the prompt
		
		String prompt = """
Strictly according to english google product taxonomy (https://www.google.com/basepages/producttype/taxonomy-with-ids.en-US.txt), 
give me the most precise english google product category for a list of categories found on market places.

Your response must be JSON format. The response will strictly follow this simple key/value format : 
- field "googleTaxonomy" : gives the most exact, precise and appropriate google taxonomy products category.
- field "englishName": An english name that describes this category
- field "frenchName": A french name that describes this category
  

Base your analyse on the following categories : 
		  		  
""";

		prompt+=StringUtils.join(v.getMatchingCategories(), "\n");
		
		// Calling the GPT model
		Map<String, String> response = aiService.jsonPrompt(prompt);
		
		
		return response;
	}
	
	

	/**
	 * Resolve a google taxonomy ID
	 * @param string
	 * @return
	 */
	private Integer resolveGoogleTaxonomy(String string) {
		Integer ret = googleTaxonomyService.resolve(string);
		LOGGER.info("Resolved google taxonomy for {} : {}",string,null); 
		return ret;
	}

	
	
}
