package org.open4goods.ui.controllers.ui.pages;

import org.open4goods.ui.config.properties.OpenDataRedirectProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Legacy download URLs of the open data files ({@code /opendata/gtin-open-data.zip},
 * {@code /opendata/isbn-open-data.zip}).
 * <p>
 * ui neither generates nor serves an open data file (GOU-2290): the api publishes them on R2, served by
 * {@code data.nudger.fr} with resumable downloads. GET and HEAD answer a permanent redirect (301) to
 * {@code {open-data-config.redirect-base-url}/{name}}. Without a configured base URL (beta, local), they answer 404:
 * ui has no file to serve.
 * </p>
 */
@Controller
public class OpenDataController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataController.class);

    static final String GTIN_FILENAME = "gtin-open-data.zip";
    static final String ISBN_FILENAME = "isbn-open-data.zip";

    private final OpenDataRedirectProperties redirectProperties;

    public OpenDataController(OpenDataRedirectProperties redirectProperties) {
        this.redirectProperties = redirectProperties;
    }

    /**
     * @return 301 to the GTIN zip on the public base URL, 404 when none is configured
     */
    @RequestMapping(path = "/opendata/" + GTIN_FILENAME, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<Void> downloadGtinData() {
        return redirect(GTIN_FILENAME);
    }

    /**
     * @return 301 to the ISBN zip on the public base URL, 404 when none is configured
     */
    @RequestMapping(path = "/opendata/" + ISBN_FILENAME, method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<Void> downloadIsbnData() {
        return redirect(ISBN_FILENAME);
    }

    /**
     * @param fileName public object name of the file
     * @return the permanent redirect, or 404 without a configured base URL
     */
    private ResponseEntity<Void> redirect(String fileName) {
        String location = redirectProperties.redirectUrl(fileName);
        if (location == null) {
            LOGGER.debug("No open-data-config.redirect-base-url: {} is not served by ui", fileName);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, location).build();
    }
}
