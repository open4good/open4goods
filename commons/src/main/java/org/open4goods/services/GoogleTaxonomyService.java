package org.open4goods.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

/**
 * This service is in charge to google taxonomy id, from a product category
 * 
 * @author goulven
 *
 * 
 */
public class GoogleTaxonomyService {

	protected static final Logger logger = LoggerFactory.getLogger(GoogleTaxonomyService.class);

	/**
	 * A mat associating last categories path with taxonomy id
	 */
	Map<String, Integer> lastCategoriesId = new HashMap<>();

	/**
	 * A mat associating last categories path with taxonomy id
	 */
	Map<String, Integer> fullCategoriesId = new HashMap<>();

	/**
	 * The full taxonomy indexed by language
	 */

	Map<String, Map<Integer, List<String>>> localizedTaxonomy = new HashMap<>();

	private RemoteFileCachingService fileCachingService;

	public GoogleTaxonomyService(RemoteFileCachingService fileCachingService) {
		super();
		this.fileCachingService = fileCachingService;
	}

	/**
	 * Load a localized taxonomy file
	 * 
	 * @param url
	 * @param language
	 * @throws IOException
	 * @throws InvalidParameterException
	 */
	public void loadGoogleTaxonUrl(String url, String language) throws IOException, InvalidParameterException {

		localizedTaxonomy.put(language, new HashMap<>());

		File taxonFile = fileCachingService.getResource(url);

		List<String> lines = Files.readAllLines(taxonFile.toPath());

		for (String line : lines) {

			// Ignorer les commentaires
			if (line.startsWith("#")) {
				continue;
			}

			int pos = line.indexOf("-");

			// Retrieving the id
			Integer id = Integer.valueOf(line.substring(0, pos - 1));

			// Adding in the full category id
			fullCategoriesId.put(IdHelper.azCharAndDigits(line.substring(pos + 2)).toLowerCase(), id);

			// The number
			List<String> fragments = Arrays.asList(line.substring(pos+1).split(">")).stream().map(e -> e.trim()).toList();

//           // Utilisation d'une variable pour stocker la catégorie trouvée
			String foundCategory = null;

			// Recherche de la dernière catégorie non vide
			for (int i = fragments.size() - 1; i >= 0; i--) {
				String val = fragments.get(i);

				if (!StringUtils.isEmpty(val)) {
					foundCategory = val;
					break;
				}
			}

			// Traitement de la catégorie trouvée
			if (foundCategory != null) {
				String fcc = IdHelper.azCharAndDigits(foundCategory).toLowerCase();
				if (lastCategoriesId.containsKey(fcc)) {
					// TODO : logger.error
//					System.err.println("Category exists : " + foundCategory);
				} else {
					List<String> cats = new ArrayList<>();
//					cats.add(foundCategory);
					// Utilisation d'une boucle améliorée pour la récupération des catégories
					for (int j = 0; j < fragments.size(); j++) {
						String catVal = fragments.get(j);
						if (StringUtils.isEmpty(catVal)) {
							break;
						} else {
							cats.add(catVal);
						}
					}

					lastCategoriesId.put(fcc, id);
					localizedTaxonomy.get(language).put(id, cats);
				}
			}
		}

	}

	/**
	 * Resolve a category to a taxonomy id
	 * @param category
	 * @return
	 */
	public Integer resolve (String category) {
		
		if (StringUtils.isEmpty(category)) {
			return null;
		}
		
		String token = IdHelper.azCharAndDigits( category).toLowerCase();
		
		// First resolving with full path
		Integer ret = fullCategoriesId.get(token);

		if (null == ret ) {
			// Fail, resolving with last path id
			ret = lastCategoriesId.get(token);
		}
		
		return ret;
		
	}

	
	/**
	 * Resolve the deepest category if from several one
	 * @param taxonomyIds
	 * @return
	 */
	public int selectDeepest( String language, List<Integer> taxonomyIds) {
		
		int deepest = -1;
		int deepestSize = -1;
		
		
		for (int i = 0; i < taxonomyIds.size(); i++) {
			
			int size = localizedTaxonomy.get(language).get(taxonomyIds.get(i)).size();
			if (  size  > deepestSize) {
				deepest = taxonomyIds.get(i);
				deepestSize = size;
			}
		}
		
		return deepest;
				
	}
	
	/**
	 * Resolve the deepest category if from several one
	 * @param language
	 * @param taxonomyIds
	 * @return
	 */
	public int selectDeepest(String language, Integer... taxonomyIds) {
		return selectDeepest(language, Arrays.asList(taxonomyIds));		
	}
	
	
	public Map<String, Integer> getLastCategoriesId() {
		return lastCategoriesId;
	}

	public void setLastCategoriesId(Map<String, Integer> lastCategoriesId) {
		this.lastCategoriesId = lastCategoriesId;
	}

	public Map<String, Map<Integer, List<String>>> getLocalizedTaxonomy() {
		return localizedTaxonomy;
	}

	public void setLocalizedTaxonomy(Map<String, Map<Integer, List<String>>> localizedTaxonomy) {
		this.localizedTaxonomy = localizedTaxonomy;
	}

	public Map<String, Integer> getFullCategoriesId() {
		return fullCategoriesId;
	}

	public void setFullCategoriesId(Map<String, Integer> fullCategoriesId) {
		this.fullCategoriesId = fullCategoriesId;
	}

}
