package org.open4goods.ui.controllers.ui.pages;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.redfin.sitemapgenerator.ChangeFreq;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class OpenSourceVerticalController  implements SitemapExposedController{

	public static final String DEFAULT_PATH="/opensource/categories-produits";
	public static final String FR_PATH="/opensource/categories-produits";
	
	
	private @Autowired UiService uiService;	
	private @Autowired UiConfig uiConfig;
	
	private String defaultYaml;


	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY)
						   .add(SitemapEntry.LANGUAGE_FR, FR_PATH);
	}

	
	@GetMapping(value = {DEFAULT_PATH, FR_PATH})
	public ModelAndView index(final HttpServletRequest request) {

		ModelAndView model = uiService.defaultModelAndView("opensource", request);

		if (null == defaultYaml) {
			// TODO : from conf
					// TODO : Cache
					try {
						this.defaultYaml =  IOUtils.toString(new URL("https://raw.githubusercontent.com/open4good/open4goods/main/verticals/src/main/resources/verticals/_default.yml"));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
			}

			model.addObject("defaultYaml", defaultYaml);
		
	
		return model;
	}



}
