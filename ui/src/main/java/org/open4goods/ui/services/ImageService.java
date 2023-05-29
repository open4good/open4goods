package org.open4goods.ui.services;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.ResourceCachingTask;
import org.open4goods.model.data.IndexedResource;
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


	/**
	 * Return the output stream for an image, with metadata and converted pageSize png
	 * @param r
	 * @return
	 * @throws ValidationException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws TechnicalException
	 */
	public InputStream getCoverPng(Resource r) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
		ResourceCachingTask rct = new ResourceCachingTask(r, imageMagickService, resourceService);

		IndexedResource ir = rct.doFetching(r);

		//TODO(gof) : if exception render default error image
		return IOUtils.toBufferedInputStream(new FileInputStream(resourceService.getCacheFile(ir)));
	}






}
