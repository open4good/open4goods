package org.open4goods.model.vertical;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ResourcesAggregationConfig(Boolean overrideResources, Set<String> md5Exclusions, int minPixelsEvictionSize) {

        private static final Logger logger = LoggerFactory.getLogger(ResourcesAggregationConfig.class);

        public ResourcesAggregationConfig() {
                this(false, new HashSet<>(), 2000);
        }

	public Boolean getOverrideResources() {
		return overrideResources;
	}


	public Set<String> getMd5Exclusions() {
		return md5Exclusions;
	}


	public int getMinPixelsEvictionSize() {
		return minPixelsEvictionSize;
	}

	
	

}
