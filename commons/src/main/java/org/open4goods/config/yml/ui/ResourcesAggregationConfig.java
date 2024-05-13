package org.open4goods.config.yml.ui;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcesAggregationConfig {

	private static final Logger logger = LoggerFactory.getLogger(ResourcesAggregationConfig.class);

	/**
	 * If set to true, the resources will be downlladed / indexed anyway, even if
	 * cached.
	 */
	private Boolean overrideResources = false;

	/**
	 * The list of MD5 files that must be excluded
	 */
	private Set<String> md5Exclusions = new HashSet<>();

	private int minPixelsEvictionSize = 2000;

	public Boolean getOverrideResources() {
		return overrideResources;
	}

	public void setOverrideResources(Boolean overrideResources) {
		this.overrideResources = overrideResources;
	}

	public Set<String> getMd5Exclusions() {
		return md5Exclusions;
	}

	public void setMd5Exclusions(Set<String> md5Exclusions) {
		this.md5Exclusions = md5Exclusions;
	}

	public int getMinPixelsEvictionSize() {
		return minPixelsEvictionSize;
	}

	public void setMinPixelsEvictionSize(int minPixelsEvictionSize) {
		this.minPixelsEvictionSize = minPixelsEvictionSize;
	}
	
	

}
