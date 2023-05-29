package org.open4goods.config.yml.ui;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaAggregationConfig {

	private static final Logger logger = LoggerFactory.getLogger(MediaAggregationConfig.class);

	/**
	 * The max conccurent threads used to to download materials (images, pdfs, ...)
	 */
	private Integer resourceDownloadConcurentThreads = 6;


	/**
	 * If true, resources downloading will be skipped. Usefull for verticals testing
	 */
	private Boolean skipResourcesFetching = false;

	/**
	 * If set to true, the resources will be downlladed / indexed anyway, even if
	 * cached.
	 */
	private Boolean overrideResources = false;

	/**
	 * The list of image ids that must be excluded
	 */
	private Set<String> resourceIdExclusions = new HashSet<>();

	/**
	 * The list of thumbnails that will be generated for products
	 */
	private Set<Integer> imageProductThumbsHeight = new HashSet<>();

	/**
	 * The list of thumbnails that will be generated for brands
	 */
	private Set<Integer> imageBrandsThumbsHeight = new HashSet<>();





	public Integer getResourceDownloadConcurentThreads() {
		return resourceDownloadConcurentThreads;
	}

	public void setResourceDownloadConcurentThreads(final Integer resourceDownloadConcurentThreads) {
		this.resourceDownloadConcurentThreads = resourceDownloadConcurentThreads;
	}

	public Boolean getOverrideResources() {
		return overrideResources;
	}

	public void setOverrideResources(final Boolean overrideResources) {
		this.overrideResources = overrideResources;
	}


	public Set<String> getResourceIdExclusions() {
		return resourceIdExclusions;
	}

	public void setResourceIdExclusions(final Set<String> resourceIdExclusions) {
		this.resourceIdExclusions = resourceIdExclusions;
	}

	public Set<Integer> getImageProductThumbsHeight() {
		return imageProductThumbsHeight;
	}

	public void setImageProductThumbsHeight(final Set<Integer> imageProductThumbsHeight) {
		this.imageProductThumbsHeight = imageProductThumbsHeight;
	}

	public Set<Integer> getImageBrandsThumbsHeight() {
		return imageBrandsThumbsHeight;
	}

	public void setImageBrandsThumbsHeight(final Set<Integer> imageBrandsThumbsHeight) {
		this.imageBrandsThumbsHeight = imageBrandsThumbsHeight;
	}

	public Boolean getSkipResourcesFetching() {
		return skipResourcesFetching;
	}

	public void setSkipResourcesFetching(Boolean skipResourcesFetching) {
		this.skipResourcesFetching = skipResourcesFetching;
	}



}
