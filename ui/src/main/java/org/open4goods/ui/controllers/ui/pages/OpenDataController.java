package org.open4goods.ui.controllers.ui.pages;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.services.OpenDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.redfin.sitemapgenerator.ChangeFreq;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
/**
 * This controller maps the opendata page and dataset
 *
 * @author gof
 *
 */
public class OpenDataController  implements SitemapExposedController{

	public static final String DEFAULT_PATH="/opendata";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataController.class);

	// The siteConfig
	private final OpenDataService openDataService;
	private @Autowired UiService uiService;
	public OpenDataController(OpenDataService openDataService) {
		this.openDataService = openDataService;
	}

	/**
	 * The Home page.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws UnirestException
	 */

	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY);
	}
	
	@GetMapping(value = {DEFAULT_PATH})	
	public ModelAndView opendata(final HttpServletRequest request) {
		final ModelAndView ret = uiService.defaultModelAndView("opendata", request);
		ret.addObject("count", openDataService.totalItems());
		ret.addObject("lastUpdated", openDataService.lastUpdate());
		ret.addObject("fileSize", openDataService.fileSize());
		ret.addObject("page","open data");
		return ret;
	}

	@GetMapping(path = "/opendata/gtin-open-data.zip")
	public void opensearch(final HttpServletResponse response) throws IOException {
		try (InputStream str = openDataService.limitedRateStream()){
			response.setHeader("Content-type", "application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"gtin-open-data.zip\"");
			IOUtils.copy(str, response.getOutputStream());
		} catch (IOException e) {
			LOGGER.error("opendata file download error or interruption : {}",e.getMessage());
			openDataService.decrementDownloadCounter();
		} catch (TechnicalException e) {
			response.sendError(429, "Exceding the " + OpenDataService.CONCURRENT_DOWNLOADS + " concurrent downloads availlable");
			LOGGER.error("opendata file download error : {}",e.getMessage());
		}
	}

}