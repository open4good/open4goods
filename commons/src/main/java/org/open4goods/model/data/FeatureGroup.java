package org.open4goods.model.data;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.Localisable;

/**
 * Strong icecat mapping, thanks to them !
 */
public class FeatureGroup {

	
	private Integer icecatCategoryFeatureGroupId;
	
	/**
	 * Erased name if needed
	 * @param categoryFeatureGroupId
	 */
	private Localisable name = new Localisable();
	
	private List<Integer> featuresId = new ArrayList<>();
	
	
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
	public Localisable getName() {
		return name;
	}
	public void setName(Localisable name) {
		this.name = name;
	}
	public List<Integer> getFeaturesId() {
		return featuresId;
	}
	public void setFeaturesId(List<Integer> featuresId) {
		this.featuresId = featuresId;
	}

	
	
}
