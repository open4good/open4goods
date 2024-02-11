
package org.open4goods.api.controller.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.model.EnrichmentFacet;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.BarcodeType;
import org.open4goods.model.constants.ProductCondition;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.data.Description;
import org.open4goods.model.product.AggregatedPrice;
import org.open4goods.model.product.Product;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This controller allows batch GTIN enrichment, for a uploaded CSV file. it will
 * re-expose the provided CSV "on the fly" with requested open4goods datas, meanings virtualy no size limit on the input.
 * 
 * TODO : Document list of available facets 
 * TODO : Dedicated service 
 * TODO : Could upgrade perf with micro batches (using multiGets on repos), and paralleilization 
 * TODO : Specific role
 * 
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_TESTER + "')")
public class CsvEnrichmentController {

	private static final String O4G_GTIN_TYPE = "o4g-gtin-type";
	private static final String O4G_ERROR = "o4g-error";
	Logger logger = LoggerFactory.getLogger(CsvEnrichmentController.class);
	
	private final static ObjectMapper csvMapper = new CsvMapper().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

	@Autowired private ProductRepository repository;
	@Autowired private BarcodeValidationService barcodeService;
	@Autowired private SerialisationService serialisationService;
	
	@PostMapping(path = "/enrich/", consumes= MediaType.MULTIPART_FORM_DATA_VALUE, produces="text/csv")
	@Operation(summary = "Enrich a CSV file with open4goods data")
	
	
	@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_TESTER+ "')")
	public void enrich(
			@Parameter(description = "Input CSV File to be enriched") @RequestParam("file") MultipartFile inputFile,
			@Parameter(description = "Facets that will be rendered") @RequestParam(required = false)  List<EnrichmentFacet> facets,
			@Parameter(description = "Name of the csv header that points the GTIN") @RequestParam(required = false) String gtinField, 
			@Parameter(description = "Name of the csv header that points the title. A gtin resolution will be prefered if availlable")@RequestParam(required = false) String titleField, 
			@Parameter(description = "CSV Schema is autodetected, but you can force here the csv quote char") @RequestParam(defaultValue = "\"") String csvQuoteChar,
			@Parameter(description = "CSV Schema is autodetected, but you can force here the csv column separator") @RequestParam(defaultValue = ",") String csvColumnSeparatorChar, 
			@Parameter(description = "CSV Schema is autodetected, but you can force here the csv escape char") @RequestParam(required = false) String csvEscapeChar,
			@Parameter(description = "Input CSV file Encoding") @RequestParam(required = false, defaultValue = "UTF-8") String inputEncoding,
			HttpServletResponse response

	) throws IOException  {

		logger.info("CSV enrichment request : " + inputFile.getOriginalFilename() + " with " + facets.size() + " facets");
		
		// Checking required parameters
		if (StringUtils.isEmpty(gtinField) && StringUtils.isEmpty(titleField)) {
			response.setStatus(400);
			return;
		}		
		
		// Retrieving original file informations
		String filaName = inputFile.getOriginalFilename();
		String contentType = inputFile.getContentType();
		long size = inputFile.getSize();

		// Getting user charset or default to UTF-8
		Charset charset = Charset.forName(inputEncoding, Charset.forName("UTF-8"));

		CsvSchema inSchema = CsvSchema.emptySchema().withHeader();
		
		// TODO : Fork the stream to allow schema detection.
//		CsvSchema inSchema  = csvService.detectSchema(destFile);		

		///////////////////////////////////////////////////////
		// Forcing CSV schema with user provided informations
		///////////////////////////////////////////////////////
		if (null != csvQuoteChar) {
			inSchema = inSchema.withQuoteChar(csvQuoteChar.charAt(0));
		}
		if (null != csvColumnSeparatorChar) {
			inSchema = inSchema.withColumnSeparator(csvColumnSeparatorChar.charAt(0));
		}
		if (null != csvEscapeChar) {
			inSchema = inSchema.withEscapeChar(csvEscapeChar.charAt(0));
		}
		
		////////////////////////////////////
		// CSV output file initialisation
		////////////////////////////////////
		
		Builder sb = CsvSchema.builder();

		// Setting outSchema iso to inSchema
		sb.setQuoteChar((char) inSchema.getQuoteChar());
		sb.setColumnSeparator(inSchema.getColumnSeparator());
		sb.setEscapeChar((char) inSchema.getEscapeChar());
		sb.setLineSeparator('\n');
		sb.setUseHeader(true);
		CsvSchema outSchema;		
		ObjectWriter csvOut = null;
		
		
		try {

			// CSV reader initialization
		
			final ObjectReader oReader = csvMapper.readerFor(Map.class).with(inSchema);
			MappingIterator<Map<String, String>> mi = null;

			Reader r = new InputStreamReader(inputFile.getInputStream(), charset);
			mi = oReader.readValues(r);
			
			Map<String, String> line = null;

			// Setting the response headers
			response.setContentType("text/csv");
			response.setHeader("Content-Disposition", "attachment; filename=\"o4g-enriched-" + filaName + "\"");

			boolean first = true;
			
			// Write the data back
			SequenceWriter writer = null;
			while (mi.hasNext()) {
				///////////////////////////////////////////////////////
				// On each line
				///////////////////////////////////////////////////////
				line = mi.next();

				logger.info("Processing line : " + line);
				
				// First line, init the output schema
				if (first) {
					// Adding the new columns
					line.keySet().forEach((c) -> {
						sb.addColumn(c);
					});
					
					for (EnrichmentFacet facet : facets) {
			            sb.addColumn("o4g-facet-"+facet.name().toLowerCase());
			        }
					sb.addColumn(O4G_ERROR);
					sb.addColumn(O4G_GTIN_TYPE);
					
					outSchema = sb.build();
 					csvOut = csvMapper.writer(outSchema);
 					 // Write the header
//					csvOut.writeHeader(response.getOutputStream());
 					
 					writer = csvOut.writeValues(response.getOutputStream());
					first = false;
				}								

				// Set the data that will be returned, add all initial datas
				Map<String, String> enriched = new HashMap<String, String>();
				enriched.putAll(line);

				String gtin = line.get(gtinField);
				String title = line.get(titleField);

				// Sanitisation and GTIN information
				if (null == gtin) {
					enriched.put(O4G_ERROR, "NO_GTIN_PROVIDED");
				} else {
					SimpleEntry<BarcodeType, String> sanitizedGtin = barcodeService.sanitize(gtin);
					enriched.put(O4G_GTIN_TYPE, sanitizedGtin.getValue());
					gtin = sanitizedGtin.getValue();

					// Getting the product
					Product p = null;
					try {
						p = repository.getById(gtin);
					} catch (ResourceNotFoundException e) {
						logger.info("Product not found in database : " + gtin);
						enriched.put(O4G_ERROR, "NOT_FOUND_IN_DATABASE");
					} catch (Exception e) {
						logger.error("Error while querying product", e);
						enriched.put(O4G_ERROR, "ERROR-QUERYING-PRODUCT");
					}
					
					if (null != p) {
						// To data enrichment
						enrich(p, enriched, facets);
					}
				}
					
				writer.write(enriched);

			}

			
		} catch (Exception e) {
			logger.error("Error while reading the provided CSV file",e);
//			response.getWriter().write("Error while reading the provided CSV file : " + e.getMessage());
			response.setStatus(500);
		}
		
		logger.info("CSV enrichment terminated : " + inputFile.getOriginalFilename());
	}

	/**
	 * Proceed to the data enrichment
	 * TODO : Externalize, mutualize with the unitary API endpiints
	 * TODO : Implement the cost model
	 * @param p
	 * @param enriched
	 * @param facets
	 */
	private void enrich(Product p, Map<String, String> enriched, List<EnrichmentFacet> facets) {

		for (EnrichmentFacet facet : facets) {

			try {
				if (null == facet) {
					continue;
				}

				String key = "o4g-facet-" + facet.name().toLowerCase();

				Optional<Description> desc;
				Optional<AggregatedPrice> Price pr;
				switch (facet) {
			
				case EnrichmentFacet.BRAND:
					enriched.put(key, p.brand());
					break;

					
					
				case EnrichmentFacet.MODEL:
					enriched.put(key, p.model());
					break;

				case EnrichmentFacet.TITLE:
					// TODO : Review / customize the title strategy
					enriched.put(key, p.bestName());
					break;
//				case EnrichmentFacet.DESCRIPTION:
//					// TODO : I18n
//					Description d = p.getDescriptions().stream().findFirst().orElse(null);
//					enriched.put(key, d== null ? "" :  d.getContent().getText());
//					break;
				case EnrichmentFacet.RAW_ATTRIBUTES:
					// TODO : I18n

					Map<String, String> raw = p.getAttributes().getUnmapedAttributes().stream().collect(HashMap::new, (m, a) -> m.put(a.getName(), a.getValue()), HashMap::putAll);
					enriched.put(key, serialisationService.toJson(raw));
					break;
					
					
//				case EnrichmentFacet.FEATURES:
//					// TODO : I18n
//
//					Set<String> feat = p.getAttributes().getFeatures().stream().collect(HashSet::new,
//							(s, a) -> s.add(a.getName()), HashSet::addAll);
//					enriched.put(key, serialisationService.toJson(feat));
//					break;

				case EnrichmentFacet.CLASSIFIED_ATTRIBUTES:
					// TODO : I18n

					Map<String, String> attrs = p.getAttributes().getAggregatedAttributes().entrySet().stream()
							.collect(HashMap::new, (m, a) -> m.put(a.getKey(), a.getValue().getValue()), HashMap::putAll);
					enriched.put(key, serialisationService.toJson(attrs));
					break;

					
//					+  
//					// TODO :  id's,

				case EnrichmentFacet.GTIN_INFOS:				
					enriched.put(key, serialisationService.toJson(p.getGtinInfos() ));
					break;
					
				case EnrichmentFacet.DATES:
					Map<String,String> dates = new HashMap<>();
					pricesHistory.put("created", p.getCreationDate());
					pricesHistory.put("updated", p.getLastChange());					
					enriched.put(key, serialisationService.toJson(dates ));
					break;
					
				case EnrichmentFacet.GOOGLE_TAXONOMY:
                    enriched.put(key, p.getGoogleTaxonomy());
                    break;
				
				case EnrichmentFacet.PRICE_HISTORY:
					Map<String,String> pricesHistory = new HashMap<>();
					pricesHistory.put("new", serialisationService.toJson(p.getPrice().getHistory(ProductCondition.NEW) ) );
					pricesHistory.put("occasion", serialisationService.toJson(p.getPrice().getHistory(ProductCondition.OCCASION) ) );
					
					enriched.put(key, serialisationService.toJson(pricesHistory ));

					break;
				case EnrichmentFacet.PRICES:
					Map<String,String> prices = new HashMap<>();
					prices.put("offers",p.getOffersCount()+"");;
					prices.put("bestPriceNew", p.getPrice().getMinPrice(ProductCondition.NEW) );
					prices.put("bestPriceOccasion", p.getPrice().getMinPrice(ProductCondition.OCCASION) );

					enriched.put(key, serialisationService.toJson(prices));
					break;
					
				case EnrichmentFacet.IMAGES:
					enriched.put(key, serialisationService.toJson(p.getResources().stream().map(r -> r.getUrl())) );
					break;
					
				default:
					logger.error("Unknown facet : " + facet);
					break;

				}
			} catch (Exception e) {
				logger.error("Error while enriching the product", e);
			}

		}
	}
}


