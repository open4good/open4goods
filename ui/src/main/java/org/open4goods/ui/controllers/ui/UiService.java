package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;


public class UiService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UiService.class);

	private @Autowired Environment env;
	protected @Autowired UiConfig config;
	private @Autowired XWikiHtmlService xwikiService;

		
	// Used to load Datasource configurations from classpath
	private static final ClassLoader cl = DataSourceConfigService.class.getClassLoader();
	private static final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
	private  int maxImg;
	
	
	private Map<String,String> languageByserverNames = new HashMap<>();
	
	// TODO :  in a service (hot, controllers are multiply instanciated)
	@PostConstruct
	public  void init() {
		
		// Setting server names
		config.getNamings().getServerNames().entrySet().forEach(e -> {
          languageByserverNames.put(e.getValue(),e.getKey());
        });
		
		
		
		try {
			// TODO : from conf
			Resource[] resources = resolver.getResources("classpath:/static/assets/img/loader/*");
			
			maxImg = resources.length;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
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
		
		
		
		// TODO(i18n,p3, 0,25)
		ret.addObject("siteLanguage", getSiteLanguage(request));
		final Locale sl = Locale.FRANCE;
		ret.addObject("siteLocale", getSiteLocale(request));

		
		
		
		ret.addObject("config",config);

		
		ret.addObject("tpl",suffix == null ? "" : suffix);
		
		ret.addObject("dev", env.acceptsProfiles(Profiles.of("dev","devsec")));

		ret.addObject("url",request.getRequestURL().toString() );

		ret.addObject("baseUrl",config.getBaseUrl(request.getLocale()));


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

			// TODO : role editor as const
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
		String referer=request.getHeader("referer");
		if (StringUtils.isEmpty(referer)) {
			LOGGER.info("Empty referer");
			return false;
		} else if (referer.startsWith(config.getBaseUrl(request.getLocale()))) {
			LOGGER.info("Request from internal source");
			return true;
		} else {
			return false;			
		}
	}


	
	public Locale getSiteLocale(HttpServletRequest request) {
		
		String language = getSiteLanguage(request);
		
		if ("default".equals(language)) {
			return Locale.ENGLISH;
		} else {
			return Locale.forLanguageTag(language);
		}
	}
	
	public String getSiteLanguage(HttpServletRequest request) {
		// TODO : Check with nginx
		String serverName = request.getServerName();

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