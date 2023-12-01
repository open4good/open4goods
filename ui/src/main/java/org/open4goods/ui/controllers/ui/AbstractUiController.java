package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.XwikiService;
import org.open4goods.ui.config.yml.UiConfig;
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


public class AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUiController.class);

	private @Autowired Environment env;
	private @Autowired UiConfig config;
	private @Autowired XwikiService xwikiService;

	

	
	// Used to load Datasource configurations from classpath
	private static final ClassLoader cl = DataSourceConfigService.class.getClassLoader();
	private static final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
	private  int maxImg;
	
	// TODO :  in a service (hot, controllers are multiply instanciated)
	@PostConstruct
	public  void init() {
		try {
			// TODO : from conf
			Resource[] resources = resolver.getResources("classpath:/static/assets/img/loader/**");
			
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
	protected ModelAndView defaultModelAndView(final String tpl, final HttpServletRequest request) {
		
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
		ret.addObject("siteLanguage", "fr");
		final Locale sl = Locale.FRANCE;

		ret.addObject("siteLocale", sl);

		ret.addObject("config",config);

		ret.addObject("dev", env.acceptsProfiles(Profiles.of("dev","devsec")));

		ret.addObject("url",request.getRequestURL().toString() );

		ret.addObject("baseUrl",config.getBaseUrl(request.getLocale()));


		ret.addObject("gaId",config.getWebConfig().getGoogleAnalyticsId());

		ret.addObject("wiki",xwikiService);

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
	
	public String loaderImage() {
		Random random = new Random();
		int nb;
		nb = random.nextInt(1,maxImg+1);
		return "/assets/img/loader/"+nb+".jpg";
		
	}
}