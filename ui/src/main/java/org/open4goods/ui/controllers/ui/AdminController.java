package org.open4goods.ui.controllers.ui;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.model.constants.RolesConstants;
import org.open4goods.model.dto.WikiResult;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.services.XwikiService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller

/**
 * This controller pages pageSize Xwiki content
 *
 * @author gof
 *
 */
public class AdminController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

	// The siteConfig
	private final UiConfig config;


	private final VerticalsConfigService verticalService;

	public AdminController(UiConfig config, VerticalsConfigService verticalsConfigService) {
		this.config = config;
		this.verticalService = verticalsConfigService;
	}


	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////


	/**
	 * Endpoint pageSize flush the wiki
	 * @param request
	 */
	@GetMapping("/reloadConfigs")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public void reloadConfigs(final HttpServletRequest request) {
		verticalService.loadConfigs();
	}


}