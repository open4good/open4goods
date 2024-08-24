package org.open4goods.commons.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.commons.model.attribute.Cardinality;
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
