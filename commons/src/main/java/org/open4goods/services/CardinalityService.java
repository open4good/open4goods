package org.open4goods.services;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.helper.SimpleImageAnalyser;
import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.data.ImageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains and provides score cardinalities
 *
 * @author Goulven.Furet
 *
 */

public class CardinalityService {

	private final static Logger log = LoggerFactory.getLogger(CardinalityService.class);

	
	private Map<String, Cardinality> cardinalities = new ConcurrentHashMap<>();


	public Map<String, Cardinality> getCardinalities() {
		return cardinalities;
	}


	public void setCardinalities(Map<String, Cardinality> cardinalities) {
		this.cardinalities = cardinalities;
	}
	
	
	
	
	

}
