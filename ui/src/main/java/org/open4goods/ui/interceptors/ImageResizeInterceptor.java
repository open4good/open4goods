package org.open4goods.ui.interceptors;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
 * <p>
 * This interceptor intercepts HTTP requests for images in WebP format, checks
 * for cached versions, resizes images if needed, and caches the results. It
 * improves performance by serving cached images and reduces server load.
 * </p>
 *
 * <ul>
 * <li>Parses dimensions from the request URI.</li>
 * <li>Fetches source images in multiple formats (e.g., PNG, JPG, WebP).</li>
 * <li>Resizes images and caches them as WebP files.</li>
 * <li>Serves cached images with appropriate headers for client-side
 * caching.</li>
 * </ul>
 *
 * This class is a Spring {@link Component} and implements
 */
public class ImageResizeInterceptor implements HandlerInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(ImageResizeInterceptor.class);

	/**
	 * Base URL for fetching source images. Configured via application properties.
	 */
	private final String imageBaseUrl;

	/**
	 * Header used internally to prevent interceptor recursion when requesting
	 * original WebP files.
	 */
	static final String BYPASS_HEADER = "X-Bypass-Image-Resize";

	/** Regex pattern for parsing dimensions from the request URI. */
	private static final Pattern DIMENSION_PATTERN = Pattern.compile("^(?:.*-)?(\\d+)(?:x(\\d+))?\\.webp$");

	private final ResourceService resourceService;

	private final Set<String> allowedResize;

	/**
	 * Constructs an {@code ImageResizeInterceptor} with the specified resource
	 * service.
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
	 * <p>
	 * If the request is for a WebP image, it verifies whether the cached image
	 * exists. If not, it resizes and caches the image. The cached image is then
	 * served to the client.
	 * </p>
	 *
	 * @param request  the HTTP servlet request
	 * @param response the HTTP servlet response
	 * @param handler  the handler for the request
	 * @return {@code false} if the request is handled by this interceptor,
	 *         {@code true} otherwise
	 * @throws IOException if an error occurs during file or image processing
	 */
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
		String requestURI = request.getRequestURI();
		logger.debug("Intercepting request URI: {}", requestURI);

		if (request.getHeader(BYPASS_HEADER) != null) {
			logger.debug("Bypassing interceptor for URI: {} due to internal header", requestURI);
			return true;
		}

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
					if (!allowedResize.contains(requestURI.substring(requestURI.lastIndexOf("-") + 1, requestURI.lastIndexOf(".")))) {
						logger.error("Image resizing suffix invalid for : {}", requestURI);
						response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
						return false;
					}

					sourceImage = resizeImage(sourceImage, dimensions[0], dimensions[1]);
					logger.info("Image resized to {}x{}", dimensions[0], dimensions[1]);
				}

				// Save the resized image in WebP format to cache
				if (!ImageIO.write(sourceImage, "webp", cachedFile)) {
					logger.info("Could not write file: {}", cachedFile.getAbsolutePath());
				} else {
					logger.info("Cached image saved: {}", cachedFile.getAbsolutePath());
				}
			} else {
				logger.info("Cache hit for file: {}", cachedFile.getAbsolutePath());
			}

			// Serve the cached file
			try {
				serveImage(response, cachedFile);
			} catch (IOException e) {
				logger.error("Error serving cached image: {}", cachedFile.getAbsolutePath(), e);
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
				return false;
			}
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
	Resource buildResource(String requestURI) {
		Resource resource = new Resource();
		resource.setFileName(requestURI);
		// Suffix in cache key to allow mass deletions / handlings
		resource.setCacheKey(IdHelper.generateResourceId(requestURI) + ".cache.webp");
		return resource;
	}

	/**
	 * Parses dimensions (width and height) from the given file name.
	 *
	 * <p>
	 * Supported patterns:
	 * <ul>
	 * <li>{@code name-200x300.webp} → width=200, height=300</li>
	 * <li>{@code name-200.webp} → width=200, height defaults to width to maintain
	 * aspect ratio</li>
	 * </ul>
	 *
	 * @param fileName the file name to parse
	 * @return an array with width and height, or {@code null} if dimensions are not
	 *         specified or invalid
	 */
	int[] parseDimensions(String fileName) {
		Matcher matcher = DIMENSION_PATTERN.matcher(fileName);
		if (matcher.matches()) {
			try {
				int width = Integer.parseInt(matcher.group(1));
				int height = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : width; // Maintain aspect ratio
				return new int[] { width, height };
			} catch (NumberFormatException e) {
				logger.error("Invalid dimensions in filename: {}", fileName, e);
			}
		}
		return null;
	}

	/**
	 * Finds the source image by checking multiple file extensions.
	 *
	 * @param baseImageName         the base name of the image file
	 * @param hasDimensionsArgument
	 * @return a {@link BufferedImage} of the source image, or {@code null} if not
	 *         found
	 * @throws IOException if an error occurs during image fetching
	 */
	BufferedImage findSourceImage(String baseImageName, boolean hasDimensionsArgument) throws IOException {
		String baseName = baseImageName.substring(0, baseImageName.lastIndexOf("."));

		// If dimensions argument, trim to original
		if (hasDimensionsArgument) {
			baseName = baseName.substring(0, baseImageName.lastIndexOf("-"));
		}

		// intercepted)
		String[] extensions = { "png", "jpg", "jpeg", "webp" };
		for (String ext : extensions) {
			String url = imageBaseUrl + baseName + "." + ext;
			logger.info("Testing image url  {} for basename {} (hasDimensions:{})", url, baseName, hasDimensionsArgument);
			BufferedImage image = fetchImageFromURL(url);
			if (image != null) {
				logger.info("Found image url {} for basename {} (hasDimensions:{})", url, baseName, hasDimensionsArgument);
				return image;
			}
		}
		logger.warn("No base image found for basename {} (hasDimensions:{})", baseName, hasDimensionsArgument);

		return null;
	}

	/**
	 * Fetches an image from a URL.
	 *
	 * @param urlString the URL of the image
	 * @return a {@link BufferedImage} of the fetched image, or {@code null} if the
	 *         image could not be fetched
	 */
	BufferedImage fetchImageFromURL(String urlString) {
		HttpURLConnection connection = null;
		try {
			connection = openConnection(urlString);
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty(BYPASS_HEADER, BYPASS_HEADER);

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				logger.info("Successfully fetched image from URL: {}", urlString);
				try (var inputStream = connection.getInputStream()) {
					return ImageIO.read(inputStream);
				}
			}
			logger.info("Unexpected HTTP {} fetching image from {}", responseCode, urlString);
		} catch (IOException e) {
			logger.info("Failed to fetch image from  {} : {}", urlString, e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return null;
	}

	HttpURLConnection openConnection(String urlString) throws IOException {
		URL url = new URL(urlString);
		return (HttpURLConnection) url.openConnection();
	}

	/**
	 * Resizes an image to the specified dimensions, maintaining aspect ratio.
	 *
	 * @param originalImage the original {@link BufferedImage} to resize
	 * @param width         the target width
	 * @param height        the target height
	 * @return the resized {@code BufferedImage}
	 */
	BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
		int originalWidth = originalImage.getWidth();
		int originalHeight = originalImage.getHeight();
		double aspectRatio = (double) originalWidth / originalHeight;

		if ((double) width / height > aspectRatio) {
			width = (int) (height * aspectRatio);
		} else {
			height = (int) (width / aspectRatio);
		}

		BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = resizedImage.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.drawImage(originalImage, 0, 0, width, height, null);
		} finally {
			graphics.dispose();
		}
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
