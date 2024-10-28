package org.open4goods.commons.model.product;

import java.util.HashMap;
import java.util.Map;

/**
 * An holder to easiest partial updates
 */
public class ProductPartialUpdateHolder {

	
	private Long productId;
	
	private Map<String,Object> changes = new HashMap<>();

	
	
	public ProductPartialUpdateHolder(Long productId) {
		super();
		this.productId = productId;
	}

	public void addChange(String key, Object object) {
		changes.put(key, object);
	}
	

	public Map<String, Object> getChanges() {
		return changes;
	}

	public void setChanges(Map<String, Object> changes) {
		this.changes = changes;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}
	
	
	
}
