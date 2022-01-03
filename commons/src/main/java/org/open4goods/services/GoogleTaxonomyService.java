package org.open4goods.services;

import java.io.File;
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

	Map<String, Integer> lastCategoriesId = new HashMap<>();

	Map<String, Map<Integer, List<String>>> localizedTaxonomy = new HashMap<>();

	
	
	/**
	 * Load a localized taxonomy file
	 * 
	 * @param filePath
	 * @param language
	 * @throws IOException
	 */
	public void loadFile(String fileClassPath, String language) throws IOException {

		localizedTaxonomy.put(language, new HashMap<>());

		final ObjectReader oReader = csvMapper.readerForListOf(String.class).with(CsvParser.Feature.WRAP_AS_ARRAY)
				.with(schema);

		URL data = getClass().getResource(fileClassPath);
		
		final MappingIterator<List<String>> mi = oReader.readValues(data);

		while (mi.hasNext()) {

			List<String> line = mi.next();

			Integer id = Integer.valueOf(line.get(0));
			for (int i = line.size() - 1; i >= 0; i--) {
				String val = line.get(i);

				if (!StringUtils.isEmpty(val)) {

					logger.info("Adding terminal category : {}",val);

					Integer knownId = lastCategoriesId.get(val);
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

						lastCategoriesId.put(val, id);
						localizedTaxonomy.get(language).put(id, cats);
					}

					break;
				}
			}

		}

	}


}