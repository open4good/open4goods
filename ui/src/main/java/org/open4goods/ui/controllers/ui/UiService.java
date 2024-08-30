package org.open4goods.ui.controllers.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

// TODO(p2,design) : Merge with uihelper
public class UiService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UiService.class);

	private @Autowired Environment env;
	protected @Autowired UiConfig config;
	private @Autowired XWikiHtmlService xwikiService;

		
	// Used to load Datasource configurations from classpath
	private  int maxImg;
	
	
	private Map<String,String> languageByserverNames = new HashMap<>();
	
	@PostConstruct
	public  void init() {
		
		// Setting server names
		config.getNamings().getServerNames().entrySet().forEach(e -> {
          languageByserverNames.put(e.getValue(),e.getKey());
        });
		
	
	}
	
	
	/**
	 * Instanciates a ModelAndView and prefills pageNumber conf and pageNumber httpRequest
	 * with : <br/>
	 * > Default attributes (languages....) > Page commons fields (titles,
	 * metas, ....)
	 *
	 * @param string
	 * @return
	 */
	public ModelAndView defaultModelAndView(final String tpl, final HttpServletRequest request) {
		
		String template;
		
		
		// Checking the "tpl" parameter for A/B testing new pages		
		String suffix = request.getParameter("tpl");
		if (null != suffix) {
			template = tpl + suffix;
		} else {
			template = tpl;
		}
		
		final ModelAndView ret = new ModelAndView(template).addObject("config", config);

		ret.addObject("userLocale", request.getLocale());
		
		ret.addObject("siteLanguage", getSiteLanguage(request));
		Locale siteLocale = getSiteLocale(request);
		ret.addObject("siteLocale", siteLocale);
		
		ret.addObject("config",config);

		
		ret.addObject("tpl",suffix == null ? "" : suffix);
		
		ret.addObject("dev", env.acceptsProfiles(Profiles.of("dev","devsec")));

		ret.addObject("url",request.getRequestURL().toString() );

		ret.addObject("baseUrl",config.getBaseUrl(siteLocale));


		ret.addObject("loaderImg",loaderImage() );
		/* test Laurent */
		ret.addObject("loaderImgLogo",loaderImageLogo() );
		/* test Laurent */

		ret.addObject("gaId",config.getWebConfig().getGoogleAnalyticsId());

		ret.addObject("wiki",xwikiService);

		ret.addObject("internalReferer", isInternalReferer(request));
		
		
		// Retrieve authentication status
		Authentication authentication = SecurityContextHolder .getContext().getAuthentication();
		if (authentication instanceof UsernamePasswordAuthenticationToken)  {
			ret.addObject("user",authentication.getName());

			Set<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
			ret.addObject("roles",roles) ;

			// TODO(p3,conf) : role editor as const
			if (roles.contains("SITEEDITOR")) {
				ret.addObject("editor","true") ;

			}
		}
		return ret;
	}
	
	
	/**
	 * Return true if an internal source
	 * @param request
	 * @return
	 */
	private boolean isInternalReferer(HttpServletRequest request) {
		if (null == request) {
			return false;
		}
		
		String referer=request.getHeader("referer");
		String baseUrl= config.getBaseUrl(request.getLocale());
		if (StringUtils.isEmpty(baseUrl)) {
			LOGGER.info("Empty baseUrl for locale {}",request.getLocale());
			return false;
		}
		if (StringUtils.isEmpty(referer)) {
			LOGGER.info("Empty referer");
			return false;
		} else if (referer.startsWith(baseUrl)) {
			LOGGER.info("Request from internal source");
			return true;
		} else {
			return false;			
		}
	}


	/**
	 * Return the SITE locale for a request
	 * @param request
	 * @return
	 */
	public Locale getSiteLocale(HttpServletRequest request) {		
		String language = getSiteLanguage(request);		
		if ("default".equals(language)) {
			return Locale.ENGLISH;
		} else {
			return Locale.forLanguageTag(language);
		}
	}
	
	/**
	 * Return the SITE language for a request
	 * @param request
	 * @return
	 */
	public String getSiteLanguage(HttpServletRequest request) {
		String serverName = request.getServerName();
		
		if (config.getNamings().getServerNames().containsValue(serverName)) {
			LOGGER.info("Server name {} found in configuration", serverName);
		} else {
			// TODO(p3,security) : Raise 403 ?
			LOGGER.error("Server name {} not found in configuration", serverName);
		}
		
		String language = languageByserverNames.get(serverName);		
		if (null == language) {
			return "default";
		} else {
			return language;			
		}
	}
	
	public String loaderImage() {		
		return "/assets/img/loader/"+random(1, maxImg)+".webp";		
	}

	public String loaderImageLogo() {		
		return "/assets/img/logo/NUDGER.png";		
	}
	
	public int random(int min, int max) {
		return min + (int)(Math.random() * ((max - min) + 1));
	}
}