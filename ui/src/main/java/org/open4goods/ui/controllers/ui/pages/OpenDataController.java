package org.open4goods.ui.controllers.ui.pages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.services.OpenDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
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

	private final OpenDataService openDataService;
	private @Autowired UiService uiService;
	private final UiConfig uiConfig;

	@Autowired
	public OpenDataController(OpenDataService openDataService, UiConfig uiConfig) {
		this.openDataService = openDataService;
		this.uiConfig = uiConfig;
	}

	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY);
	}

	@Override
	public List<SitemapEntry> getMultipleExposedUrls() {
		return Arrays.asList(
				SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY),
				SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, "/opendata/gtin", 0.3, ChangeFreq.YEARLY),
				SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, "/opendata/isbn", 0.3, ChangeFreq.YEARLY)
		);
	}

	@GetMapping(value = {DEFAULT_PATH})
	public ModelAndView opendata(final HttpServletRequest request) {
		final ModelAndView ret = uiService.defaultModelAndView("opendata", request);
		ret.addObject("count", openDataService.totalItems());
		ret.addObject("countGTIN", openDataService.totalItemsGTIN());
		ret.addObject("countISBN", openDataService.totalItemsISBN());
		ret.addObject("isbnLastUpdated", openDataService.isbnLastUpdate());
		ret.addObject("isbnFileSize", openDataService.isbnFileSize());
		ret.addObject("gtinLastUpdated", openDataService.gtinLastUpdate());
		ret.addObject("gtinFileSize", openDataService.gtinFileSize());
		ret.addObject("page", "open data");
		return ret;
	}

	@GetMapping(value = {DEFAULT_PATH + "/gtin"})
	public ModelAndView opendataGtin(final HttpServletRequest request) {
		final ModelAndView ret = uiService.defaultModelAndView("opendata-gtin", request);
		ret.addObject("lastUpdated", openDataService.gtinLastUpdate());
		ret.addObject("countGTIN", openDataService.totalItemsGTIN());
		ret.addObject("fileSize", openDataService.gtinFileSize());
		ret.addObject("page", "gtin data");
		return ret;
	}

	@GetMapping(value = {DEFAULT_PATH + "/isbn"})
	public ModelAndView opendataIsbn(final HttpServletRequest request) {
		final ModelAndView ret = uiService.defaultModelAndView("opendata-isbn", request);
		ret.addObject("lastUpdated", openDataService.isbnLastUpdate());
		ret.addObject("countISBN", openDataService.totalItemsISBN());
		ret.addObject("fileSize", openDataService.isbnFileSize());
		ret.addObject("page", "isbn data");
		return ret;
	}

	@GetMapping(path = "/opendata/gtin-open-data.zip")
	public void downloadGtinData(final HttpServletResponse response) throws IOException {
		downloadData(response, "gtin-open-data.zip", uiConfig.gtinZipFile());
	}

	@GetMapping(path = "/opendata/isbn-open-data.zip")
	public void downloadIsbnData(final HttpServletResponse response) throws IOException {
		downloadData(response, "isbn-open-data.zip", uiConfig.isbnZipFile());
	}

	private void downloadData(final HttpServletResponse response, String fileName, File zipFile) throws IOException {
		try (InputStream str = new FileInputStream(zipFile)) {
			response.setHeader("Content-type", "application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
			IOUtils.copy(str, response.getOutputStream());
		} catch (IOException e) {
			LOGGER.error("opendata file download error or interruption : {}", e.getMessage());
			openDataService.decrementDownloadCounter();
		}
	}
	
	
	@GetMapping(path = "/opendata/generate")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void generate(final HttpServletResponse response) throws IOException {
		openDataService.generateOpendata();
	}

}
