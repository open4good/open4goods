package org.open4goods.ui.controllers.ui;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.constants.ProductState;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.dto.VerticalSearchResponse;
import org.open4goods.ui.services.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Sets;

@Controller
//	TODO(i18n, P2, 0.25) : I18n the pathes
public class GlobalSearchController extends AbstractUiController {

	
	@Autowired private SearchService searchService;
	@Autowired UiConfig config;
	

	@PostMapping({"/recherche/{query}"})
	public ModelAndView searchPost(final HttpServletRequest request, @PathVariable String query, HttpServletResponse response) {

		response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
		
		String[] frags = request.getServletPath().substring(1).split("/");
		response.setHeader("Location", config.getBaseUrl(Locale.FRANCE) +  frags[0]+"/"+ URLEncoder.encode(frags[1],Charset.defaultCharset()) );
		return null;
	}	

	@GetMapping({"/recherche/{query}"})
	public ModelAndView searchGet(final HttpServletRequest request, @PathVariable String query) {

		String q = StringUtils.normalizeSpace(URLDecoder.decode(query, Charset.defaultCharset()));
		
		
		ModelAndView model = defaultModelAndView("search", request);

//		Set<String> categroies = Sets.newHashSet("JOUR>AUTRES MEUBLES");
		VerticalSearchResponse results = searchService.globalSearch(q, null, null, null, null,0,50);
		
		model.addObject("results",results);
		model.addObject("query", q);
		
		return model;
	}	
	
	
	@RequestMapping({"/recherche","/recherche/"})
	public ModelAndView search(final HttpServletRequest request) {
		return searchGet(request,"");
	}	
	
	
}
