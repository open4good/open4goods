package org.open4goods.ui.controllers.ui.pages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.service.OpenDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
public class OpenDataController {

	public static final String DEFAULT_PATH="/opendata";
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataController.class);

    private final OpenDataService openDataService;
    private final OpenDataConfig openDataConfig;

    public OpenDataController(OpenDataService openDataService, OpenDataConfig openDataConfig) {
        this.openDataService = openDataService;
        this.openDataConfig = openDataConfig;
    }


	@RequestMapping(path = "/opendata/gtin-open-data.zip", method = { RequestMethod.GET, RequestMethod.HEAD })
	public void downloadGtinData(final HttpServletRequest request, final HttpServletResponse response) throws IOException, TechnicalException {
        downloadData(request, response, "gtin-open-data.zip", openDataConfig.gtinZipFile());
	}

	@RequestMapping(path = "/opendata/isbn-open-data.zip", method = { RequestMethod.GET, RequestMethod.HEAD })
	public void downloadIsbnData(final HttpServletRequest request, final HttpServletResponse response) throws IOException, TechnicalException {
        downloadData(request, response, "isbn-open-data.zip", openDataConfig.isbnZipFile());
	}

	private void downloadData(final HttpServletRequest request, final HttpServletResponse response, String fileName, File zipFile) throws IOException, TechnicalException {
		String redirectBase = openDataConfig.getRedirectBaseUrl();
		if (StringUtils.isNotBlank(redirectBase)) {
			String base = redirectBase.endsWith("/") ? redirectBase.substring(0, redirectBase.length() - 1) : redirectBase;
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", base + "/" + fileName);
			return;
		}
		if (HttpMethod.HEAD.matches(request.getMethod())) {
			// A HEAD never starts the throttled stream nor counts as a download.
			response.setHeader("Content-type", "application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
			response.setContentLengthLong(zipFile.length());
			return;
		}
		try (InputStream str = openDataService.limitedRateStream(zipFile.getAbsolutePath())) {
			response.setHeader("Content-type", "application/octet-stream");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
			IOUtils.copy(str, response.getOutputStream());
		} catch (IOException e) {
			if (isClientAbort(e)) {
				LOGGER.info("opendata file download interrupted by client: {}", e.getMessage());
			} else {
				LOGGER.error("opendata file download error : {}", e.getMessage(), e);
			}
			openDataService.decrementDownloadCounter();
		}
	}

	/**
	 * Detects common servlet container messages for clients closing the download
	 * before the ZIP stream is fully written.
	 *
	 * @param exception download exception
	 * @return {@code true} when the failure is a client abort
	 */
	private boolean isClientAbort(IOException exception) {
		String message = exception.getMessage();
		return message != null && (message.contains("Broken pipe") || message.contains("ClientAbortException")
				|| message.contains("Connection reset by peer"));
	}

}
