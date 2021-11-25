package org.open4goods.crawler.extractors;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.config.yml.datasource.RatingConfig;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.InStockParser;
import org.open4goods.helper.ProductStateParser;
import org.open4goods.helper.ShippingCostParser;
import org.open4goods.helper.ShippingTimeParser;
import org.open4goods.helper.WarrantyParser;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Rating;
import org.w3c.dom.Document;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 *
 */
public class XpathExtractor extends Extractor {


	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConfig, final Locale locale,
                      final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {

		final ExtractorConfig c = getExtractorConfig();

		///////////////////////////////////////////////////////////////
		// Applying xpath exp from config to model
		///////////////////////////////////////////////////////////////






		////////////////////
		// Category
		////////////////////
		if (null != c.getCategory()) {

			if (! c.getCategory().startsWith("/") && ! c.getCategory().startsWith(".") && ! c.getCategory().contains("::")) {
				// A fixed value
				p.addProductTag( c.getCategory());
				return  ;
			}



			List<String> cats = evalMultipleAndLogs(document, c.getCategory(), url);

			if (cats.size() > 0) {
				Integer from = c.getCategoryFrom();
				if (null == from) {
					from = 0;
				}
				Integer to = c.getCategoryTo();
				if (null == to || to > cats.size()) {
					to = cats.size();
				}

				cats = cats.subList(from, to);
			}
			p.addProductTags(cats);

			// Special rule. The product tag is mandatory, so to avoid useless xpath
			// processing, we break if it is nothing
			if (providerConfig.webDataSource().getEvictIfNoXpathCategory() &&   StringUtils.isEmpty(p.getCategory())) {
				getDedicatedLogger().info("product tags not found in xpathextractor, skipping remaining processing of {}", url);
				return;
			}
		}


		////////////////////
		// Warranty
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getWarranty())) {
				p.setWarranty(WarrantyParser.parse(evalAndLogs(document, c.getWarranty(), url)));
			}
		} catch (final Exception e) {
			parserLogger.warn("Error while parsing Warranty : {}  ; {}", e.getMessage(), url);
		}

		////////////////////
		// Shipping cost
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getShippingCost())) {
				p.setShippingCost(ShippingCostParser.parse(evalAndLogs(document, c.getShippingCost(), url)));
			}
		} catch (final Exception e) {
			parserLogger.warn("{}  ; {}", e.getMessage(), url);
		}



		////////////////////
		// Shipping time
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getShippingTime())) {
				p.setShippingTime(ShippingTimeParser.parse(evalAndLogs(document, c.getShippingTime(), url)));
			}
		} catch (final Exception e) {
			parserLogger.warn("Error while parsing ShippingTime : {}  ; {}", e.getMessage(), url);
		}




		////////////////////
		// Product state
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getProductState())) {
				p.setProductState(ProductStateParser.parse(evalAndLogs(document, c.getProductState(), url)));
			}
		} catch (final Exception e) {
			parserLogger.warn("Error while parsing ProductState : {}  ; {}", e.getMessage(), url);
		}




		////////////////////
		// InStock
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getInStock())) {
				p.setInStock(InStockParser.parse(evalAndLogs(document, c.getInStock(), url)));
			}
		} catch (final Exception e) {
			parserLogger.info("Error while parsing inStock : {}  ; {}", e.getMessage(), url);
		}


		////////////////////
		// Review Rating
		////////////////////

		RatingConfig rrc = c.getExpertRating();
		if (null != rrc) {
			try {
				p.addExpertRating(evalRating(rrc, document, url, providerConfig,  p));
			} catch (final ValidationException e) {
				getDedicatedLogger().warn("Error while validating expert rating : {} ; {}", e.getMessage(), url);

			}

		}

		rrc = c.getUserRating();

		if (null != rrc) {
			try {
				p.addUserRating(evalRating(rrc, document, url, providerConfig,  p));
			} catch (final ValidationException e) {
				getDedicatedLogger().warn("Error while validating user rating : {} ; {}", e.getMessage(), url);

			}

		}


		rrc = c.getRseRating();

		if (null != rrc) {
			try {
				p.addRseRating(evalRating(rrc, document, url, providerConfig,  p));
			} catch (final ValidationException e) {
				getDedicatedLogger().warn("Error while validating rse rating : {} ; {}", e.getMessage(), url);

			}

		}
		////////////////////
		// Name
		////////////////////
		if (null != c.getName()) {
			//TODO(design) : space normalisation should apply on other "single line string" attributes
			p.addName(StringUtils.normalizeSpace(evalAndLogs(document, c.getName(), url) ));
		}

		////////////////////
		// Brand And Id
		////////////////////
		try {
			if (!StringUtils.isEmpty(c.getBrandAndId())) {
				final String brandid = c.getBrandAndId();


				if (StringUtils.isEmpty(brandid)) {
					getDedicatedLogger().info("Empty brandId : {}", url);
				} else {

					String eRes = evalAndLogs(document, c.getBrandAndId(), url);

					// Removing token if specified
					if (!StringUtils.isEmpty(c.getBrandAndIdRemoval())) {
						eRes = eRes.replace(c.getBrandAndIdRemoval(), "");
					}

					final String[] terms = eRes
							.split(c.getBrandAndIdSeparator());

					p.addAttribute(ReferentielKey.BRAND.toString(), terms[0], page.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators());
					p.addAttribute(ReferentielKey.MODEL.toString(), eRes.replace(terms[0], "") , page.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators());
				}
			}

		} catch (final Exception e) {
			getDedicatedLogger().warn("Error while parsing brandAndId : {} ; {}", e.getMessage(), url);
		}

		////////////////////
		// Price
		////////////////////

		try {
			if (null != c.getPrice()) {
				p.setPriceAndCurrency(evalAndLogs(document, c.getPrice(), url), locale);
			}
		} catch (final ParseException e) {
			getDedicatedLogger().warn("priceAndCurrency parsing error ; {} ; {}", e.getMessage(), url);
		}

		////////////////////
		// Currency
		////////////////////
		if (null != c.getCurrency()) {
			try {

				p.setCurrency(evalAndLogs(document, c.getCurrency(), url));
			} catch (final ParseException e) {
				getDedicatedLogger().warn("currency parsing error ; {} ; {}", e.getMessage(), url);
			}
		}

		String xpath = c.getPros();

		if (!StringUtils.isEmpty(xpath)) {

			final List<String> ress = evalMultipleAndLogs(document, xpath, url);
			for (final String r : ress) {
				try {
					p.addPro(r, locale.getLanguage());
				} catch (final ValidationException e) {
					getDedicatedLogger().warn("Error while adding pro", e);
				}
			}
		}
		////////////////////
		// Cons
		////////////////////
		xpath = c.getCons();

		if (!StringUtils.isEmpty(xpath)) {

			final List<String> ress = evalMultipleAndLogs(document, xpath, url);
			for (final String r : ress) {
				try {
					p.addCon(r, locale.getLanguage());
				} catch (final ValidationException e) {
					getDedicatedLogger().warn("Error while adding con", e);
				}
			}
		}

		////////////////////
		// Description
		////////////////////

		if (null != c.getDescription()) {
			p.addDescription(evalAndLogs(document, c.getDescription(), url), page.getLanguage());
		}
		if (!StringUtils.isEmpty(c.getGtin13())) {
			
					p.addAttribute(ReferentielKey.GTIN.toString(), evalAndLogs(document, c.getGtin13(), url), page.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators());
		}
		////////////////////
		// subSeller
		////////////////////

		if (null != c.getSubseller()) {
			try {
				p.addSubSeller(evalAndLogs(document, c.getSubseller(), url));
			} catch (final ParseException e) {
				getDedicatedLogger().warn("subSeller parsing error ; {} ; {}", e.getMessage(), url);
			}
		}

		if (!StringUtils.isEmpty(c.getBrand())) {
			
					p.addAttribute(ReferentielKey.BRAND.toString(), evalAndLogs(document, c.getBrand(), url), page.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators());
		}
		////////////////////
		// Image
		////////////////////

		if (!StringUtils.isEmpty(c.getImage())) {
			try {
				final List<String> imgNodes = evalMultipleAndLogs(document, c.getImage(), url);
				for (final String imgSrc : imgNodes) {
					p.addResource(imgSrc);
				}
			} catch (final ValidationException e) {
				getDedicatedLogger().warn("Image validation error ; {} ; {}", e.getMessage(), url);
			}
		}

		////////////////////
		// Resources
		////////////////////

		try {
			for (final String xpathR : c.getResources()) {
				final List<String> ress = evalMultipleAndLogs(document, xpathR, url);
				for (final String r : ress) {

					String target = r;

					// Handling resource name replacements
					for (final Entry<String,String> repl : c.getResourceReplacements().entrySet()) {
						target = r.replace(repl.getKey(), repl.getValue());
					}


					p.addResource(target);
				}
			}
		} catch (final ValidationException e) {
			getDedicatedLogger().warn("Resources validation error ; {} ; {}", e.getMessage(), url);
		}

		////////////////////
		// vendorUid
		////////////////////

		if (!StringUtils.isEmpty(c.getBrandUid())) {
			p.addAttribute(ReferentielKey.MODEL.toString(),
					evalAndLogs(document, c.getBrandUid(), url), page.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators());
		}
	}

	public Rating evalRating(final RatingConfig rrc, final Document document, final String url,
                             final DataSourceProperties providerConfig, final DataFragment p) {

		final Rating gbr = new Rating();

		// Value
		String val = evalAndLogs(document, rrc.getValue(), url);
		try {
			if (!StringUtils.isEmpty(val)) {
				gbr.setValue(Double.valueOf(val.replace(",", ".")));
			}
		} catch (final NumberFormatException e) {
			getDedicatedLogger().warn("Error while parsing GlobalReviewRating (ratingValue:{}) ; {}", val, url);
		}

		// Max
		if (null == rrc.getMax()) {
			if (null == providerConfig.getRatingMax()) {
				getDedicatedLogger()
						.error("Max rating not defined in DataFragment, neither in conf. Will be set to 5 : {}", url);
				gbr.setMax(5.0);
			}
			gbr.setMax(providerConfig.getRatingMax());

		} else {

			if (StringUtils.isNumeric(rrc.getMax())) {
				val = rrc.getMax();
			} else {
				val = evalAndLogs(document, rrc.getMax(), url);
			}
			try {
				if (!StringUtils.isEmpty(val)) {
					gbr.setMax(Double.valueOf(val));
				}
			} catch (final NumberFormatException e) {
				getDedicatedLogger().warn("Error while parsing GlobalReviewRating (ratingMax:{}) ; {}", val, url);
			}
		}

		// Min
		if (null == rrc.getMin()) {
			if (null == providerConfig.getRatingMin()) {
				getDedicatedLogger()
						.error("Min rating not defined in DataFragment, neither in conf. Will be set to 1 : {}", url);
				gbr.setMin(1);
			}
			gbr.setMin(providerConfig.getRatingMin());

		} else {

			if (StringUtils.isNumeric(rrc.getMin())) {
				val = rrc.getMin();
			} else {
				val = evalAndLogs(document, rrc.getMin(), url);
			}
			try {
				if (!StringUtils.isEmpty(val)) {
					gbr.setMin(Integer.valueOf(val));
				}
			} catch (final NumberFormatException e) {
				getDedicatedLogger().warn("Error while parsing GlobalReviewRating (ratingMin:{}) ; {}", val, url);
			}
		}

		// Number of voters
		if (null != rrc.getVoters()) {
			if (StringUtils.isNumeric(rrc.getVoters())) {
				val = rrc.getVoters();
			} else {
				val = evalAndLogs(document, rrc.getVoters(), url);
			}
			try {
				if (!StringUtils.isEmpty(val)) {
					gbr.setNumberOfVoters(Long.valueOf(val));
				}
			} catch (final NumberFormatException e) {
				getDedicatedLogger().warn("Error while parsing GlobalReviewRating (ratingVoters:{}) ; {}", val, url);
			}
		}

		return gbr;

	}

}
