package org.open4goods.ui.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.favicon.dto.FaviconResponse;
import org.open4goods.services.favicon.service.FaviconService;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service responsible for retrieving logo or icon image content for data sources.
 */
@Service
public class DatasourceImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceImageService.class);
    private static final String CONTENT_TYPE_IMAGE_PNG = "image/png";

    private final DataSourceConfigService datasourceConfigService;
    private final FeedService feedService;
    private final RemoteFileCachingService remoteFileCachingService;
    private final FaviconService faviconService;

    public DatasourceImageService(DataSourceConfigService datasourceConfigService,
                                  FeedService feedService,
                                  RemoteFileCachingService remoteFileCachingService,
                                  FaviconService faviconService) {
        this.datasourceConfigService = datasourceConfigService;
        this.feedService = feedService;
        this.remoteFileCachingService = remoteFileCachingService;
        this.faviconService = faviconService;
    }

    /**
     * Returns the image (logo or icon or favicon) content for the given datasource name.
     *
     * @param dsName                name of the datasource
     * @param allowFaviconFallback whether to fallback to favicon lookup
     * @return ImageResult containing bytes and content type
     */
    public ImageResult getDatasourceImage(String dsName, boolean allowFaviconFallback) {
        String datasourceName = IdHelper.toDatasourceId(dsName);

        try {
            ImageResult result = loadDatasourceImage(datasourceName, allowFaviconFallback);

            if (result == null || result.data == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found for datasource: " + datasourceName);
            }

            return result;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing image for datasource: " + datasourceName, e);
        } catch (InvalidParameterException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid parameter " + datasourceName, e);
		}
    }

    public String cleanName(String datasourceName) {
        return IdHelper.toDatasourceId(datasourceName);
    }

    public boolean hasLogo(String datasourceName) {
        return hasImage(cleanName(datasourceName), false);
    }

    public boolean hasIcon(String datasourceName) {
        return hasImage(cleanName(datasourceName), true);
    }

    /**
     * Tries to load the image to check if it exists.
     */
    private boolean hasImage(String datasourceName, boolean allowFaviconFallback) {
        try {
            ImageResult result = loadDatasourceImage(datasourceName, allowFaviconFallback);
            return result != null && result.data != null && result.data.length > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Cached method that fetches the image content from logo, icon or favicon.
     * @throws InvalidParameterException
     */
    @Cacheable(value = CacheConstants.FOREVER_LOCAL_CACHE_NAME, key = "#datasourceName + '-' + #allowFaviconFallback")
    public ImageResult loadDatasourceImage(String datasourceName, boolean allowFaviconFallback) throws IOException, InvalidParameterException {
        DataSourceProperties ds = datasourceConfigService.getDatasourceConfig(datasourceName);
        String imageUrl = (ds != null) ? ds.getLogo() : null;

        if (imageUrl == null) {
            AffiliationPartner partner = feedService.getPartners().stream()
                    .filter(e -> datasourceName.equalsIgnoreCase(cleanName(e.getName())) || datasourceName.equalsIgnoreCase(cleanName(e.getName())))
                    .findAny().orElse(null);

            if (partner != null) {
            	imageUrl = partner.getLogoUrl();
            }
            if (imageUrl == null && allowFaviconFallback && ds != null) {

            	imageUrl = ds.getFavico();
            }
        }

        if (allowFaviconFallback && (imageUrl == null || imageUrl.isBlank()) && ds != null && ds.getPortalUrl() != null) {
            FaviconResponse faviconResponse = faviconService.getFavicon(ds.getPortalUrl());
            if (faviconResponse != null && faviconResponse.faviconData() != null) {
                return new ImageResult(faviconResponse.faviconData(), faviconResponse.contentType());
            }
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            File file = remoteFileCachingService.getResource(imageUrl, 1);
            byte[] bytes = FileUtils.readFileToByteArray(file);
            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) contentType = CONTENT_TYPE_IMAGE_PNG;
            return new ImageResult(bytes, contentType);
        }

        return null;
    }

    /**
     * Simple wrapper for binary image data and its content type.
     */
    public record ImageResult(byte[] data, String contentType) {
    }
}
