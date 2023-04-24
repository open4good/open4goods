package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.checkerframework.framework.qual.PostconditionAnnotation;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.model.data.AffiliationToken;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);
	
	private @Autowired AggregatedDataRepository aggregatedDataRepository;
	private @Autowired SerialisationService serialisationService;
	private @Autowired DataSourceConfigService datasourceConfigService;

	private Map<DataSourceProperties, String> partners = new HashMap<>();

	
	/**
	 * Constructor
	 */
	@PostConstruct
	public void post() {	
		for (DataSourceProperties dsp : datasourceConfigService.datasourceConfigs().values()) {
			
			AffiliationToken token = new AffiliationToken(dsp.getName(),dsp.getPortalUrl());
			String link;
			try {
				link = URLEncoder.encode(serialisationService.compressString(serialisationService.toJson(token)), Charset.defaultCharset());
			} catch (IOException e) {
				LOGGER.error("Error while generating link for partner",e);
				link = dsp.getPortalUrl();
			}
						
			partners.put(dsp, link);
		}
	}
	
	
	
	@GetMapping("/")
	public ModelAndView index(final HttpServletRequest request) {

		// TODO : Remove this test page
		ModelAndView model ;
		if (null != request.getParameter("new")) {
			 model = defaultModelAndView("index2", request);
		} else {
			 model = defaultModelAndView("index", request);
		}
		
		model.addObject("totalItems", aggregatedDataRepository.countMainIndexHavingPrice());
		
		model.addObject("partners",  partners);
		model.addObject("url",  "/");
		
		return model;
	}
		
	
}
