package org.open4goods.ui.services;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.krysalis.barcode4j.impl.upcean.EAN13Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.data.Resource;
import org.open4goods.commons.services.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ui customisation of alerting service. Simply log as CSV the affiliated
 * links click
 *
 *
 * @author Goulven.Furet
 *
 */
public class GtinService {

	protected static final Logger logger = LoggerFactory.getLogger(GtinService.class);

	private ResourceService resourceService;

	public GtinService(ResourceService resourceService) {
		super();
		this.resourceService = resourceService;
	}

	public InputStream gtin(String gtin) throws ValidationException, FileNotFoundException, IOException {


		/////////////////////////
		// Generate UEAN image
		/////////////////////////
		final String key = IdHelper.generateResourceId(gtin);
		logger.info("Will generate barcode for {} ", key);

		// 2 : Generate the image

		final String gtinKey = "gtin-" + key;
		generateGtin13Img(gtinKey, gtin);

		Resource r = new Resource();
		r.setCacheKey(gtinKey);
		r.setTimeStamp(System.currentTimeMillis());

		// TODO(gof) : if exception render default error image
		return IOUtils.toBufferedInputStream(new FileInputStream(resourceService.getCacheFile(r)));



	}

	public File generateGtin13Img(final String key, final String gtin) throws ValidationException {
		final File dest = resourceService.getCacheFile(key);

		if (dest.exists()) {
			logger.info("Skipping barcode generation, file exists");

		} else {
			final EAN13Bean bean = new EAN13Bean();

			try (OutputStream out = new FileOutputStream(dest)) {
				final BitmapCanvasProvider canvas = new BitmapCanvasProvider(out, "image/x-png",
						// TODO : Size pageNumber conf
						250, BufferedImage.TYPE_BYTE_GRAY, true, 0);
				bean.generateBarcode(canvas, gtin);
				canvas.finish();

			} catch (final Exception e) {
				logger.warn("Error while generating barCode", e);
				throw new ValidationException("Cannot genrate barcode : " + e.getMessage());
			}

		}

		return dest;
	}

}
