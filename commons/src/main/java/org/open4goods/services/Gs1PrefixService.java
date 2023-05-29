package org.open4goods.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.ibm.icu.util.ULocale;

/**
 * This service renders the GS1 country associated for a bar code
 *
 * @author Goulven.Furet
 *
 */
public class Gs1PrefixService {

	protected static final Logger logger = LoggerFactory.getLogger(Gs1PrefixService.class);

	/**
	 * The GS1 prefixes, loaded from file in the format
	 * https://www.gs1.org/standards/id-keys/company-prefix
	 */
	private Map<String, String> prefixes = new HashMap<>();

	/**
	 * Schema for gs1 prefix
	 *
	 * @author Goulven.Furet
	 *
	 */

	/**
	 * The constructor initlaize the prefix resolution map from the csv, provided as
	 * resource
	 *
	 * @param csvUri
	 * @param resourceResolver
	 * @throws IOException
	 */
	public Gs1PrefixService(String csvUri, ResourcePatternResolver resourceResolver) throws IOException {

		Map<String, String> countryNames = new HashMap<>();

		for (String loc : ULocale.getISOCountries()) {
			String country = new ULocale("", loc).getDisplayCountry(ULocale.US);
			countryNames.put(country.toLowerCase(), loc);
			logger.info("Adding locale country : {} > {}", country, loc);
		}

		logger.info("Loading gs1 prefix from {}", csvUri);
		// Getting the resource

		try {
			CsvMapper mapper = new CsvMapper().enable(CsvParser.Feature.SKIP_EMPTY_LINES).enable(CsvParser.Feature.TRIM_SPACES);

			CsvSchema schema = CsvSchema.emptySchema().withHeader();

			MappingIterator<Gs1PrefixSchema> iterator = mapper.readerFor(Gs1PrefixSchema.class).with(schema)

					.readValues(resourceResolver.getResource(csvUri)
							.getInputStream());

			iterator.readAll().forEach(e -> {
				prefixes.putAll(e.expand(countryNames));
			});

			iterator.close();

		} catch (IOException e) {
			logger.error("Cannot initialize gs1prefix service", e);
		}

	}

	/**
	 * Return the country for a given Gtin
	 *
	 * @param gtin
	 * @return
	 */
	public String detectCountry(String gtin) {

		if (StringUtils.isEmpty(gtin)) {
			return null;
		}

		String prefix = gtin.substring(0, 3);
		return prefixes.get(prefix);
	}

}
