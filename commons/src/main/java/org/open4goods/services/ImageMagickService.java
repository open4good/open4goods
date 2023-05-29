package org.open4goods.services;

import java.io.File;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.helper.SimpleImageAnalyser;
import org.open4goods.model.data.ImageInfo;
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

	public void convertToPng(final File source, final File target) {

		try {
			final ProcessBuilder pb = new ProcessBuilder("convert",
					source.getAbsolutePath(), target.getAbsolutePath());

			final Process p = pb.start();
			p.waitFor();

			final String err = IOUtils.toString(p.getErrorStream(), "UTF-8");
			final String std = IOUtils.toString(p.getInputStream(), "UTF-8");

			IOUtils.closeQuietly(p.getErrorStream());
			IOUtils.closeQuietly(p.getInputStream());


			if (log.isInfoEnabled() && !StringUtils.isEmpty(std)) {
				log.info("Image magick output : {}", err);
			}

			if (!StringUtils.isEmpty(err)) {
				log.error("Error with image magick command : {}", err);
			}

		} catch (final Exception e) {
			log.error("Error while generating default translated image for favico {} : {}", source, e.getMessage());


		}
	}

	/**
	 * Generate a thumbnail from the originaly translated (png) image
	 *
	 *
	 *
	 * @param height
	 */
	public void generateThumbnail(final File src, final File target, final Integer height) {

		try {

			final ProcessBuilder pb = new ProcessBuilder("convert", "-geometry", "x" + height, src.getAbsolutePath(),
					target.getAbsolutePath());

			final Process p = pb.start();
			p.waitFor();

			final String err = IOUtils.toString(p.getErrorStream(), "UTF-8");
			final String std = IOUtils.toString(p.getInputStream(), "UTF-8");

			IOUtils.closeQuietly(p.getErrorStream());
			IOUtils.closeQuietly(p.getInputStream());

			if (log.isInfoEnabled() && !StringUtils.isEmpty(std)) {
				log.info("Image magick output : {}", err);
			}

			if (!StringUtils.isEmpty(err)) {
				log.error("Error with image magick command : {}", err);
			}

		} catch (final Exception e) {
			log.error("Error while generating default translated image for {} : {}", src.getAbsolutePath(),
					e.getMessage());
		}

	}

	/**
	 * Use image magick to transform to png with no metadata
	 *
	 *
	 *
	 */
	public void normalizeImage(final File source, final File target) {

		try {
			final ProcessBuilder pb = new ProcessBuilder("convert", "-strip", source.getAbsolutePath(),
					target.getAbsolutePath());

			final Process p = pb.start();
			p.waitFor();

			final String err = IOUtils.toString(p.getErrorStream(), "UTF-8");
			final String std = IOUtils.toString(p.getInputStream(), "UTF-8");

			IOUtils.closeQuietly(p.getErrorStream());
			IOUtils.closeQuietly(p.getInputStream());

			if (log.isInfoEnabled() && !StringUtils.isEmpty(std)) {
				log.info("Image magick output : {}", err);
			}

			if (!StringUtils.isEmpty(err)) {
				log.error("Error with image magick command : {}", err);
			}

		} catch (final Exception e) {
			log.error("Error while generating default translated image for {} : {}", source.getAbsolutePath(),
					e.getMessage());
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
			log.warn("Cannot read image size / height : {} > {}", target.getAbsolutePath(), e.getMessage());
		}

		return null;
	}

}
