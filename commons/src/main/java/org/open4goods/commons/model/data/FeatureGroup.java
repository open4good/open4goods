package org.open4goods.commons.model.data;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.commons.model.Localisable;
import org.open4goods.commons.model.icecat.IcecatFeature;

/**
 * Strong icecat mapping, thanks to them !
 */
public class FeatureGroup {

	
	private Integer icecatCategoryFeatureGroupId;
	
	/**
	 * Erased name if needed
	 * @param categoryFeatureGroupId
	 */
	private Localisable<String,String> name = new Localisable<>();
	
	private List<Integer> featuresId = new ArrayList<>();
	
	/**
	 * Features, if loaded
	 */
	private List<IcecatFeature> features = new ArrayList<IcecatFeature>();
	
	
	
	public FeatureGroup() {
	
	}
	public FeatureGroup(int categoryFeatureGroupId) {
		this.icecatCategoryFeatureGroupId = categoryFeatureGroupId;
	
	}

	public Integer getIcecatCategoryFeatureGroupId() {
		return icecatCategoryFeatureGroupId;
	}
	public void setIcecatCategoryFeatureGroupId(Integer icecatFeatureGroupId) {
		this.icecatCategoryFeatureGroupId = icecatFeatureGroupId;
	}

	public List<Integer> getFeaturesId() {
		return featuresId;
	}
	public void setFeaturesId(List<Integer> featuresId) {
		this.featuresId = featuresId;
	}
	public Localisable<String, String> getName() {
		return name;
	}
	public void setName(Localisable<String, String> name) {
		this.name = name;
	}
	public List<IcecatFeature> getFeatures() {
		return features;
	}
	public void setFeatures(List<IcecatFeature> features) {
		this.features = features;
	}

	
	
}
