package org.open4goods.services.imageprocessing.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.resource.ImageInfo;
import org.open4goods.services.imageprocessing.helper.SimpleImageAnalyser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An image service based on image magick
 *
 * @author Goulven.Furet
 *
 */

public class ImageMagickService {

	private final static Logger log = LoggerFactory.getLogger(ImageMagickService.class);

	// /**
	// * Use image magick to transform to png with no metadata
	// *
	// *
	// *
	// */

	/**
	 * Converts an image to a temporary PNG that Java image libraries can decode.
	 *
	 * <p>ImageMagick supports several supplier formats, notably WebP, that are
	 * not always available through ImageIO or DJL. The caller owns the returned
	 * temporary file and must delete it after processing.</p>
	 *
	 * @param source source image to normalize
	 * @return readable PNG file containing the first image frame
	 * @throws IOException when ImageMagick cannot create a usable PNG
	 */
	public File createJavaCompatiblePng(final File source) throws IOException {
		final File target = File.createTempFile("open4goods-image-", ".png");
		try {
			final Process process = new ProcessBuilder(
					"convert", source.getAbsolutePath() + "[0]", "-strip", target.getAbsolutePath()).start();
			final int exitCode = process.waitFor();
			final String error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8).trim();
			IOUtils.closeQuietly(process.getInputStream());
			IOUtils.closeQuietly(process.getErrorStream());
			if (exitCode != 0 || !target.isFile() || target.length() == 0L) {
				throw new IOException("ImageMagick could not normalize image" +
						(StringUtils.isBlank(error) ? "" : ": " + error));
			}
			return target;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Image normalization was interrupted", exception);
		} catch (IOException exception) {
			Files.deleteIfExists(target.toPath());
			throw exception;
		} catch (RuntimeException exception) {
			Files.deleteIfExists(target.toPath());
			throw exception;
		}
	}

	/**
	 * NOTE(gof) : Not efficient, as we load the whole image
	 *
	 *
	 *
	 * @return
	 * @return
	 */
	public ImageInfo buildImageInfo(final File target) {
		try {
			final SimpleImageAnalyser sii = new SimpleImageAnalyser(target);
			final ImageInfo ii = new ImageInfo();
			ii.setHeight(sii.getHeight());
			ii.setWidth(sii.getWidth());

			return ii;
		} catch (final Exception e) {
			log.debug("SimpleImageAnalyser failed ({}), falling back to ImageMagick identify: {}", e.getMessage(), target.getAbsolutePath());
		}

		// Fallback for formats unsupported by SimpleImageAnalyser (e.g. WebP)
		try {
			final ProcessBuilder pb = new ProcessBuilder("identify", "-format", "%wx%h", target.getAbsolutePath() + "[0]");
			final Process p = pb.start();
			p.waitFor();
			final String out = IOUtils.toString(p.getInputStream(), StandardCharsets.UTF_8).trim();
			IOUtils.closeQuietly(p.getInputStream());
			IOUtils.closeQuietly(p.getErrorStream());
			if (!StringUtils.isBlank(out) && out.contains("x")) {
				final String[] parts = out.split("x");
				final ImageInfo ii = new ImageInfo();
				ii.setWidth(Integer.parseInt(parts[0]));
				ii.setHeight(Integer.parseInt(parts[1]));
				return ii;
			}
		} catch (final Exception e) {
			log.warn("Cannot read image size / height : {} > {}", target.getAbsolutePath(), e.getMessage());
		}

		return null;
	}

}
