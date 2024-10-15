package org.open4goods.commons.model.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.commons.model.constants.ReferentielKey;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttributesHolder  {

	/**
	 * The referentiel attributes
	 */
	private Map<ReferentielKey, String> referentielAttributes = new HashMap<>();

	//TODO: rename
	
	/**
	 * Those attributes are defined from configuration, and indexed in elasticsearch 
	 */
	private Map<String,AggregatedAttribute> indexedAttributes = new HashMap<>();


	/**
	 * Those attributes are not indexed 
	 */
	private Map<String, AggregatedAttribute> all = new HashMap<>();


	private Set<AggregatedFeature> features = new HashSet<>();

	@Override
	public String toString() {
		return "ref:"+referentielAttributes.size()+ " , agg:"+indexedAttributes.size() +" , unmaped:"+all.size() +" , features:"+features.size();
	}
	
	

	public int count() {
		
		return referentielAttributes.size() + indexedAttributes.size() + all.size() + features.size();
	}
	

	// TODO : performance
	public AggregatedAttribute attributeByFeatureId(Integer featureId) {
		if (null == featureId) {
			return null;
		}
		
		AggregatedAttribute ret = all.values().stream().filter(a -> a.getIcecatTaxonomyIds().contains(featureId)).findFirst()
				.orElse(null);
				return ret;
	}
	
	public void addReferentielAttribute(ReferentielKey key, String value) {
		referentielAttributes.put(key, value);
	}

	public Map<String, String> referentielAttributesAsStringKeys() {
		return referentielAttributes.entrySet().stream().collect(Collectors.toMap(
					e -> e.getKey().toString(),
				Map.Entry::getValue
					
				));
	}
	
	



	public Map<String, AggregatedAttribute> getIndexedAttributes() {
		return indexedAttributes;
	}
	public void setIndexedAttributes(Map<String, AggregatedAttribute> aggregatedAttributes) {
		this.indexedAttributes = aggregatedAttributes;
	}
	public Set<AggregatedFeature> getFeatures() {
		return features;
	}

	public void setFeatures(Set<AggregatedFeature> features) {
		this.features = features;
	}

	

	public Map<String, AggregatedAttribute> getAll() {
		return all;
	}



	public void setAll(Map<String, AggregatedAttribute> allAttributes) {
		this.all = allAttributes;
	}



	public Map<ReferentielKey, String> getReferentielAttributes() {
		return referentielAttributes;
	}

	public void setReferentielAttributes(Map<ReferentielKey, String> referentielAttributes) {
		this.referentielAttributes = referentielAttributes;
	}
}
