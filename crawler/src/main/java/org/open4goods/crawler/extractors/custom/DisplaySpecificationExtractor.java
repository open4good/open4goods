package org.open4goods.crawler.extractors.custom;

import java.util.Locale;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.extractors.Extractor;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.UnindexedKeyValTimestamp;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.collect.Sets;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 *
 */
public class DisplaySpecificationExtractor extends Extractor {

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConfig, final Locale locale,
                      final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {



		try {
			// Handling comments.. BazaarVoice is easy to browse .. No productId, get all the comments (gurps !)

			 final NodeList keys = xpathMultipleEval(document, "//tr/td[1]");
			 final NodeList values = xpathMultipleEval(document, "//tr/td[2]");


			 if (keys.getLength() != values.getLength()) {
				 getDedicatedLogger().error("Not the same number of keys and values in Dispolay§SpecExtractor : {}",url);
				 return;
			 }


			 for (int i =0; i < keys.getLength(); i++) {
				 final Node key = keys.item(i);
				 final Node value = values.item(i);

				 try {



					final String attrKey =  IdHelper.sanitize(key.getFirstChild().getTextContent());
					final String v = IdHelper.sanitize(extractContentWithCariageReturns(value,Sets.newHashSet("br")));

					if (!org.apache.commons.lang3.StringUtils.isEmpty(attrKey) && !org.apache.commons.lang3.StringUtils.isEmpty(v)) {
						p.addAttribute(attrKey, v, page.getLanguage(),false,Sets.newHashSet("\r\n", "\n"));
					}
				} catch (final Exception e) {
					getDedicatedLogger().info("Cannot handle a key/pair");
				}



			 }

			 // Adding alternate id's

			 final Attribute alias = p.getAttribute("MODEL ALIAS");
			 if (null != alias) {
				 if (alias.multivalued()) {
					 alias.stringValues().stream().forEach(e -> p.getAlternateIds().add( new UnindexedKeyValTimestamp(ReferentielKey.MODEL.toString(), e)));
				 } else {
					 getDedicatedLogger().warn("Alias presents ({}) but not multivalued at {} ",alias,url);
				 }
			 }


		} catch (final Exception e) {
			getDedicatedLogger().error("Error while parsing comments : {}",url,e);
		}





	}
}
