package org.open4goods.helper;

import java.io.File;
import java.io.FileInputStream;

import org.apache.http.client.fluent.Request;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.constants.ResourceType;
import org.open4goods.model.data.ImageInfo;
import org.open4goods.model.data.IndexedResource;
import org.open4goods.model.data.Resource;
import org.open4goods.services.ImageMagickService;
import org.open4goods.services.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO(0.5, p3, test) : write a test : already broken once with tika versions (file download + detection + indexation)
/**
 * This Thread is in charge of :
 * <ul>
 * <li>Downloading resources associated to DataFragments</li>
 * <li>Operating data analyze (image type, pdf metadatas, so on))</li>
 * <li>Resource effectiv indexation</li>
 * </ul>
 * @author Goulven.Furet
 *
 */
public class ResourceCachingTask  {

	private static final Logger logger = LoggerFactory.getLogger(ResourceCachingTask.class);

	//	private final VerticalConfig uiConfig;

	private final Resource resource;

	private static final Tika tika = new Tika();

	private static final TikaConfig config = TikaConfig.getDefaultConfig();

	//	private final ElasticsearchRestTemplate esTemplate;

	private final ImageMagickService imageService;

	private ResourceService resourceService;

	public ResourceCachingTask(final Resource r,
			final ImageMagickService imageService,  ResourceService resourceService) {
		resource = r;
		//		this.uiConfig = uiConfig;
		//		this.esTemplate = esTemplate;
		this.imageService = imageService;
		this.resourceService = resourceService;
	}



	//
	//	@Override
	//	public void run() {
	//		IndexedResource ir;
	//		try {
	//			// Fetching
	//			ir = doFetching(resource);
	//
	//			// Indexing in the dedicated index
	//			index(ir);
	//		} catch (ValidationException e) {
	//			logger.info("{} for resource {} ",e.getMessage(), resource);
	//		} catch (TechnicalException e) {
	//			logger.warn("Error while processing resource {}",resource,e);
	//		}
	//
	//
	//	}

	public IndexedResource doFetching(Resource resource) throws ValidationException, TechnicalException{
		logger.info("Handling resource : {} ", resource);

		// Get MD5 hash
		final String hash = IdHelper.generateResourceId(resource.getUrl());
		final String url = resource.getUrl();

		// Creating the indexed version
		final IndexedResource indexed = new IndexedResource();
		indexed.setCacheKey(hash);
		indexed.setUrl(url);
		indexed.setProviderName(resource.getProviderName());
		indexed.setTimeStamp(resource.getTimeStamp());
		indexed.setTags(resource.getTags());

		// Cached file reference
		File target;
		target =  resourceService.getCacheFile(indexed);


		//		// Checking if the indexed resource already exists
		//		if (!uiConfig.getResourcesConfig().getOverrideResources() && target.exists()) {
		//
		//			throw new ValidationException("resource already cached : "+ resource);
		//		}

		if (target.exists()) {
			//			throw new ValidationException("resource already cached : "+ resource);
			return indexed;
		}

		try {

			// Downloading the file
			if (!target.exists()) {
				logger.info("Saving resource to local : {}",target);
				Request.Get(url)
				//TODO from conf
				.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:5.0.1) Gecko/20100101 Firefox/5.0.1")
				.connectTimeout(1000)
				.socketTimeout(1000)
				.execute()
				.saveContent(target);
			}

			// Computing the MD5
			try (FileInputStream fis = new FileInputStream(target)) {
				final String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis).toUpperCase();
				indexed.setMd5(md5);
			} catch (final Exception e) {
				throw new TechnicalException("Cannot compute hash", e);

			}

			try {
				// Checking type with apache tika
				indexed.setMimeType(tika.detect(target));


				final MimeType mimeType = config.getMimeRepository().forName(indexed.getMimeType());

				indexed.setExtension(mimeType.getExtension().substring(1));
			} catch (final Exception e) {
				logger.error("Exception while determining mimetype",e);
			}


			////////////////////////////////////////////////////////
			// Applying specific parsing depending on the mimeType
			////////////////////////////////////////////////////////

			switch (indexed.getMimeType()) {
			case "image/png":
			case "image/jpg":
			case "image/jpeg":
				//			case "application/octet-stream":
			case "image/gif":
				// Case of image

				processImage(indexed, target);
				break;

			case "application/pdf":
				// Case of PDF
				processPdf(indexed, target);
				break;

			default:
				logger.warn("Unknown resource type : {} : {}", indexed.getMimeType(), indexed.getUrl());
				// Case of Unknown
				processUnknown(indexed, target);
			}

			logger.debug("fetching and analysis done : {} ", resource);
		} catch (final Exception e) {
			logger.warn("Resource integration fail : {} : {} ",  e.getMessage(), url);
		}

		return indexed;
	}

	private void processUnknown(final IndexedResource indexed, final File target) {
		indexed.setResourceType(ResourceType.UNKNOWN);
	}

	private void processPdf(final IndexedResource indexed, final File target) {
		// TODO(1,p3,feature) : Generate default PNG version, generate thumnails from config, html version, so on...
		// handle metadatas
		indexed.setResourceType(ResourceType.PDF);

	}

	/**
	 * We detect the image height / width, then images are default translated to
	 * PNG, then thumbnails are generated
	 *
	 * @param indexed
	 * @param target
	 */
	private void processImage(final IndexedResource indexed, final File src) {

		indexed.setResourceType(ResourceType.IMAGE);

		// Detect height / width
		final ImageInfo ii = imageService.buildImageInfo(src);
		indexed.setImageInfo(ii);

		// Generate the default PNG version
		try {
			imageService.normalizeImage(src, resourceService.getTranslatedCacheFile(indexed));
		} catch (final ValidationException e) {
			logger.warn("Error processing image : ", indexed.getUrl(), e.getMessage());
		}

		//		// Generate thumbnails
		//		for (final Integer height : uiConfig.getResourcesConfig().getImageProductThumbsHeight()) {
		//			if (null != indexed) {
		//				// Detect height / width
		//				try {
		//					imageService.generateThumbnail(resourceService.getTranslatedCacheFile(indexed),
		//							resourceService.getThumbnailCacheFile(indexed, height), height);
		//					indexed.getImageInfo().getAvaillableThumbs().add(height);
		//				} catch (final ValidationException e) {
		//					logger.warn("Cannot generate thumnail for {} : {}", indexed.getUrl(), e.getMessage());
		//				}
		//			}
		//		}
	}

	//	private void index(final IndexedResource indexed) {
	//		final IndexQuery q = new IndexQueryBuilder().withObject(indexed).withId(indexed.getCacheKey()).build();
	//		esTemplate.save(q,IndexCoordinates.of(uiConfig.resourceIndex()) );
	//
	//	}

}
