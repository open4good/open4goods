package org.open4goods.crawler.extractors;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.config.yml.datasource.ExtractorConfig;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.data.Rating;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 *
 * @author goulven
 * TODO(design) : rename to ratingXpathExtractor ?
 */
public class ReviewXpathExtractor extends Extractor {

	private final static Logger logger = LoggerFactory.getLogger(ReviewXpathExtractor.class);

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConf, final Locale locale, final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {

		final ExtractorConfig c = getExtractorConfig();

		if (!StringUtils.isEmpty(c.getReviewXpathValue())) {
			///////////////////////////////////////////////////////////////////////////////////////
			// In this mode, we iterate
			// over the table, and take the tr/td[0], tr/td[0], and inject
			/////////////////////////////////////////////////////////////////////////////////////// the
			/////////////////////////////////////////////////////////////////////////////////////// th
			/////////////////////////////////////////////////////////////////////////////////////// as
			/////////////////////////////////////////////////////////////////////////////////////// context
			///////////////////////////////////////////////////////////////////////////////////////
			try {
				final NodeList labels = xpathMultipleEval(document, c.getReviewXpathLabel());
				final NodeList values = xpathMultipleEval(document, c.getReviewXpathValue());


				final Integer min = c.getReviewXpathMinValue();
				final Double max = c.getReviewXpathMaxValue();

				if (labels.getLength() == 0 || labels.getLength() != values.getLength()) {
					getDedicatedLogger().warn("Not the same number of labels and values  {} ; {} ; {} ",
							labels.getLength(), values.getLength(), url);
					return;
				}

				for (int i = 0; i < labels.getLength(); i++) {
					try {
						final Rating rr = new Rating();
						rr.setMin(min);
						rr.setMax(max);

						labels.item(i).getTextContent();
						final String value = values.item(i).getTextContent();

//						rr.setLabel(label);

						// Checking for (5/5)
						if (value.contains("/")) {
							final String[] terms = value.split("/");
							try {
								rr.setValue(Double.valueOf(terms[0]));
								rr.setMax(Double.valueOf(terms[1]));
							} catch (final Exception e) {
								getDedicatedLogger().error("Cannot convert to rating value (with /) :  {} ; {} ; {} ",
										terms[0], e.getMessage(), url);
							}
						} else {
							try {
								rr.setValue(Double.valueOf(value));
							} catch (final Exception e) {
								getDedicatedLogger().error("Cannot convert to rating value (with /) :  {} ; {} ; {} ",
										value, e.getMessage(), url);
							}
						}

						// Adding the rating
						rr.addTag(c.getReviewXpathType());
						p.addRating(rr);
					} catch (final ValidationException e) {
						getDedicatedLogger().warn("Cannot validate the labeled reviews ; {} ", url, e);
					}catch (final Exception e) {
						getDedicatedLogger().error("Cannot add the labeled reviews ; {} ", url, e);
					}
				}

			} catch (final Exception e) {
				getDedicatedLogger().error("error with xpath review extraction  {} ; {} ; {} ", e.getMessage(),
						c.getXpathKeys(), url);

			}
		}
	}
}
