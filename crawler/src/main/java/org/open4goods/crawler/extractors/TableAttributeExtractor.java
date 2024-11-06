package org.open4goods.crawler.extractors;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.config.yml.datasource.ExtractorConfig;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
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
public class TableAttributeExtractor extends Extractor {

	private final static Logger logger = LoggerFactory.getLogger(TableAttributeExtractor.class);

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConf, final Locale locale, final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {

		final ExtractorConfig c = getExtractorConfig();

		if (!StringUtils.isEmpty(c.getXpathTable())) {
			///////////////////////////////////////////////////////////////////////////////////////
			// In this mode, we iterate
			// over the table, and take the tr/td[0], tr/td[0], and inject
			/////////////////////////////////////////////////////////////////////////////////////// the
			/////////////////////////////////////////////////////////////////////////////////////// th
			/////////////////////////////////////////////////////////////////////////////////////// as
			/////////////////////////////////////////////////////////////////////////////////////// context
			///////////////////////////////////////////////////////////////////////////////////////
			try {
				final NodeList table = xpathMultipleEval(document, c.getXpathTable());

//				if (1 != table.getLength()) {
//					getDedicatedLogger().warn("table extraction {} values, expecting one ; {} ; {}",
//							table.getLength(), c.getXpathKeys(), url);
//					return;
//				}
				for (int j = 0; j < table.getLength(); j++) {
					final Node n = table.item(j);
					// System.out.println("SIZE : " +
					// n.getChildNodes().getLength());
					for (int ch = 0; ch < n.getChildNodes().getLength(); ch++) {
						final Node chN = n.getChildNodes().item(ch);
						final NodeList tr = chN.getChildNodes();

						if ("th".equalsIgnoreCase(tr.item(0).getNodeName())) {
							tr.item(0).getTextContent();

							continue;
						} else {

							final String name = IdHelper.sanitize(tr.item(0).getTextContent());
							//TODO(feature) : could infer splitter tags;
							final String value = IdHelper.sanitize(extractContentWithCariageReturns(tr.item(1),null));
							final String language = locale.getLanguage();

							
									p.addAttribute(name, value, language,null);

						}

					}

				}
			} catch (final Exception e) {
				getDedicatedLogger().error("error with table extraction  {} ; {} ; {} ", e.getMessage(),
						c.getXpathKeys(), url);

			}

		}

		/////////////////////////////////////////////////
		// Fetching by key values pair
		/////////////////////////////////////////////////

		if (!StringUtils.isEmpty(c.getXpathValues())) {

			final List<String> values = evalMultipleAndLogs (document, c.getXpathValues(),url);

			if (StringUtils.isEmpty(c.getXpathKeys())) {

				final String splitChars = c.getXpathSplitChars();
				if (StringUtils.isEmpty(splitChars)) {
					logger.error("You must either fill xpathValue or xpathSplitChars : {}", url);
					return;
				} else {
					for (final String value : values) {
						if (StringUtils.isEmpty(value)) {
							continue;
						}

						final String[] fragments = value.split(splitChars);
						if (fragments.length != 2) {
							getDedicatedLogger().info("Was expecting 2 fragments. Got {} for {} at {}",fragments.length,value,url);
						} else {
							logger.debug("Adding attribute : {}:{}", fragments[0], fragments[1]);
							p.addAttribute(fragments[0], fragments[1], locale.getLanguage(),null);
						}



					}
				}

			} else {

				final List<String> keys =  evalMultipleAndLogs(document, c.getXpathKeys(),url,c.getSanitize());

				if (keys.size() != values.size()) {
					getDedicatedLogger().warn("Mismatch in number of keys / values : {} <> {}. Skipped. ; {}",
							keys.size(), values.size(), url);
					return;
				}

				for (int i = 0; i < keys.size(); i++) {
					final String key = keys.get(i);
					final String val = values.get(i);
					logger.debug("Adding attribute : {}:{}", key, val);

					if (StringUtils.isEmpty(key)) {
						getDedicatedLogger().warn("Empty key with value '{}' at {}",val,url);
						continue;
					}

					if (StringUtils.isEmpty(val)) {
						getDedicatedLogger().warn("Empty val for key {} at {}",key,url);
						continue;
					}

					p.addAttribute(key, val, locale.getLanguage(),null);

				}
			}
		}

	}

}
