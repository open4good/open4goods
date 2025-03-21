package org.open4goods.services.favicon.controller;

import org.open4goods.services.favicon.dto.FaviconResponse;
import org.open4goods.services.favicon.service.FaviconService;
import org.open4goods.services.favicon.exception.FaviconException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for favicon retrieval endpoints.
 */
@RestController
@RequestMapping("/favicon")
public class FaviconController {

    private static final Logger logger = LoggerFactory.getLogger(FaviconController.class);

    private final FaviconService faviconService;

    public FaviconController(FaviconService faviconService) {
        this.faviconService = faviconService;
    }

    /**
     * Endpoint to check if a favicon exists for a given URL.
     *
     * @param url the URL to check.
     * @return true if a favicon exists, false otherwise.
     */
    @GetMapping("/exists")
    public ResponseEntity<Boolean> hasFavicon(@RequestParam("url") String url) {
        boolean exists = faviconService.hasFavicon(url);
        return ResponseEntity.ok(exists);
    }

    /**
     * Endpoint to retrieve the favicon for a given URL.
     *
     * @param url the URL for which to retrieve the favicon.
     * @return the favicon image as a byte array with the detected content type.
     */
    @GetMapping
    public ResponseEntity<byte[]> getFavicon(@RequestParam("url") String url) {
        try {
            FaviconResponse response = faviconService.getFavicon(url);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, response.contentType());
            return ResponseEntity.ok().headers(headers).body(response.faviconData());
        } catch (FaviconException e) {
            logger.error("Error retrieving favicon: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Endpoint to clear the favicon cache.
     *
     * @return a success message.
     */
    @DeleteMapping("/cache")
    public ResponseEntity<String> clearCache() {
        faviconService.clearCache();
        return ResponseEntity.ok("Favicon cache cleared.");
    }
}
