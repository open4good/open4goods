package org.open4goods.api.services.completion;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.constants.ResourceType;
import org.open4goods.model.data.ImageInfo;
import org.open4goods.model.data.Resource;
import org.open4goods.model.data.ResourceStatus;
import org.open4goods.model.product.Product;
import org.open4goods.services.ImageMagickService;
import org.open4goods.services.ResourceService;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;

// TODO : Detect / Prefer image transparency
// TODO : Delete evicted cache file
// TODO : Convert images to WEBP
// TODO : Clean config on MediaAggregationConfig
// TODO : Should put the processed flag on majority of evicted cases
public class ResourceCompletionService  extends AbstractCompletionService{


	// TODO : From yaml
	private static final double SIMILARITY_SCORE = 0.40;
	private static final int PERCEPTIV_HASH_SIZE = 32;

	// TODO : Dedicated logger
	protected static final Logger logger = LoggerFactory.getLogger(ResourceCompletionService.class);

	private final ApiProperties apiProperties;
	private final ImageMagickService imageService;	
	private final ResourceService resourceService;

	// TODO : move to Tika V2
	private static final Tika tika = new Tika();
	private static final TikaConfig config = TikaConfig.getDefaultConfig();

	// TODO : Warning : Probably not thread safe
	private static final HashingAlgorithm hasher = new PerceptiveHash(PERCEPTIV_HASH_SIZE);

	// private final ElasticsearchRestTemplate esTemplate;

	public ResourceCompletionService(ImageMagickService imageService, VerticalsConfigService verticalConfigService,
			ResourceService resourceService, ProductRepository dataRepository, ApiProperties apiProperties) {
		
		// TODO : Should set a specific log level here (not "agg(regation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());		

		this.apiProperties = apiProperties;
		
		this.imageService = imageService;
		this.resourceService = resourceService;

		// Replace all pixels with alpha values smaller than 0-255. The alpha value
		// cutoff is taken into account after down scaling the image, therefore choose a
		// reasonable value.
		int alphaThreshold = 243;
		hasher.setOpaqueHandling(Color.WHITE, alphaThreshold);
	}

	/**
	 * Process resources for one product
	 * 
	 * @param data
	 * @param vertical
	 */
	
	@Override
	public void processProduct(VerticalConfig vertical, Product data ) {

		////////////////////
		// Update all new items
		/////////////////////
		
		
		// Deleting existing groups
		data.getResources().forEach(e -> e.setGroup(null));
		
		
		List<Resource> resources = data.getResources().stream()
				// Exclude already processed
				.filter( e ->  vertical.getResourcesConfig().getOverrideResources() || !e.isProcessed() )
				// Exclude already the previously detected invalids
				.filter(e -> vertical.getResourcesConfig().getOverrideResources() || !e.isEvicted())
				// Download the resources and do the analyze
				.map(e -> fetchResource(e, vertical)).toList();

		
		data.getResources().removeAll(resources);
		data.getResources().addAll(resources);

		
		
		
		////////////////////////////////////////
		// Filtering images by validity
		////////////////////////////////////////
		
		Set<String> md5s = new HashSet<>();
		
		List<Resource> images = data.getResources().stream()
				.filter(e -> !e.isEvicted())
				// Filtering on images
				.filter(e -> e.getResourceType() == ResourceType.IMAGE)
				// Checking if not a blacklisted md5
				.map(e -> {
					if (vertical.getResourcesConfig().getMd5Exclusions().contains(e.getMd5())) {
						logger.warn("Excluded because of blacklisted MD5 : {}", e.getUrl());
						e.setStatus(ResourceStatus.MD5_EXCLUSION);
						e.setEvicted(true);
						e.setProcessed(true);						
					}
					return e;
				})
				// Deduplicating by MD5
				.map(e -> {
					
					if (md5s.contains(e.getMd5())) {
						logger.warn("Excluded because a duplicate MD5 : {}", e.getUrl());
						e.setStatus(ResourceStatus.MD5_DUPLICATE);
						e.setEvicted(true);
						e.setProcessed(true);
					}
					md5s.add(e.getMd5());
					return e;
				})
				// Filtering by number of pixels
				.map(e -> {

					if (e.getImageInfo().pixels() < vertical.getResourcesConfig().getMinPixelsEvictionSize()) {
						logger.warn("Excluded because image is too small : {}", e.getUrl());
						e.setStatus(ResourceStatus.TOO_SMALL);
						e.setEvicted(true);
						e.setProcessed(true);
					}
					return e;
				})
				

				.toList();


		// Updating
		data.getResources().removeAll(images);
		data.getResources().addAll(images);
		
		/////////////////////////////////////////
		// Images similarity clusterization
		/////////////////////////////////////////
		ArrayList<List<Resource>> classified = classify(images.stream().filter(e -> !e.isEvicted()).toList());
		
		logger.info("{} - {} resources links, {} processed, {} retained and classified in {} bucket", data.gtin(), data.getResources().size(), resources.size(), images.size(), classified.size());
		
		
		List<Resource> resultingImages = new ArrayList<>();
		
		for (List<Resource> dups : classified) {
			logger.info("{} duplicates found (resolution ordered) \n  {}", dups.size(), StringUtils.join(dups,"\n  "));				
			resultingImages.add(dups.getFirst());			
		}

		logger.info("{}/{} images selected for product {} : \n  {}",resultingImages.size(),images.size(), data.gtin(), StringUtils.join(resultingImages,"\n  ") );
		
		// TODO : Delete all evicted items after a given time
		
		
		// Setting the images on the product
		data.setImages(resultingImages);
		
		// Deleting useless files and unsetting attributes to preserve space
		for (Resource r : data.getResources()) {
			
			if (r.isEvicted()) {
				// If an evicted resource, systematicaly delete file.
				File evicted = resourceService.getCacheFile(r);
				logger.info("Deleting evicted resource :{} -> {}",r, evicted);
// TODO : uncomment to effectivly rm files				
//				if (!evicted.delete()) {
//					logger.error("Could not delete evicted resource : {}",resourceService.getCacheFile(r));
//				}
			
//				data.getResources().remove(r);
			}
			
		}
	}

