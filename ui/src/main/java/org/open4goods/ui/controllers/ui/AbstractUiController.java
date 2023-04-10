package org.open4goods.ui.controllers.ui;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;


public class AbstractUiController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUiController.class);
	
	private @Autowired Environment env;
	private @Autowired UiConfig config;
	
	/**
	 * Instanciates a ModelAndView and prefills from conf and from httpRequest
	 * with : <br/>
	 * > Default attributes (languages....) > Page commons fields (titles,
	 * metas, ....)
	 *
	 * @param string
	 * @return
	 */
	protected ModelAndView defaultModelAndView(final String template, final HttpServletRequest request) {
		final ModelAndView ret = new ModelAndView(template).addObject("config", config);

		ret.addObject("userLocale", request.getLocale());
		// TODO(i18n,p3, 0,25)
		ret.addObject("siteLanguage", "fr");
		final Locale sl = Locale.FRANCE;	

		ret.addObject("siteLocale", sl);

		ret.addObject("config",config);

		ret.addObject("dev", env.acceptsProfiles("dev","devsec"));

		ret.addObject("url",request.getRequestURL().toString() );
		
		ret.addObject("baseUrl",config.getBaseUrl(request.getLocale()));
		
		
		ret.addObject("gaId",config.getWebConfig().getGoogleAnalyticsId());
	
		// Retrieve authentication status
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof UsernamePasswordAuthenticationToken)  {
			ret.addObject("user",authentication.getName());
		}
		
	
		
		
		return ret;
	}
}
