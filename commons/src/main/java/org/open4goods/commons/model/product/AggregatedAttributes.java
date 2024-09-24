package org.open4goods.commons.model.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.commons.model.constants.ReferentielKey;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
//@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregatedAttributes  {

	/**
	 * The referentiel attributes
	 */
	private Map<ReferentielKey, String> referentielAttributes = new HashMap<>();

	//TODO(p3,naming) : rename
	/**
	 * The key is the english name, such as classified by icecat
	 */
	private Map<String,AggregatedAttribute> attributes = new HashMap<>();

	@Field(enabled = false, store = false, type = FieldType.Object)
	private Set<AggregatedAttribute> unmatchedAttributes = new HashSet<>();

	@Field(enabled = false,  store = false, type = FieldType.Object)
	private Set<AggregatedFeature> features = new HashSet<>();

	@Override
	public String toString() {
		return "ref:"+referentielAttributes.size()+ " , agg:"+attributes.size() +" , unmaped:"+unmatchedAttributes.size() +" , features:"+features.size();
	}
	
	public int count() {
		return referentielAttributes.size() + attributes.size() + unmatchedAttributes.size() + features.size();
	}
	

	// TODO : performance
	public AggregatedAttribute attributeByFeatureId(Integer featureId) {
		if (null == featureId) {
			return null;
		}
		
		AggregatedAttribute ret = unmatchedAttributes.stream().filter(a -> a.getIcecatTaxonomyIds().contains(featureId)).findFirst()
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


	public Map<String, AggregatedAttribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(Map<String, AggregatedAttribute> aggregatedAttributes) {
		this.attributes = aggregatedAttributes;
	}
	public Set<AggregatedFeature> getFeatures() {
		return features;
	}

	public void setFeatures(Set<AggregatedFeature> features) {
		this.features = features;
	}

	public Set<AggregatedAttribute> getUnmatchedAttributes() {
		return unmatchedAttributes;
	}

	public void setUnmatchedAttributes(Set<AggregatedAttribute> unmapedAttributes) {
		this.unmatchedAttributes = unmapedAttributes;
	}

	public Map<ReferentielKey, String> getReferentielAttributes() {
		return referentielAttributes;
	}

	public void setReferentielAttributes(Map<ReferentielKey, String> referentielAttributes) {
		this.referentielAttributes = referentielAttributes;
	}









}
