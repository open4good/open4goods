package org.open4goods.model.icecat;

import java.util.ArrayList;
import java.util.List;

public class IcecatCategoryFeatureGroup {
    String id;
    String no;
    List<IcecatFeature> features = new ArrayList<>();
    // Getters and Setters
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getNo() {
		return no;
	}
	public void setNo(String no) {
		this.no = no;
	}
	public List<IcecatFeature> getFeatures() {
		return features;
	}
	public void setFeatures(List<IcecatFeature> features) {
		this.features = features;
	}
    
    
}