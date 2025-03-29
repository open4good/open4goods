package org.open4goods.ui.services;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.open4goods.api.services.feed.FeedService;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.favicon.dto.FaviconResponse;
import org.open4goods.services.favicon.service.FaviconService;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
// TODO(p2,design) : use spring cache

public class DatasourceImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceImageService.class);
    private static final String CONTENT_TYPE_IMAGE_PNG = "image/png";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";

    private final DataSourceConfigService datasourceConfigService;
    private final FeedService feedService;
    private final RemoteFileCachingService remoteFileCachingService;
    private final FaviconService faviconService;

    private final Map<String, byte[]> iconCache = new ConcurrentHashMap<>();
    private final Map<String, String> iconContentTypeCache = new ConcurrentHashMap<>();

    public DatasourceImageService(DataSourceConfigService datasourceConfigService,
                                  FeedService feedService,
                                  RemoteFileCachingService remoteFileCachingService,
                                  FaviconService faviconService) {
        this.datasourceConfigService = datasourceConfigService;
        this.feedService = feedService;
        this.remoteFileCachingService = remoteFileCachingService;
        this.faviconService = faviconService;
    }

    public void serveDatasourceImage(String dsName, boolean allowFaviconFallback, HttpServletResponse response) {
       	String datasourceName = IdHelper.azCharAndDigitsPointsDash(dsName.toLowerCase());
        
        try {
            byte[] cachedData = iconCache.get(datasourceName);
            String cachedContentType = iconContentTypeCache.get(datasourceName);

            if (cachedData != null && cachedContentType != null) {
                writeToResponse(cachedData, cachedContentType, response);
                return;
            }

            DataSourceProperties ds = datasourceConfigService.getDatasourceConfig(datasourceName);
            String imageUrl = (ds != null) ? (allowFaviconFallback ? ds.getFavico() : ds.getLogo()) : null;

            if (imageUrl == null) {
                ds = feedService.getFeedsUrl().stream()
                		 .filter(e -> datasourceName.equalsIgnoreCase(cleanName(e.getDatasourceConfigName())) || datasourceName.equalsIgnoreCase(cleanName(e.getName())))
                        .findAny().orElse(null);

                if (ds != null) {
                    imageUrl = allowFaviconFallback ? ds.getFavico() : ds.getLogo();
                }
            }

            if (allowFaviconFallback && (imageUrl == null || imageUrl.isBlank()) && ds != null && ds.getPortalUrl() != null) {
                FaviconResponse faviconResponse = faviconService.getFavicon(ds.getPortalUrl());
                if (faviconResponse != null && faviconResponse.faviconData() != null) {
                    cache(datasourceName, faviconResponse.faviconData(), faviconResponse.contentType());
                    writeToResponse(faviconResponse.faviconData(), faviconResponse.contentType(), response);
                    return;
                }
            }

            if (imageUrl != null && !imageUrl.isBlank()) {
                File file = remoteFileCachingService.getResource(imageUrl, 1);
                byte[] bytes = FileUtils.readFileToByteArray(file);
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) contentType = CONTENT_TYPE_IMAGE_PNG;
                cache(datasourceName, bytes, contentType);
                writeToResponse(bytes, contentType, response);
                return;
            }

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found for datasource: " + datasourceName);

        } catch (InvalidParameterException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameter: " + datasourceName, e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing image for datasource: " + datasourceName, e);
        }
    }

    public String cleanName(String datasourceName) {
        return IdHelper.azCharAndDigitsPointsDash(datasourceName.toLowerCase());
    }

    public boolean hasLogo(String datasourceName) {
        return hasImage(cleanName(datasourceName.toLowerCase()), false);
    }

    public boolean hasIcon(String datasourceName) {
        return hasImage(cleanName(datasourceName.toLowerCase()), true);
    }

    private boolean hasImage(String datasourceName, boolean allowFaviconFallback) {
        try {
            DataSourceProperties ds = datasourceConfigService.getDatasourceConfig(datasourceName);
            String imageUrl = (ds != null) ? (allowFaviconFallback ? ds.getFavico() : ds.getLogo()) : null;

            if (imageUrl == null) {
                ds = feedService.getFeedsUrl().stream()
                        .filter(e -> datasourceName.equalsIgnoreCase(cleanName(e.getDatasourceConfigName())) || datasourceName.equalsIgnoreCase(cleanName(e.getName())))
                        .findFirst().orElse(null);

                if (ds != null) {
                    imageUrl = allowFaviconFallback ? ds.getFavico() : ds.getLogo();
                }
            }

            if (allowFaviconFallback && (imageUrl == null || imageUrl.isBlank()) && ds != null && ds.getPortalUrl() != null) {
                FaviconResponse response = faviconService.getFavicon(ds.getPortalUrl());
                return response != null && response.faviconData() != null && response.faviconData().length > 0;
            }

            if (imageUrl != null && !imageUrl.isBlank()) {
                File file = remoteFileCachingService.getResource(imageUrl, 1);
                return file.exists() && file.length() > 0;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeToResponse(byte[] bytes, String contentType, HttpServletResponse response) throws IOException {
        response.setContentType(contentType != null ? contentType : CONTENT_TYPE_IMAGE_PNG);
        response.setHeader(HEADER_CACHE_CONTROL, "public, max-age=86400");
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    private void cache(String datasourceName, byte[] data, String contentType) {
        iconCache.put(datasourceName, data);
        iconContentTypeCache.put(datasourceName, contentType);
    }
}
