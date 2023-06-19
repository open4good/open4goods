
package org.open4goods.crawler.extractors;

import java.io.IOException;
import java.util.Locale;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.config.yml.datasource.RatingConfig;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.InStockParser;
import org.open4goods.helper.WarrantyParser;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Rating;
import org.slf4j.Logger;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 * Enable to extract DataFragment data from conf and Json
 *
 * @author goulven
 *
 */
public class JsonExtractor extends Extractor {


	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document, final DataSourceProperties conf, final Locale locale, final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {
		throw new RuntimeException("This call should not have happened");

	}


	public  void parse(final String json, final DataFragment p, final org.slf4j.Logger dedicatedLogger,
			final ExtractorConfig c, final Page page, final Locale locale, final DataSourceProperties provider,
			 final DataFragmentWebCrawler offerWebCrawler) throws IOException {

		final String url = page.getWebURL().getURL();
		JsonNode root;
		// try {
		root = serialisationService.getJsonMapper().readTree(json);


		if ("INFO".contentEquals(provider.getDedicatedLogLevel())) {
			dedicatedLogger.warn(
					"\n=============================\nJSONEXTRACTOR\n=============================\n{}\n=============================\n{}",
					url, serialisationService.toJson(root, true));
		}

		///////////////////////////////////////////////////////////////
		// Applying config json pointers operations to model
		///////////////////////////////////////////////////////////////

		
		
		////////////////////
		// Custom attributes
		////////////////////
		
		for (final Entry<String, String> attr : c.getAttributes().entrySet()) {
			throw new RuntimeException("ATTRIBUTES MAPPING NOT IMPLEMENTED IN JSON EXTRACTOR");
			
		}
		////////////////////
		// Review Rating
		////////////////////

		RatingConfig rrc = c.getExpertRating();

		if (null != rrc) {
			try {
				p.addExpertRating(evalRating(rrc, dedicatedLogger, root, url, provider, p));
			} catch (final ValidationException e) {
				dedicatedLogger.info("Error while validating expert rating  : {}  ; {}", e.getMessage(), url);
			}
		}

		rrc = c.getUserRating();

		if (null != rrc) {
			try {
				p.addUserRating(evalRating(rrc, dedicatedLogger, root, url, provider, p));
			} catch (final ValidationException e) {
				dedicatedLogger.info("Error while validating user rating: {}  ; {}", e.getMessage(), url);
			}
		}


		rrc = c.getRseRating();

		if (null != rrc) {
			try {
				p.addRseRating(evalRating(rrc, dedicatedLogger, root, url, provider, p));
			} catch (final ValidationException e) {
				dedicatedLogger.info("Error while validating rse rating: {}  ; {}", e.getMessage(), url);
			}
		}
		////////////////////
		// Name
		////////////////////
		if (!StringUtils.isEmpty(c.getName())) {
			p.addName(jsonEval(root, c.getName()));
		}

		////////////////////
		// Price
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getPrice())) {
				p.setPriceAndCurrency(jsonEval(root, c.getPrice()), locale);
			}
		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing price : {}  ; {}", e.getMessage(), url);
		}

		////////////////////
		// InStock
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getInStock())) {
				//TODO(design) : No parsing infos
				p.setInStock(InStockParser.parse(jsonEval(root, c.getInStock())));
			}
		} catch (final Exception e) {
			parserLogger.info("Error while parsing inStock : {}  ; {}", e.getMessage(), url);
		}

		////////////////////
		// Warranty
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getWarranty())) {
				p.setWarranty(WarrantyParser.parse(jsonEval(root, c.getWarranty())));
			}
		} catch (final Exception e) {
			parserLogger.info("Error while parsing warranty : {}  ; {}", e.getMessage(), url);
		}



		////////////////////
		// Currency
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getCurrency())) {
				p.setCurrency(jsonEval(root, c.getCurrency()));
			}

		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing currency : {}  ; {}", e.getMessage(), url);
		}

		////////////////////
		// Description
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getDescription())) {
				p.addDescription(jsonEval(root, c.getDescription()), page.getLanguage());
			}

		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing description : {}  ; {}", e.getMessage(), url);
		}

		////////////////////
		// Category
		////////////////////
		if (!StringUtils.isEmpty(c.getCategory())) {
			p.addProductTag(jsonEval(root, c.getCategory()));
			// TODO : log if fail everywhere
		}

		////////////////////
		// SubSeller
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getGtin13())) {
				
						p.addAttribute(ReferentielKey.GTIN.toString(), jsonEval(root, c.getGtin13()), page.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators());
			}

		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing gtin : {} ; {}", e.getMessage(), url);
		}

		////////////////////
		// subSeller
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getSubseller())) {
				
						p.addAttribute("SubSeller", jsonEval(root, c.getSubseller()), page.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators() );
			}

		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing subSeller : {} ; {}", e.getMessage(), url);
		}

		////////////////////
		// Brand
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getBrand())) {
				final String brand = jsonEval(root, c.getBrand());

					
							p.addAttribute(ReferentielKey.BRAND.toString(), brand, page.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators());
				if (!StringUtils.isEmpty(brand)) {
					//TODO(bug) : should set as referentiel from  all extractors, to avoid datafragmentcompletion to override "well set" referentiel attrs
					p.addReferentielAttribute(ReferentielKey.BRAND.toString(),brand);
				}

			}

		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing brand : {} ; {}", e.getMessage(), url);
		}

		////////////////////
		// Brand And Id
		////////////////////
		String eRes = null;
		try {
			if (!StringUtils.isEmpty(c.getBrandAndId())) {
				final String brandid = c.getBrandAndId();
				if (StringUtils.isEmpty(brandid)) {
					dedicatedLogger.info("Empty brandId : {}", url);
				} else {

					eRes = jsonEval(root, brandid);
					// Removing token if specified
					if (!StringUtils.isEmpty(c.getBrandAndIdRemoval())) {
						eRes = eRes.replace(c.getBrandAndIdRemoval(), "");
					}


					final String[] terms = eRes.split(c.getBrandAndIdSeparator());

					p.addAttribute(ReferentielKey.BRAND.toString(), terms[0], page.getLanguage(),true,null);
					p.addAttribute(ReferentielKey.MODEL.toString(), terms[1], page.getLanguage(),true,null);
				}
			}

		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing brandAndId. {} = {} : {} at {}",c.getBrandAndId(), eRes, e.getMessage(), url);

		}

		////////////////////
		// Resources
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getImage())) {
				p.addResource(jsonEval(root, c.getImage()));
			}

		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing image : {} ; {}", e.getMessage(), url);
		}

		////////////////////
		// VendorUid
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getBrandUid())) {
				
						p.addAttribute(ReferentielKey.MODEL.toString(), jsonEval(root, c.getBrandUid()), page.getLanguage(),true,null);
			}

		} catch (final Exception e) {
			dedicatedLogger.warn("Error while parsing vendorUid : {} ; {}", e.getMessage(), url);
		}

	}


	private static Rating evalRating(final RatingConfig rrc, final Logger dedicatedLogger, final JsonNode root,
                                     final String url, final DataSourceProperties provider, final DataFragment p) {

		final Rating gbr = new Rating();

		// Value
		String val = jsonEval(root, rrc.getValue());

		if (StringUtils.isEmpty(val)) {
			dedicatedLogger.info("No rating value at {} ; {}", rrc.getValue(), url);
			return null;
		}



		val = val.replace(",", ".");

		try {
			gbr.setValue(Double.valueOf(val.replace(",", ".")));
		} catch (final NumberFormatException e1) {
			dedicatedLogger.warn("Unable to associate rating value (not numeric) :  {} ; {}", val, url);
		}

		// Max
		if (null == rrc.getMax()) {
			if (null == provider.getRatingMax()) {
				dedicatedLogger.error("Max rating not defined in DataFragment, neither in conf. Will be set to 5 : {}",
						url);
				gbr.setMax(5.0);
			}
			gbr.setMax(provider.getRatingMax());

		} else {

			val = jsonEval(root, rrc.getMax());
			try {
				if (!StringUtils.isEmpty(val)) {
					gbr.setMax(Double.valueOf(val));
				}
			} catch (final NumberFormatException e) {
				dedicatedLogger.warn("Error while parsing GlobalReviewRating (ratingMax:{}) ; {}", val, url);
			}
		}

		// Min
		if (null == rrc.getMin()) {
			if (null == provider.getRatingMin()) {
				dedicatedLogger.error("Min rating not defined in DataFragment, neither in conf. Will be set to 1 : {}",
						url);
				gbr.setMin(1);
			}
			gbr.setMin(provider.getRatingMin());

		} else {
			if (StringUtils.isNumeric(rrc.getMin())) {
				val = rrc.getMin();
			} else {
				val = jsonEval(root, rrc.getMin());
			}
			try {
				if (!StringUtils.isEmpty(val)) {
					gbr.setMin(Integer.valueOf(val));
				}
			} catch (final NumberFormatException e) {
				dedicatedLogger.warn("Error while parsing GlobalReviewRating (ratingMin:{}) ; {}", val, url);
			}
		}

		// Min
		if (null != rrc.getVoters()) {
			if (StringUtils.isNumeric(rrc.getVoters())) {
				val = rrc.getVoters();
			} else {
				val = jsonEval(root, rrc.getVoters());
			}
			try {
				if (!StringUtils.isEmpty(val)) {
					gbr.setNumberOfVoters(Long.valueOf(val));
				}
			} catch (final NumberFormatException e) {
				dedicatedLogger.warn("Error while parsing GlobalReviewRating (ratingVoters:{}) ; {}", val, url);
			}
		}

		return gbr;

	}




}
