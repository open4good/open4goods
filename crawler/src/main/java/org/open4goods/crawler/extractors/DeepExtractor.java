package org.open4goods.crawler.extractors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.config.yml.datasource.HtmlDataSourceProperties;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.model.data.DataFragment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 *
 */
public class DeepExtractor extends Extractor {


	@Autowired
	private ApplicationContext applicationContext;

	List<Extractor> extractors = null;



	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConfig,  final Locale locale,
                      final DataFragment p, final DataFragmentWebCrawler offerWebCrawler, final CrawlController controller) {

		final ExtractorConfig ec = getExtractorConfig();

		final HtmlDataSourceProperties webdatasource = providerConfig.webDataSource();

		////////////////////////
		// Instanciating extractors if first time
		// TODO(gof) : should outside in another common classe. But could be more globally refactored into a non introspected manier. @see WebDatasourceFetchingService
		////////////////////////
		if (null == extractors) {

			extractors = new ArrayList<>();
			for (final ExtractorConfig conf : ec.getExtractors()) {
				try {
					final Extractor extractor = getInstance(conf, getDedicatedLogger());
					applicationContext.getAutowireCapableBeanFactory().autowireBean(extractor);
					extractors.add(extractor);
				} catch (final Exception e) {
					getDedicatedLogger().error("Cannot instanciate extractor {}", conf.getClass(), e);
				}
			}
		}

		///////////////////////////////
		// Computing url base
		//////////////////////////////

		if (null == ec.getUrl()) {
			getDedicatedLogger().warn("No url defined for DeepExtraction : {}",url);
			return;
		}

		String targetUrl = evalAndLogs(document, ec.getUrl(), url);

		if (null == targetUrl) {
			getDedicatedLogger().warn("Cannot evalute targetUrl to DeepExtract : {} at {}",ec.getUrl(),url);
			return;
		}

		// handling url replacement
		if (null != ec.getUrlReplacement()) {
			for (final Entry<String,String> repl : ec.getUrlReplacement().entrySet()) {
				targetUrl = targetUrl.replace(repl.getKey(), repl.getValue());
			}
		}


		if (!targetUrl.startsWith("http")) {
			targetUrl = webdatasource.getBaseUrl() + (targetUrl.startsWith("/") ? targetUrl : "/"+targetUrl);
		}

		Integer current = ec.getStartPage();
		boolean more = true;


		while (more) {

			if (StringUtils.hasLength(ec.getParameterPage())) {
				targetUrl = getPaginatedUrl(targetUrl,ec.getParameterPage(),current);
			}

			if (StringUtils.hasLength(ec.getPathIncrementVariablePrefix())) {
				targetUrl = getPathIncrementVariable(targetUrl,ec.getPathIncrementVariablePrefix(),ec.getPathIncrementVariableSuffix(), current);
			}

			current++;
			if (current > ec.getPageLimit()) {
				getDedicatedLogger().error("Stop deep visiting because it exceeds the page limt {} : {}",ec.getPageLimit(), targetUrl);
				break;
			}

			getDedicatedLogger().info("Deep visiting {}",targetUrl);
			final DataFragment deepData = offerWebCrawler.visitNow(controller,  targetUrl, extractors);

			more = false;
			if (null == deepData) {
				getDedicatedLogger().error("Stop deep visiting because no datafragment retrieved : {}",targetUrl);
				break;
			}

			//TODO(gof) : complete with other extractors
			for (final Extractor e : extractors) {
				if (e instanceof TableAttributeExtractor) {
					if (deepData.getAttributes().size() >0) {
						p.getAttributes().addAll(deepData.getAttributes());
						more = true;
					}

				}
				else {
					getDedicatedLogger().error("The extractor {} has not been coded in DeepExtractors : {}",e.getClass().getSimpleName(),url);
				}
			}



			if (!StringUtils.hasLength(ec.getPathIncrementVariablePrefix()) && !StringUtils.hasLength(ec.getParameterPage())) {
				// If no parameter page url, this is a one shot deep crawling
				more = false;
			}

		}
	}


	/**
	 * *
	 * @param targetUrl
	 * @param pathIncrementVariablePrefix
	 * @param current
	 * @return
	 */
	private String getPathIncrementVariable(final String targetUrl, final String pathIncrementVariablePrefix, final String pathIncrementVariableSuffix, final Integer current) {

		final String prefix = targetUrl.substring(0,targetUrl.indexOf(pathIncrementVariablePrefix) + pathIncrementVariablePrefix.length());
		final String suffix = targetUrl.substring(targetUrl.indexOf(pathIncrementVariableSuffix, prefix.length()));

		return prefix+current+suffix;
	}


	private String getPaginatedUrl(final String baseUrl, final String parameterName, final Integer parameterValue) {
		final StringBuilder b = new StringBuilder();

		final UriComponents parsedUrl = UriComponentsBuilder.fromUriString(baseUrl).build();

		b.append(parsedUrl.getScheme());
		b.append("://");
		b.append(parsedUrl.getHost());
		if (!StringUtils.hasLength(parsedUrl.getPath())) {
			b.append("/");
		} else {
			b.append(parsedUrl.getPath());
		}

		final MultiValueMap<String, String> params = parsedUrl.getQueryParams();

		final Map<String, String> allParams = new HashMap<>();

		for (final Entry<String, List<String>> str : params.entrySet()) {

			if (str.getValue().size() != 1) {
				getDedicatedLogger().warn("A multi valued param : {} has {} values in {}", str.getKey(), str.getValue().size(), baseUrl);
			} else {
				allParams.put(str.getKey(), str.getValue().get(0));
			}
		}

		// Adding our param
		allParams.put(parameterName, parameterValue.toString());


		int counter = 0;
		for (final Entry<String, String> str : allParams.entrySet()) {

			b.append(counter == 0 ? "?" : "&");
			b.append(str.getKey()).append("=").append(str.getValue());
			counter++;
		}




		return b.toString();
	}

}
