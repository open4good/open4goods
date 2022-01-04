package org.open4goods.services;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * This service is in charge of  google taxonomy classification, from a product category
 * 
 * @author goulven
 *
 *
 */
public class GoogleTaxonomyService {

	protected static final Logger logger = LoggerFactory.getLogger(GoogleTaxonomyService.class);

	private final ObjectMapper csvMapper = new CsvMapper().enable((CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE));

	private CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(',')
// .withEscapeChar(SANITIZED_ESCAPE_CHAR)
			.withQuoteChar('"');

	Map<String,Map<String, Integer>> lastCategoriesId = new HashMap<>();

	Map<String, Map<Integer, List<String>>> localizedTaxonomy = new HashMap<>();

	Map<Integer,List<Integer>> parentsCategories = new HashMap<>();  
	
	Map<String,Map<Integer,String>> categoryNames = new HashMap<>();
	
	
	/**
	 * Load a localized taxonomy file
	 * 
	 * @param filePath
	 * @param language
	 * @throws IOException
	 */
	public void loadFile(String fileClassPath, String language) throws IOException {

		// Init per language dictionaries
		localizedTaxonomy.put(language, new HashMap<>());
		lastCategoriesId.put(language, new HashMap<>());
		categoryNames.put(language, new HashMap<>());
		
		
		// Reading CSV
		final ObjectReader oReader = csvMapper.readerForListOf(String.class)
				.with(CsvParser.Feature.WRAP_AS_ARRAY)
				.with(schema);

		URL data = getClass().getResource(fileClassPath);		
		final MappingIterator<List<String>> mi = oReader.readValues(data);

		while (mi.hasNext()) {

			List<String> line = mi.next();

			Integer id = Integer.valueOf(line.get(0));
			// For each cell
			for (int i = line.size() - 1; i >= 0; i--) {
				String val = line.get(i);

				if (!StringUtils.isEmpty(val)) {

					logger.info("Adding terminal category : {}",val);

					Integer knownId = lastCategoriesId.get(language).get(val);
					if (null != knownId && !knownId.equals(id)) {
						logger.error("Last category {} already exists with id {}. Actual id is {}", val, knownId,id);
					} else {
						List<String> cats = new ArrayList<>();

						for (int j = 1; j < line.size(); j++) {
							String catVal = line.get(j);
							if (StringUtils.isEmpty(catVal)) {
								break;
							} else {
								cats.add(catVal);
							}
						}
						
						// Filing dictionaries
						lastCategoriesId.get(language).put(val, id);
						localizedTaxonomy.get(language).put(id, cats);
						categoryNames.get(language).put(id, val);
					}

					break;
				}
			}

		}
		
		// Filling parent categories id
		for (Integer val : lastCategoriesId.get(language).values()) {
			
			
			List<Integer> pCat = new ArrayList<>(); 
			for (String cat :  localizedTaxonomy.get(language).get(val)) {
				pCat.add(getTaxonomyId(language, cat));
			}
			
			List<Integer> oldPcap = parentsCategories.get(val);
			
			if (null == oldPcap) {
				parentsCategories.put(val, pCat);
			} else if (!oldPcap.equals(pCat)) {
				logger.error("Skiping erasing parent categories for {} : from {} to {}",val,oldPcap,pCat);
			}
			
		}
		
		

	}


	/**
	 * Return a category name for the id and the language
	 * @param language
	 * @param id
	 * @return
	 */
	public String getCategory(String language, Integer id) {
		Map<Integer, String> cat = categoryNames.get(language);
		
		if (null == cat) {
			logger.error("Language {} does not exists", language);
			return null;
		}
		return cat.get(id);
	}
	
	/**
	 * Return the parent categories id for a given id
	 * @param language
	 * @param id
	 * @return
	 */
	public List<Integer> getParentsCategoriesId(Integer id) {
			return parentsCategories.get(id);
	}
	
	
	/**
	 * Return the parent categories for a given id
	 * @param language
	 * @param id
	 * @return
	 */
	public List<String> getParentsCategories(String language, Integer id) {
		try {
			return localizedTaxonomy.get(language).get(id);
		} catch (Exception e) {
			logger.error("Language {} or category {} does not exists",language, id);
			return List.of();
		}
	}
	

	/**
	 * Return the taxonomy id for a given category and language
	 * @param language
	 * @param category
	 * @return
	 */
	public Integer getTaxonomyId(String language, String category) {
		
		Map<String, Integer> cat = lastCategoriesId.get(language);
		
		if (null == cat) {
			logger.error("Language {} does not exists", language);
			return null;
		}
		return cat.get(category);
	}
	
	/////////////////////////////////////
	// Getters and setters
	/////////////////////////////////////
	
	
	public Map<String, Map<String, Integer>> getLastCategoriesId() {
		return lastCategoriesId;
	}



	public void setLastCategoriesId(Map<String, Map<String, Integer>> lastCategoriesId) {
		this.lastCategoriesId = lastCategoriesId;
	}



	public Map<String, Map<Integer, List<String>>> getLocalizedTaxonomy() {
		return localizedTaxonomy;
	}



	public void setLocalizedTaxonomy(Map<String, Map<Integer, List<String>>> localizedTaxonomy) {
		this.localizedTaxonomy = localizedTaxonomy;
	}

	
	
	
	

}