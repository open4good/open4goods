
package org.open4goods.crawler.extractors;

import java.util.Locale;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.data.DataFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 *
 */
public class JsonLdExtractor extends Extractor {

	private final static Logger logger = LoggerFactory.getLogger(JsonLdExtractor.class);

	private final JsonExtractor jsonExtractor = new JsonExtractor();


	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties conf, final Locale locale,
                      final DataFragment DataFragment, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {

		final ExtractorConfig c = getExtractorConfig();

		if (null != c.getXpathJsonLd()) {
			String jsonLd = null;
			try {

				if (!StringUtils.isEmpty(c.getXpathJsonLdMustContains())) {

					final NodeList lds = xpathMultipleEval(document, c.getXpathJsonLd());

					for (int i =0; i < lds.getLength(); i++) {
						final String tmp = lds.item(i).getTextContent();
						if (tmp.contains(c.getXpathJsonLdMustContains())) {
							jsonLd = tmp;
							break;
						}
					}
					if (StringUtils.isEmpty(jsonLd)) {
						getDedicatedLogger().warn("No Json-ld informations containing {}  in {} at {}",c.getXpathJsonLdMustContains(), c.getXpathJsonLd(),url);
					}


				} else {
					jsonLd = xpathEval(document, c.getXpathJsonLd());
				}
//
//				if (conf.getDevMode() && env.acceptsProfiles("dev")) {
//					getDedicatedLogger().warn("JsonLD for {} : {}", c.getXpathJsonLd(), jsonLd);
//				}

				if (StringUtils.isEmpty(jsonLd)) {
					getDedicatedLogger().warn("Empty Json-ld informations in {} at {}", c.getXpathJsonLd(),url);
					return;
				}

				jsonLd = jsonLd.replace("&quot;", "&[quot;");




				final int bracketPos = jsonLd.indexOf("[");
				final int accoPos = jsonLd.indexOf('{');

				if (bracketPos != -1 && bracketPos < accoPos ) {
					jsonLd =  jsonLd.substring(bracketPos, jsonLd.lastIndexOf(']') + 1);
				} else {
					jsonLd =  jsonLd.substring(jsonLd.indexOf('{'), jsonLd.lastIndexOf('}') + 1);
				}
				jsonLd = jsonLd.replace("&[quot;", "&quot;");

				// Replace cariage return
				jsonLd = jsonLd.replace("\r\n", "");
				jsonLd = jsonLd.replace("\n", "");

				try {

					jsonExtractor.parse(jsonLd, DataFragment, getDedicatedLogger(), getExtractorConfig(), page, locale,
							conf, offerWebCrawler);

				} catch (final Exception e1) {
					logger.warn("Unexpected exception while processing json-ld ; {} at {} ", jsonLd, url, e1);
				}
			} catch (XPathExpressionException | IndexOutOfBoundsException e) {
				getDedicatedLogger().warn("Error while finding Json-ld informations in {} at {} ; {}", c.getXpathJsonLd(), url, e.getMessage());
			} catch (final ResourceNotFoundException e) {
				getDedicatedLogger().warn("No Json-ld informations in {} at {}", c.getXpathJsonLd(),url);
			}

		} else {
			getDedicatedLogger().error("Cannot use jsonLdExtractor, missing jsonLd xpath expression");
		}

	}
}
