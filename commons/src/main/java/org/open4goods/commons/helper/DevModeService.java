package org.open4goods.commons.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Stream;

import org.open4goods.commons.config.yml.DevModeConfiguration;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * This service provides stuff used in DevMode, is used to populate the database with demo data in dev mode, 
 */
public class DevModeService {

	static Logger LOGGER = LoggerFactory.getLogger(DevModeService.class);
	private SerialisationService serialisationService;
	private ProductRepository repository;
	private DevModeConfiguration productLoadEndpoint;
	private final VerticalsConfigService verticalConfigService;

	public DevModeService(DevModeConfiguration devModeConfiguration, ProductRepository repository,
			SerialisationService serialisationService, VerticalsConfigService verticalsConfigService) {

		this.serialisationService = serialisationService;
		this.repository = repository;
		this.productLoadEndpoint = devModeConfiguration;
		this.verticalConfigService = verticalsConfigService;

		checkAndIngest();

	}

	/**
	 * Check if the database needs data ingestion and do it if needed
	 */
	public void checkAndIngest() {
		LOGGER.info("DevMode : Checking if products is needed");
		if (needsUpdate()) {
			try {
				indexProductsFromEndpoint();
			} catch (IOException e) {
				LOGGER.error("Error while ingesting products", e);
			}
		}
	}
	
	/**
	 * Check if the database needs data ingestion
	 * @return
	 */
	public boolean needsUpdate() {
		boolean ret = repository.countMainIndexHavingPrice() == 0;
		LOGGER.info("DevMode : Products needs update : "+ret);
		return ret;
	}
	
	/**
	 * Fetch the demo products into stores. Index products from the configured endpoint, updating timestamp to have o longer product life. 
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	public void indexProductsFromEndpoint() throws IOException, MalformedURLException {
		LOGGER.info("DevMode : Indexing products from endpoint : "+productLoadEndpoint.getDevModeProductEndpoint());
		try (InputStream is = new URL(productLoadEndpoint.getDevModeProductEndpoint()).openConnection().getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				Stream<String> stream = reader.lines()) {
			stream.forEach(e -> {
				try {
					Product p = serialisationService.fromJson(e, Product.class);
					p.getPrice().getMinPrice().setTimeStamp(System.currentTimeMillis());
					LOGGER.info("DevMode : Indexing product : "+p.getId());
					repository.forceIndex(p);
				} catch (Exception e1) {
					LOGGER.error("Error while importing product", e1);
				}

			});

		}
	}

	/**
	 * This endpoint streams sample data to an HTTP Response. It is used from the other side, in order to produce the JSON Demo data set
	 * @param response
	 * @throws IOException
	 */
	public void streamSampleProducts(HttpServletResponse response) throws IOException {
		response.setHeader(HttpHeaders.CONTENT_ENCODING, MediaType.APPLICATION_JSON_VALUE);

		////////////////////
		// For each vertical
		////////////////////
		verticalConfigService.getConfigsWithoutDefault().forEach(vertical -> {
			////////////////////
			// Export a subset
			///////////////////
			repository.exportVerticalWithValidDate(vertical, false).limit(100).forEach(p -> {
				try {
					// Set last offer date to provide a longer product visibility in UI
					response.getWriter().write(serialisationService.toJson(p)+"\n");
				} catch (IOException e) {
					LOGGER.error("Error while streaming products", e);
				}
			});
		});
	}

}
