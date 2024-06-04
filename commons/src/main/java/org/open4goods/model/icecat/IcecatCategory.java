package org.open4goods.model.icecat;
import java.util.ArrayList;
import java.util.List;

public class IcecatCategory {
    String id;
    String lowPic;
    String uncatId;
    String updated;
    List<IcecatCategoryFeatureGroup> categoryFeatureGroups = new ArrayList<>();
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLowPic() {
		return lowPic;
	}
	public void setLowPic(String lowPic) {
		this.lowPic = lowPic;
	}
	public String getUncatId() {
		return uncatId;
	}
	public void setUncatId(String uncatId) {
		this.uncatId = uncatId;
	}
	public String getUpdated() {
		return updated;
	}
	public void setUpdated(String updated) {
		this.updated = updated;
	}
	public List<IcecatCategoryFeatureGroup> getCategoryFeatureGroups() {
		return categoryFeatureGroups;
	}
	public void setCategoryFeatureGroups(List<IcecatCategoryFeatureGroup> categoryFeatureGroups) {
		this.categoryFeatureGroups = categoryFeatureGroups;
	}

    // Getters and Setters
    
    
}