	/**
	 * Operates image clusterisation based on a pHash distance
	 * 
	 * @param list
	 * @return
	 */
	private ArrayList<List<Resource>> classify(List<Resource> list) {
		// TODO Auto-generated method stub

		logger.info("Starting image perceptive clusterisation");

		Map<Resource, Set<Resource>> cluster = new HashMap<>();

		// Compare to each other
		for (Resource r1 : list) {
			cluster.put(r1, new HashSet<>());
			cluster.get(r1).add(r1);
			
			Hash hash0 = getHash(r1);
			for (Resource r2 : list) {
				if (r2.equals(r1)) {
					continue;
				}
				Hash hash1 = getHash(r2);

				double similarityScore = hash0.normalizedHammingDistanceFast(hash1);
				logger.info("image similarityScore : {} ", similarityScore);
				// TODO : From conf
				if (similarityScore < SIMILARITY_SCORE) {
					// Considered a duplicate in this particular case
					cluster.get(r1).add(r2);
					cluster.remove(r2);
				}
			}

		}

		
		// Formating output
		ArrayList<List<Resource>> sortedCluster = new ArrayList<List<Resource>>();
		
		
		
		
		// Sorting buckets content by image size (best image first)
		List<Resource> forcedFirst = null;
		for (Set<Resource> resourceGroups : cluster.values() ) {
			List<Resource> tmpList = new ArrayList<>();
			tmpList.addAll(resourceGroups);
			Collections.sort(tmpList, (o1, o2) -> o2.getImageInfo().pixels().compareTo(o1.getImageInfo().pixels()));
			sortedCluster.add(tmpList);			
			
			// We priorize on amazon primary image
			boolean primary =  resourceGroups.stream().map(e->e.getTags()).anyMatch(e -> e.contains(AmazonCompletionService.AMAZON_PRIMARY_TAG));
			if (primary) {
				forcedFirst = tmpList;
			}
		}
				
		// Sorting bucketsby number of occurences
		Collections.sort(sortedCluster, (o1, o2) -> Integer.compare(o2.size(), o1.size()));

		if (null != forcedFirst) {
			sortedCluster.remove(forcedFirst);
			sortedCluster.addFirst(forcedFirst);
		}
		// But 
		
		
		// Adding the group number		
		for (int i = 0; i < sortedCluster.size(); i++) {			
			for (Resource res : sortedCluster.get(i)) {
				res.setGroup(i);
			}
		}
		
		
		return sortedCluster;
	}

	/**
	 * Return the library hash from stored data
	 * 
	 * @param r2
	 * @return
	 */
	private Hash getHash(Resource r2) {
		return new Hash(BigInteger.valueOf(r2.getImageInfo().getpHashValue()), r2.getImageInfo().getpHashLength(), 0);
	}

