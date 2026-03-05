package org.open4goods.nudgerfrontapi.controller.api;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.nudgerfrontapi.service.AffiliationPartnerService;
import org.open4goods.services.favicon.dto.FaviconResponse;
import org.open4goods.services.favicon.service.FaviconService;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing partner and datasource assets through front-api.
 */
@RestController
@RequestMapping
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
public class ResourceAssetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceAssetController.class);
    private static final MediaType DEFAULT_MEDIA_TYPE = MediaType.IMAGE_PNG;

    private final AffiliationPartnerService affiliationPartnerService;
    private final RemoteFileCachingService remoteFileCachingService;
    private final FaviconService faviconService;

    /**
     * Build the resource controller.
     *
     * @param affiliationPartnerService partner cache service
     * @param remoteFileCachingService remote file cache used for partner logos
     * @param faviconService favicon lookup service
     */
    public ResourceAssetController(AffiliationPartnerService affiliationPartnerService,
            RemoteFileCachingService remoteFileCachingService,
            FaviconService faviconService) {
        this.affiliationPartnerService = affiliationPartnerService;
        this.remoteFileCachingService = remoteFileCachingService;
        this.faviconService = faviconService;
    }

    /**
     * Return the configured partner logo.
     *
     * @param partnerName encoded partner name
     * @return logo content or 404 when unavailable
     */
    @GetMapping("/logo/{partnerName}")
    public ResponseEntity<byte[]> logo(@PathVariable String partnerName) {
        String decodedName = URLDecoder.decode(partnerName, StandardCharsets.UTF_8);
        Optional<AffiliationPartner> partner = affiliationPartnerService.getPartners().stream()
                .filter(candidate -> decodedName.equalsIgnoreCase(candidate.getName()))
                .findFirst();

        if (partner.isEmpty() || partner.get().getLogoUrl() == null || partner.get().getLogoUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        return fetchImage(partner.get().getLogoUrl());
    }

    /**
     * Return a datasource icon, aliasing partner logos.
     *
     * @param datasourceName datasource identifier
     * @return icon content or 404 when unavailable
     */
    @GetMapping("/icon/{datasourceName}")
    public ResponseEntity<byte[]> icon(@PathVariable String datasourceName) {
        return logo(datasourceName);
    }

    /**
     * Return the favicon for a provided portal URL.
     *
     * @param url source URL used to derive the favicon
     * @return favicon bytes or 404 when unavailable
     */
    @GetMapping("/favicon")
    public ResponseEntity<byte[]> favicon(@RequestParam("url") String url) {
        FaviconResponse faviconResponse = faviconService.getFavicon(url);
        if (faviconResponse == null || faviconResponse.faviconData() == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = parseMediaType(faviconResponse.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(faviconResponse.faviconData());
    }

    private ResponseEntity<byte[]> fetchImage(String imageUrl) {
        try {
            File file = remoteFileCachingService.getResource(imageUrl, 1);
            byte[] content = Files.readAllBytes(file.toPath());
            MediaType mediaType = parseMediaType(Files.probeContentType(file.toPath()));
            return ResponseEntity.ok().contentType(mediaType).body(content);
        }
        catch (Exception exception) {
            LOGGER.warn("Unable to load remote asset from {}: {}", imageUrl, exception.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private MediaType parseMediaType(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_MEDIA_TYPE;
        }
        try {
            return MediaType.parseMediaType(value);
        }
        catch (Exception exception) {
            return DEFAULT_MEDIA_TYPE;
        }
    }
}
