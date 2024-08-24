
package org.open4goods.crawler.extractors;

import java.util.Locale;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.text.StringEscapeUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.data.DataFragment;
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
 *
 */
public class ResourceExtractor extends Extractor {

	private final static Logger logger = LoggerFactory.getLogger(ResourceExtractor.class);

//	private final static ObjectMapper mapper = new ObjectMapper();// .writerWithDefaultPrettyPrinter();

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties conf,  final Locale locale, final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {

		NodeList result;
		try {
			result = xpathMultipleEval(document, "//a/@href");
			for (int i = 0; i < result.getLength(); i++) {
				final String u = StringEscapeUtils.unescapeHtml4(result.item(i).getTextContent());

				final String testContains = getExtractorConfig().getResourceUrlMustContains();
				if (null != testContains && !u.contains(testContains)) {
					continue;
				}

				if (u.endsWith(".pdf")) {
					try {
						p.addResource(u);
					} catch (final ValidationException e) {
						getDedicatedLogger().warn("Unable to add resource ; {} ; {}", e.getMessage(), url);
					}
				}
			}
		} catch (final XPathExpressionException e) {
			logger.error("Error with xpath expression in ResourceExtractor", e);
		}

	}

}