	/**
	 * Download the file, and raw process the file detection features (image
	 * similarity, .....)
	 * 
	 * @param resource
	 * @param vertical
	 * @return
	 * @return
	 * @throws ValidationException
	 * @throws TechnicalException
	 */
	public Resource fetchResource(Resource resource, VerticalConfig vertical) {
		logger.info("Handling resource : {} ", resource);

		// Set FNV cache key based on url
		resource.setCacheKey(IdHelper.generateResourceId(resource.getUrl()));
		resource.setTimeStamp(System.currentTimeMillis());

		// Extracting filename
		resource.setFileName(resource.getUrl().substring(resource.getUrl().lastIndexOf('/')));
		int posPoint = resource.getFileName().indexOf('.');
		if (-1 != posPoint) {
			resource.setFileName(resource.getFileName().substring(1, posPoint));
		}

		// Get cached file reference if exists
		File target = resourceService.getCacheFile(resource);

		// Downloading the file if not cached
		// TODO : A specific config property to force re-download
		if (!target.exists()) {
			logger.info("Downloading resource to local : {}", target);

			try {
				Request.Get(resource.getUrl())
						// TODO from conf
						.userAgent("Mozilla/5.0 (Windows NT 5.1; rv:5.0.1) Gecko/20100101 Firefox/5.0.1")
						.connectTimeout(1000).socketTimeout(1000).execute().saveContent(target);
				
			} catch (ClientProtocolException e) {
				logger.error("Cannot download ({}) : {}", e.getMessage(), resource.getUrl());
				resource.setStatus(ResourceStatus.PROTOCOL_EXCEPTION);
				resource.setEvicted(true);
				return resource;
			} catch (IOException e) {
				logger.error("Cannot download ({}) : {}", e.getMessage(), resource.getUrl());
				resource.setStatus(ResourceStatus.IO_EXCEPTION);
				resource.setEvicted(true);
				return resource;
			}
		}
		
		// Setting file size
		resource.setFileSize(target.length());
		
		// Computing the MD5
		try (FileInputStream fis = new FileInputStream(target)) {
			final String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
			resource.setMd5(md5);

		} catch (final Exception e) {
			logger.error("Cannot compute hash", e);
			resource.setStatus(ResourceStatus.MD5_CHECKSUM_FAIL);
			resource.setEvicted(true);
			return resource;
		}

		try {
			// Checking type with apache tika
			resource.setMimeType(tika.detect(target));
			final org.apache.tika.mime.MimeType mimeType = config.getMimeRepository().forName(resource.getMimeType());
			resource.setExtension(mimeType.getExtension().substring(1));
		} catch (final Exception e) {
			logger.error("Cannot get mimetype ({}) : {}", e.getMessage(), resource.getUrl());
			resource.setStatus(ResourceStatus.NO_MIME_TYPE);
			resource.setEvicted(true);
			return resource;
		}

		try {

			////////////////////////////////////////////////////////
			// Applying specific parsing depending on the mimeType
			////////////////////////////////////////////////////////

			switch (resource.getMimeType()) {
			case "image/png":
			case "image/jpg":
			case "image/jpeg":
			case "image/webp":
				// case "application/octet-stream":
			case "image/gif":
				// Case of an image
				processImage(resource, target);
				resource.setProcessed(true);
				break;

			case "application/pdf":
				// Case of PDF
				processPdf(resource, target);
				break;

			default:
				logger.warn("Unknown resource type : {} : {}", resource.getMimeType(), resource.getUrl());
				// Case of Unknown
				resource.setResourceType(ResourceType.UNKNOWN);
			}

			logger.debug("fetching and analysis done : {} ", resource);
		} catch (final Exception e) {
			logger.warn("Resource integration fail : {} : {} ", e.getMessage(), resource);
		}

		return resource;
	}

	private void processPdf(final Resource indexed, final File target) {
		// TODO(1,p3,feature) : Generate default PNG version, generate thumnails from
		// config, html version, so on...
		// handle metadatas
		indexed.setResourceType(ResourceType.PDF);

	}

	/**
	 * We detect the image height / width, then images are default translated to
	 * PNG, then thumbnails are generated
	 *
	 * @param resource
	 * @param target
	 */
	private void processImage(final Resource resource, final File src) {

		resource.setResourceType(ResourceType.IMAGE);

		// Detect height / width
		final ImageInfo ii = imageService.buildImageInfo(src);

		if (null == ii) {
			logger.error("Cannot analyse image : {}", resource.getUrl());
			resource.setStatus(ResourceStatus.CANNOT_ANALYSE);
			resource.setEvicted(true);
			return ;
		} else {
			// Detect image phash
			try {
				Hash hash = hasher.hash(src);
				// TODO : Could remove as we always use the same
	//			ii.setpHashAlgorithmId(hash.getAlgorithmId());
				ii.setpHashValue(hash.getHashValue().longValue());
				ii.setpHashLength(hash.getBitResolution());
			} catch (IOException e) {
				logger.error("Cannot compute perceptive hash({}) : {}", e.getMessage(), resource.getUrl());
				resource.setStatus(ResourceStatus.PERCEPTIV_HASH_FAIL);
				resource.setEvicted(true);
			}
		}
	
		resource.setImageInfo(ii);
		
		
	}


}
