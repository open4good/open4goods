package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.Localisable;
import org.open4goods.model.icecat.IcecatFeature;

/**
 * Strong icecat mapping, thanks to them !
 */
public record FeatureGroup(
                Integer icecatCategoryFeatureGroupId,
                Localisable<String, String> name,
                List<Integer> featuresId,
                List<IcecatFeature> features) {

        public FeatureGroup() {
                this(null, new Localisable<>(), new ArrayList<>(), new ArrayList<>());
        }

        public FeatureGroup(int categoryFeatureGroupId) {
                this(categoryFeatureGroupId, new Localisable<>(), new ArrayList<>(), new ArrayList<>());
        }
	
	
	

        public Integer getIcecatCategoryFeatureGroupId() {
                return icecatCategoryFeatureGroupId;
        }
        public List<Integer> getFeaturesId() {
                return featuresId;
        }
        public Localisable<String, String> getName() {
                return name;
        }
        public List<IcecatFeature> getFeatures() {
                return features;
        }

	
	
}
