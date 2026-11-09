package org.open4goods.ui.interceptors;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FileUtils;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.luciad.imageio.webp.WebPReadParam;

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
 * <p>
 * Memory is bounded on a cache miss (GOU-1952): a source image is decoded at
 * most {@value #MAX_DECODED_PIXELS} pixels (larger sources are subsampled while
 * decoding, so their full raster is never allocated), and at most
 * {@value #MAX_CONCURRENT_CONVERSIONS} conversions run at the same time. A
 * request that waits more than {@value #CONVERSION_WAIT_SECONDS} seconds for a
 * conversion slot gets a 503.
 * </p>
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

	/**
	 * Largest number of pixels decoded from a source image: 4096 x 4096, i.e.
	 * 64 MB per ARGB copy. A full-size decode of a ~9000 x 9000 product image
	 * took ~333 MB per copy, and a few concurrent cache misses exhausted the 2 GB
	 * heap of {@code ui} (OOM of 2026-11-06, GOU-1952).
	 */
	static final long MAX_DECODED_PIXELS = 4096L * 4096L;

	/** Largest width or height the WebP encoder accepts. */
	static final int WEBP_MAX_DIMENSION = 16383;

	/** Cache misses decoded and encoded at the same time. */
	static final int MAX_CONCURRENT_CONVERSIONS = 2;

	/** Time a cache miss waits for a conversion slot before a 503. */
	static final long CONVERSION_WAIT_SECONDS = 30;

	private final ResourceService resourceService;

	private final Set<String> allowedResize;

	private final long maxDecodedPixels;

	private final Semaphore conversions;

	private final Duration conversionWait;

	/**
	 * Constructs an {@code ImageResizeInterceptor} with the default memory
	 * bounds ({@value #MAX_DECODED_PIXELS} decoded pixels,
	 * {@value #MAX_CONCURRENT_CONVERSIONS} concurrent conversions).
	 *
	 * @param resourceService the service for managing cached resources
	 * @param allowedResize   allowed size suffixes
	 * @param imageBaseUrl    base URL of the source images
	 */
	public ImageResizeInterceptor(ResourceService resourceService, Set<String> allowedResize, String imageBaseUrl) {
		this(resourceService, allowedResize, imageBaseUrl, MAX_DECODED_PIXELS, MAX_CONCURRENT_CONVERSIONS,
				Duration.ofSeconds(CONVERSION_WAIT_SECONDS));
	}

	/**
	 * Constructs an {@code ImageResizeInterceptor} with explicit memory bounds.
	 *
	 * @param resourceService          the service for managing cached resources
	 * @param allowedResize            allowed size suffixes
	 * @param imageBaseUrl             base URL of the source images
	 * @param maxDecodedPixels         largest number of pixels decoded from a source image
	 * @param maxConcurrentConversions cache misses converted at the same time
	 * @param conversionWait           time a cache miss waits for a conversion slot
	 */
	ImageResizeInterceptor(ResourceService resourceService, Set<String> allowedResize, String imageBaseUrl,
			long maxDecodedPixels, int maxConcurrentConversions, Duration conversionWait) {
		this.resourceService = resourceService;
		this.allowedResize = allowedResize;
		this.imageBaseUrl = imageBaseUrl;
		this.maxDecodedPixels = maxDecodedPixels;
		this.conversions = new Semaphore(maxConcurrentConversions, true);
		this.conversionWait = conversionWait;
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
				if (!generateWithinConversionSlot(requestURI, cachedFile, response)) {
					return false;
				}
			} else {
				logger.info("Cache hit for file: {}", cachedFile.getAbsolutePath());
			}

			// Serve the cached file
			try {
				serveImage(response, cachedFile);
			} catch (IOException e) {
				logger.error("Error serving cached image: {}", cachedFile.getAbsolutePath(), e);
				response.resetBuffer();
				response.setContentType("application/json");
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
				return false;
			}
			return false; // Prevent further request processing
		}

		return true;
	}

	/**
	 * Generates the cached image while holding one of the conversion slots, so
	 * that only a bounded number of decoded images live in memory at once.
	 *
	 * @param requestURI the requested image URI
	 * @param cachedFile the cache file to generate
	 * @param response   the HTTP response, used for error statuses
	 * @return {@code true} when the cached file is ready to serve, {@code false}
	 *         when an error status has been sent
	 * @throws IOException if an error occurs while sending an error status
	 */
	private boolean generateWithinConversionSlot(String requestURI, File cachedFile, HttpServletResponse response)
			throws IOException {
		boolean acquired;
		try {
			acquired = conversions.tryAcquire(conversionWait.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			acquired = false;
		}
		if (!acquired) {
			logger.warn("No image conversion slot within {}, refusing {}", conversionWait, requestURI);
			response.setHeader("Retry-After", String.valueOf(conversionWait.toSeconds()));
			response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Image conversion busy");
			return false;
		}
		try {
			// Another request may have generated the same file while this one waited
			if (cachedFile.exists()) {
				logger.info("Cache filled while waiting for file: {}", cachedFile.getAbsolutePath());
				return true;
			}
			return generateCachedImage(requestURI, cachedFile, response);
		} finally {
			conversions.release();
		}
	}

	/**
	 * Fetches, optionally resizes, and caches the source image of a request.
	 *
	 * @param requestURI the requested image URI
	 * @param cachedFile the cache file to generate
	 * @param response   the HTTP response, used for error statuses
	 * @return {@code true} when the cached file was written, {@code false} when
	 *         an error status has been sent
	 * @throws IOException if an error occurs during image fetching
	 */
	private boolean generateCachedImage(String requestURI, File cachedFile, HttpServletResponse response)
			throws IOException {
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

		if (!saveToCache(sourceImage, cachedFile)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found");
			return false;
		}
		return true;
	}

	/**
	 * Saves the source image in cache as WebP and validates the generated file.
	 *
	 * @param sourceImage the source image to store
	 * @param cachedFile  the cache file destination
	 * @return {@code true} if the cache file was successfully generated and is
	 *         readable, {@code false} otherwise
	 */
	boolean saveToCache(BufferedImage sourceImage, File cachedFile) {
		File parent = cachedFile.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			logger.error("Could not create cache folder for file: {}", cachedFile.getAbsolutePath());
			return false;
		}

		try {
			// The WebP writer refuses grayscale and 16 bits per channel images: redraw those in ARGB
			if (!ImageIO.write(sourceImage, "webp", cachedFile)
					&& !ImageIO.write(toArgb(sourceImage), "webp", cachedFile)) {
				logger.error("Could not write file: {}", cachedFile.getAbsolutePath());
				return false;
			}
		} catch (IOException e) {
			logger.error("Could not write file: {}", cachedFile.getAbsolutePath(), e);
			return false;
		}

		if (!cachedFile.exists() || cachedFile.length() <= 0 || !cachedFile.isFile() || !cachedFile.canRead()) {
			logger.error("Generated cache file is invalid: {}", cachedFile.getAbsolutePath());
			return false;
		}

		logger.info("Cached image saved: {}", cachedFile.getAbsolutePath());
		return true;
	}

	/**
	 * Redraws an image into a {@link BufferedImage#TYPE_INT_ARGB} copy.
	 *
	 * <p>
	 * The {@code webp-imageio} writer returns {@code false} for grayscale or 16 bits per
	 * channel sources (e.g. a 16-bit RGB PNG or a grayscale JPEG), which made those
	 * unresized originals answer 404 (GOU-1155). It accepts this copy.
	 * </p>
	 *
	 * @param image the decoded source image
	 * @return an ARGB copy of {@code image}
	 */
	static BufferedImage toArgb(BufferedImage image) {
		BufferedImage converted = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = converted.createGraphics();
		try {
			graphics.drawImage(image, 0, 0, null);
		} finally {
			graphics.dispose();
		}
		return converted;
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
					return decodeBounded(inputStream);
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

	/**
	 * Decodes an image without allocating more than the configured number of
	 * pixels.
	 *
	 * <p>
	 * The dimensions are read from the image header first. When the image is
	 * larger than the bound (or than the WebP limit of
	 * {@value #WEBP_MAX_DIMENSION} pixels per side), it is subsampled while
	 * decoding, so the full-size raster is never allocated. An unresized
	 * original larger than the bound is therefore cached at a lower resolution.
	 * </p>
	 *
	 * @param inputStream the encoded image, left open
	 * @return the decoded image, or {@code null} if no reader supports the format
	 * @throws IOException if the image cannot be decoded
	 */
	BufferedImage decodeBounded(InputStream inputStream) throws IOException {
		try (ImageInputStream imageInput = ImageIO.createImageInputStream(inputStream)) {
			if (imageInput == null) {
				return null;
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
			if (!readers.hasNext()) {
				return null;
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(imageInput, true, true);
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				int step = subsamplingStep(width, height, maxDecodedPixels);
				ImageReadParam param = reader.getDefaultReadParam();
				if (step > 1) {
					logger.warn("Source image of {}x{} pixels decoded at 1/{} to stay under {} pixels", width, height,
							step, maxDecodedPixels);
					if (param instanceof WebPReadParam webpParam) {
						// The WebP reader ignores source subsampling, it scales natively
						webpParam.setUseScaling(true);
						webpParam.setScaledWidth(Math.ceilDiv(width, step));
						webpParam.setScaledHeight(Math.ceilDiv(height, step));
					} else {
						param.setSourceSubsampling(step, step, 0, 0);
					}
				}
				return reader.read(0, param);
			} finally {
				reader.dispose();
			}
		}
	}

	/**
	 * Smallest subsampling step that keeps a decoded image under
	 * {@code maxPixels} pixels and under {@value #WEBP_MAX_DIMENSION} pixels per
	 * side. A step of {@code n} keeps one pixel out of {@code n} in each
	 * direction.
	 *
	 * @param width     source width
	 * @param height    source height
	 * @param maxPixels largest number of decoded pixels
	 * @return the subsampling step, {@code 1} when the image already fits
	 */
	static int subsamplingStep(int width, int height, long maxPixels) {
		int step = Math.max(1, (int) Math.ceil(Math.sqrt((double) width * height / maxPixels)));
		while ((long) Math.ceilDiv(width, step) * Math.ceilDiv(height, step) > maxPixels
				|| Math.max(Math.ceilDiv(width, step), Math.ceilDiv(height, step)) > WEBP_MAX_DIMENSION) {
			step++;
		}
		return step;
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
		if (!cachedFile.isFile() || !cachedFile.canRead()) {
			throw new IOException("Cached image does not exist or is unreadable");
		}
		response.setContentType("image/webp");
		response.setHeader("Cache-Control", "public, max-age=86400");
		FileUtils.copyFile(cachedFile, response.getOutputStream());
		logger.info("Served cached image: {}", cachedFile.getAbsolutePath());
	}
}
