package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.helper.DevModeService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.services.OpenDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
public class DevModeExportController extends AbstractUiController {

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
