package org.open4goods.api.controller.ui;

import java.io.IOException;

import org.open4goods.commons.helper.DevModeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletResponse;

@Controller
/**
 * This controller maps the opendata page and dataset
 *
 * @author gof
 *
 */
public class DevModeExportController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DevModeExportController.class);

	// The siteConfig
	private final DevModeService devModeService;

	
	public DevModeExportController(DevModeService devModeService) {
		this.devModeService = devModeService;
	}
	
    @GetMapping(value = "/devmode/products")
    /**
     * Deliver a subset of products for dev mode alimentation
     * @param response
     * @throws IOException
     */
    public void demoProducts(HttpServletResponse response) throws IOException {
    	LOGGER.info("Delivering demo products");
    	devModeService.streamSampleProducts(response);
    	
    }
}
