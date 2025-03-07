package org.open4goods.ui.controllers.ui;

import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.services.UrlCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller to trigger reading a sitemap and checking URLs for health.
 */
@RestController
@RequestMapping("/urlcheck")
@PreAuthorize("hasAuthority('" + RolesConstants.ROLE_ADMIN + "')")
public class UrlCheckController {

    private final UrlCheckService urlCheckService;
    @Autowired UiConfig uiConfig;

    public UrlCheckController(UrlCheckService urlCheckService) {
        this.urlCheckService = urlCheckService;
    }

    /**
     * Endpoint to read a sitemap from the given URL and store newly found URLs in Elasticsearch.
     * @param sitemapUrl the sitemap (or sitemap index) URL
     * @return a short message
     */
    @GetMapping("/read-sitemap")
    public String readSitemap() {
        try {
            urlCheckService.readSitemapAndStore(uiConfig.getUrlcheck().getSitemapUrl());
            return "Sitemap read successfully";
        } catch (Exception e) {
            return "Error reading sitemap: " + e.getMessage();
        }
    }

    /**
     * Endpoint to perform a check of all stored URLs.
     * @return a short status message
     */
    @GetMapping("/check-all")
    public String checkAllUrls() {
        urlCheckService.checkAllUrls();
        // Return summary from counters
        return String.format("Check completed. \n" +
                "Total tested: %d\n" +
                "HTTP 500: %d\n" +
                "Bad patterns: %d\n" +
                "Redirects (30x): %d\n" +
                "Other statuses: %d\n",
                urlCheckService.getTotalUrlsTested(),
                urlCheckService.getTotal500Errors(),
                urlCheckService.getTotalBadPatternHits(),
                urlCheckService.getTotalRedirects(),
                urlCheckService.getTotalOtherStatus()
        );
    }
}
