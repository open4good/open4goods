package org.open4goods.model.data;

import org.open4goods.model.constants.ResourceType;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * In this class, all possible values for resources are flat handled. (for elastic indexxation purpose)
 * @author goulven
 *
 */
//@Document(indexName = "products", shards = 1, replicas = 0, refreshInterval = "-1")
public class IndexedResource extends Resource {

	private String mimeType;

	private String extension;

	private String md5;

	private ResourceType resourceType;

	private ImageInfo imageInfo;



	/**
	 * Convenient method to retrieve the image fileName
	 * @return
	 */
	public String filename() {
		final StringBuilder sb = new StringBuilder(getCacheKey().length()+5);
		sb.append(getCacheKey()).append(".").append(extension);
		return sb.toString();
	}


	// NOTE(gof) : Should be only for image, but with elastic, using specialized objects is not an option (?)

	public String translatedFileName() {
		final StringBuilder sb = new StringBuilder(getCacheKey().length()+5);
		sb.append(getCacheKey()).append("_ORIGINAL.png");
		return sb.toString();
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(final String md5) {
		this.md5 = md5;
	}

	public ResourceType getResourceType() {
		return resourceType;
	}

	public void setResourceType(final ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public ImageInfo getImageInfo() {
		return imageInfo;
	}

	public void setImageInfo(final ImageInfo imageInfo) {
		this.imageInfo = imageInfo;
	}


	public String getExtension() {
		return extension;
	}

	public void setExtension(final String extension) {
		this.extension = extension;
	}

}
