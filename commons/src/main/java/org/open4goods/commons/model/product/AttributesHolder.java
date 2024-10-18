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
	private Map<ReferentielKey, String> referentiel = new HashMap<>();

	//TODO: rename
	
	/**
	 * Those attributes are defined from configuration, and indexed in elasticsearch 
	 */
	private Map<String,ProductAttribute> indexed = new HashMap<>();


	/**
	 * Those attributes are not indexed 
	 */
	private Map<String, ProductAttribute> all = new HashMap<>();


	private Set<AggregatedFeature> features = new HashSet<>();

	@Override
	public String toString() {
		return "ref:"+referentiel.size()+ " , agg:"+indexed.size() +" , unmaped:"+all.size() +" , features:"+features.size();
	}
	
	

	public int count() {
		
		return referentiel.size() + indexed.size() + all.size() + features.size();
	}
	

	// TODO : performance
	public ProductAttribute attributeByFeatureId(Integer featureId) {
		if (null == featureId) {
			return null;
		}
		
		ProductAttribute ret = all.values().stream().filter(a -> a.getIcecatTaxonomyIds().contains(featureId)).findFirst()
				.orElse(null);
				return ret;
	}
	
	public void addReferentielAttribute(ReferentielKey key, String value) {
		referentiel.put(key, value);
	}

	public Map<String, String> referentielAttributesAsStringKeys() {
		return referentiel.entrySet().stream().collect(Collectors.toMap(
					e -> e.getKey().toString(),
				Map.Entry::getValue
					
				));
	}
	
	



	public Map<String, ProductAttribute> getIndexed() {
		return indexed;
	}
	public void setIndexed(Map<String, ProductAttribute> aggregatedAttributes) {
		this.indexed = aggregatedAttributes;
	}
	public Set<AggregatedFeature> getFeatures() {
		return features;
	}

	public void setFeatures(Set<AggregatedFeature> features) {
		this.features = features;
	}

	

	public Map<String, ProductAttribute> getAll() {
		return all;
	}



	public void setAll(Map<String, ProductAttribute> allAttributes) {
		this.all = allAttributes;
	}



	public Map<ReferentielKey, String> getReferentiel() {
		return referentiel;
	}

	public void setReferentiel(Map<ReferentielKey, String> referentielAttributes) {
		this.referentiel = referentielAttributes;
	}
}
