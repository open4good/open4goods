package org.open4goods.ui.controllers.ui.pages;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.open4goods.commons.helper.IpHelper;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.data.GlobalUserSearch;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.SearchService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.repository.UserSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class GlobalSearchController  implements SitemapExposedController{

	public static final String DEFAULT_PATH="/search";
	public static final String FR_PATH="/recherche";


	private final SearchService searchService;
	private final UiConfig config;
	private @Autowired UiService uiService;
	private UserSearchRepository searchRepository;
	
	public GlobalSearchController(SearchService searchService, UiConfig config, UserSearchRepository searchRepository) {
		this.searchService = searchService;
		this.config = config;
		this.searchRepository = searchRepository;
	}

	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY)
						   .add(SitemapEntry.LANGUAGE_FR, FR_PATH);
	}
	
	@PostMapping({ FR_PATH+ "/{query}",  DEFAULT_PATH+ "/{query}"})
	public ModelAndView searchPost(final HttpServletRequest request, @PathVariable String query, HttpServletResponse response) {

		response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);

		String[] frags = request.getServletPath().substring(1).split("/");
		response.setHeader("Location", config.getBaseUrl(Locale.FRANCE) +  frags[0]+"/"+ URLEncoder.encode(frags[1],Charset.defaultCharset()) );
		return null;
	}

	@GetMapping({FR_PATH, DEFAULT_PATH})
	// TODO : PErf : Caching
	public ModelAndView searchGet(final HttpServletRequest request, @RequestParam(required = false, defaultValue = "") String q) {

			ModelAndView model = uiService.defaultModelAndView("search", request);

		//		Set<String> categroies = Sets.newHashSet("JOUR>AUTRES MEUBLES");


		VerticalSearchResponse results = searchService.globalSearch(q, null, null, null, null,0,25,0, true);

		model.addObject("results",results);
		model.addObject("query", q);

		String amazonSearchLinkTemplate = config.getAmazonConfig().getAmazonSearchLink();
		String encodedQuery = URLEncoder.encode(q, StandardCharsets.UTF_8);
		String amazonLink = amazonSearchLinkTemplate
				.replace("{search_terms}", encodedQuery)
				.replace("{affiliate_tag}", config.getAmazonConfig().getAffiliateTag());

		model.addObject("amazonLink", amazonLink);
		
		
		// Saving the user search
		// TODO(perf) : Bulk, parallelize		
		GlobalUserSearch search = new GlobalUserSearch(IpHelper.getIp(request), request.getHeader("User-Agent"), q);
		searchRepository.save(search);

		return model;
	}


	@RequestMapping({FR_PATH, DEFAULT_PATH})
	public ModelAndView search(final HttpServletRequest request) {
		return searchGet(request,"");
	}


}
