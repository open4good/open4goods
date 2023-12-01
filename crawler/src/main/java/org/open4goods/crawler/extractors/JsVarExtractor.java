package org.open4goods.crawler.extractors;

import java.util.Locale;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.w3c.dom.Document;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 *
 */
public class JsVarExtractor extends Extractor {

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConfig, final Locale locale,
                      final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {

		final ExtractorConfig c = getExtractorConfig();

		// Extracting attributes from JS variables definitions

		for (final Entry<String, String> attr : c.getAttributes().entrySet()) {

			final String jsVar = extractJsVar(parseData, document, attr.getValue(), url,c.getEqualSign());

			if (StringUtils.isEmpty(jsVar)) {
				getDedicatedLogger().warn("Cannot extract attribute {} with jsVarExtractor ({}) at {}", attr.getKey(),
						attr.getValue(), url);
			} else {


				if (ReferentielKey.isValid(attr.getKey().toUpperCase())) {
					// Adding as referentiel attribute
					p.addReferentielAttribute(attr.getKey().toUpperCase(), StringEscapeUtils.unescapeHtml4(jsVar).trim());
				} else {
					// Adding as classical attribute
					p.addAttribute(attr.getKey(), StringEscapeUtils.unescapeHtml4(jsVar).trim(), locale.getLanguage(),c.getIgnoreCariageReturns(),c.getAttributeSeparators());

				}




			}
		}

	}



}
