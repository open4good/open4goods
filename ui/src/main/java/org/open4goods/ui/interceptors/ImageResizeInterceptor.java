package org.open4goods.ui.interceptors;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that handles on-the-fly resizing and caching of image requests.
 *
 * <p>This interceptor intercepts HTTP requests for images in WebP format,
 * checks for cached versions, resizes images if needed, and caches the results.
 * It improves performance by serving cached images and reduces server load.</p>
 *
 * <ul>
 *   <li>Parses dimensions from the request URI.</li>
 *   <li>Fetches source images in multiple formats (e.g., PNG, JPG, WebP).</li>
 *   <li>Resizes images and caches them as WebP files.</li>
 *   <li>Serves cached images with appropriate headers for client-side caching.</li>
 * </ul>
 *
 * This class is a Spring {@link Component} and implements {@link HandlerInterceptor}.
 * TODO(p2, perf) : perf : add a in memory cache for commons url patterns (/assets, ...)
 */
public class ImageResizeInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ImageResizeInterceptor.class);

    /** Base URL for fetching source images. Configured via application properties. */
    private final String imageBaseUrl;

    /** Regex pattern for parsing dimensions from the request URI. */
    private static final Pattern DIMENSION_PATTERN = Pattern.compile(
            "^(?:.*-)?(\\d+)(?:x(\\d+))?\\.webp$"
    );

    private final ResourceService resourceService;

	private Set<String> allowedResize;

    /**
     * Constructs an {@code ImageResizeInterceptor} with the specified resource service.
     *
     * @param resourceService the service for managing cached resources
     */
    public ImageResizeInterceptor(ResourceService resourceService, Set<String> allowedResize, String imageBaseUrl) {
        this.resourceService = resourceService;
        this.allowedResize = allowedResize;
        this.imageBaseUrl = imageBaseUrl;
    }

    /**
     * Intercepts HTTP requests to check for image resizing and caching logic.
     *
     * <p>If the request is for a WebP image, it verifies whether the cached image exists.
     * If not, it resizes and caches the image. The cached image is then served to the client.</p>
     *
     * @param request  the HTTP servlet request
     * @param response the HTTP servlet response
     * @param handler  the handler for the request
     * @return {@code false} if the request is handled by this interceptor, {@code true} otherwise
     * @throws IOException if an error occurs during file or image processing
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String requestURI = request.getRequestURI();
        logger.debug("Intercepting request URI: {}", requestURI);

        if (requestURI.endsWith(".webp")) {
            Resource resource = buildResource(requestURI);

            File cachedFile = resourceService.getCacheFile(resource);
            if (!cachedFile.exists()) {
            	logger.info("Cache miss for file: {}", requestURI);

                int[] dimensions = parseDimensions(requestURI);


                BufferedImage sourceImage = findSourceImage(requestURI, dimensions != null);

                if (sourceImage == null) {
                    logger.warn("Source image not found for URI: {}", requestURI);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                    return false;
                }

                // Resize the image if dimensions are provided
                if (dimensions != null) {

                	// Wheck this is a valid requested size
                	if (!allowedResize.contains(requestURI.substring(requestURI.lastIndexOf("-")+1,requestURI.lastIndexOf(".") ))) {
                		logger.error("Image resizing suffix invalid for : {}",requestURI );
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
                        return false;
                	}

                	sourceImage = resizeImage(sourceImage, dimensions[0], dimensions[1]);
                    logger.info("Image resized to {}x{}", dimensions[0], dimensions[1]);
                }

                // Save the resized image in WebP format to cache
               if (! ImageIO.write(sourceImage, "webp", cachedFile)) {
            	   logger.info("Could not write file: {}", cachedFile.getAbsolutePath());
               } else {
            	   logger.info("Cached image saved: {}", cachedFile.getAbsolutePath());
               }
            } else {
                logger.info("Cache hit for file: {}", cachedFile.getAbsolutePath());
            }

            // Serve the cached file
            serveImage(response, cachedFile);
            return false; // Prevent further request processing
        }

        return true;
    }

    /**
     * Builds a {@link Resource} object for the given request URI.
     *
     * @param requestURI the URI of the image request
     * @return a {@code Resource} representing the requested image
     */
    private Resource buildResource(String requestURI) {
        Resource resource = new Resource();
        resource.setFileName(requestURI);
        // Suffix in cache key to allow mass deletions / handlings
        resource.setCacheKey(IdHelper.generateResourceId(requestURI)+".cache.webp");
        return resource;
    }

    /**
     * Parses dimensions (width and height) from the given file name.
     *
     * @param fileName the file name to parse
     * @param dimensions
     * @return an array with width and height, or {@code null} if dimensions are not specified
     */
    private int[] parseDimensions(String fileName) {
        Matcher matcher = DIMENSION_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            try {
                int width = Integer.parseInt(matcher.group(1));
                int height = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : width; // Maintain aspect ratio
                return new int[]{width, height};
            } catch (NumberFormatException e) {
                logger.error("Invalid dimensions in filename: {}", fileName, e);
            }
        }
        return null;
    }

    /**
     * Finds the source image by checking multiple file extensions.
     *
     * @param baseImageName the base name of the image file
     * @param hasDimensionsArgument
     * @return a {@link BufferedImage} of the source image, or {@code null} if not found
     * @throws IOException if an error occurs during image fetching
     */
    private BufferedImage findSourceImage(String baseImageName, boolean hasDimensionsArgument) throws IOException {
        String baseName = baseImageName.substring(0, baseImageName.lastIndexOf("."));

        // If dimensions argument, trim to original
        if (hasDimensionsArgument) {
        	baseName = baseName.substring(0, baseImageName.lastIndexOf("-"));
        }

        // TODO(p1,feature) : Enable original webp handling, (infinite loop cause re intercepted)
        String[] extensions = { "png", "jpg", "jpeg"};
        for (String ext : extensions) {
            String url = imageBaseUrl + baseName + "." + ext;
            logger.info("Testing image url  {} for basename {} (hasDimensions:{})",url,baseName, hasDimensionsArgument);
            BufferedImage image = fetchImageFromURL(url);
            if (image != null) {
                logger.info("Found image url {} for basename {} (hasDimensions:{})",url,baseName, hasDimensionsArgument);
            	return image;
            }
        }
        logger.warn("No base image found for basename {} (hasDimensions:{})",baseName, hasDimensionsArgument);

        return null;
    }

    /**
     * Fetches an image from a URL.
     *
     * @param urlString the URL of the image
     * @return a {@link BufferedImage} of the fetched image, or {@code null} if the image could not be fetched
     */
    private BufferedImage fetchImageFromURL(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                logger.info("Successfully fetched image from URL: {}", urlString);
                return ImageIO.read(connection.getInputStream());
            }
        } catch (IOException e) {
            logger.info("Failed to fetch image from  {} : {}", urlString, e.getMessage());
        }
        return null;
    }

    /**
     * Resizes an image to the specified dimensions, maintaining aspect ratio.
     *
     * @param originalImage the original {@link BufferedImage} to resize
     * @param width the target width
     * @param height the target height
     * @return the resized {@code BufferedImage}
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double aspectRatio = (double) originalWidth / originalHeight;

        if ((double) width / height > aspectRatio) {
            width = (int) (height * aspectRatio);
        } else {
            height = (int) (width / aspectRatio);
        }

        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        resizedImage.getGraphics().drawImage(originalImage.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return resizedImage;
    }

    /**
     * Serves a cached image file to the client.
     *
     * @param response   the HTTP servlet response
     * @param cachedFile the cached image file to serve
     * @throws IOException if an error occurs during file transfer
     */
    private void serveImage(HttpServletResponse response, File cachedFile) throws IOException {
        response.setContentType("image/webp");
        response.setHeader("Cache-Control", "public, max-age=86400");
        FileUtils.copyFile(cachedFile, response.getOutputStream());
        logger.info("Served cached image: {}", cachedFile.getAbsolutePath());
    }
}
