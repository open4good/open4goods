package org.open4goods.ui.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.Resource;
import org.open4goods.services.ImageMagickService;
import org.open4goods.services.ResourceService;

/**
 * The ui customisation of alerting service. Simply log as CSV the affiliated
 * links click
 *
 *
 * @author Goulven.Furet
 *
 */
public class ImageService  {

	private ImageMagickService imageMagickService;

	private ResourceService resourceService;


	public ImageService(ImageMagickService imageMagickService, ResourceService resourceService) {
		this.imageMagickService = imageMagickService;
		this.resourceService = resourceService;

	}


}
