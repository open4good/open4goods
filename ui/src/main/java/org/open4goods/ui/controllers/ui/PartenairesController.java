package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.model.data.AffiliationToken;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.SerialisationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Controller
/**
 * This controller maps the index page
 *
 * @author gof
 *
 */
public class PartenairesController  {

	private static final Logger LOGGER = LoggerFactory.getLogger(PartenairesController.class);

	// The siteConfig
	private final UiConfig config;
	private @Autowired UiService uiService;
	private final DataSourceConfigService datasourceConfigService;

	private final SerialisationService serialisationService;

	private Map<DataSourceProperties, String> partners = new HashMap<>();

	public PartenairesController(UiConfig config, DataSourceConfigService datasourceConfigService, SerialisationService serialisationService) {
		this.config = config;
		this.datasourceConfigService = datasourceConfigService;
		this.serialisationService = serialisationService;
	}


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




	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	@GetMapping("/partenaires")
	public ModelAndView partenaires(final HttpServletRequest request) {
		ModelAndView ret = uiService.defaultModelAndView(("partenaires"), request);
		ret.addObject("page","compensation écologique");
		ret.addObject("partners",  partners);
		return ret;
	}

}