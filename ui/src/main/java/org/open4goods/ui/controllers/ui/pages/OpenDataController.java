package org.open4goods.ui.controllers.ui.pages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.service.OpenDataService;
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
public class OpenDataController {

	public static final String DEFAULT_PATH="/opendata";
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataController.class);

    private final OpenDataService openDataService;
    private final OpenDataConfig openDataConfig;

    public OpenDataController(OpenDataService openDataService, OpenDataConfig openDataConfig) {
        this.openDataService = openDataService;
        this.openDataConfig = openDataConfig;
    }


	@GetMapping(path = "/opendata/gtin-open-data.zip")
	public void downloadGtinData(final HttpServletResponse response) throws IOException, TechnicalException {
        downloadData(response, "gtin-open-data.zip", openDataConfig.gtinZipFile());
	}

	@GetMapping(path = "/opendata/isbn-open-data.zip")
	public void downloadIsbnData(final HttpServletResponse response) throws IOException, TechnicalException {
        downloadData(response, "isbn-open-data.zip", openDataConfig.isbnZipFile());
	}

	private void downloadData(final HttpServletResponse response, String fileName, File zipFile) throws IOException, TechnicalException {
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

	@GetMapping(path = "/opendata/generate")
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
	public void generate(final HttpServletResponse response) throws IOException {
		openDataService.generateOpendata();
	}

}
