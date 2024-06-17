package org.open4goods.model.product;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.dto.UiFeatureGroups;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)
public class AggregatedAttributes  {

	/**
	 * The referentiel attributes
	 */
	@Field(index = true, store = false, type = FieldType.Object)
	private Map<ReferentielKey, String> referentielAttributes = new HashMap<>();

	@Field(index = true, store = false, type = FieldType.Object)

	
	//TODO: rename
	private Map<String,AggregatedAttribute> aggregatedAttributes = new HashMap<>();


	@Field(index = false, store = false,type = FieldType.Object)
	private Set<AggregatedAttribute> unmapedAttributes = new HashSet<>();


	@Field(index = false, store = false, type = FieldType.Object)
	private Set<AggregatedFeature> features = new HashSet<>();

	@Override
	public String toString() {
		return "ref:"+referentielAttributes.size()+ " , agg:"+aggregatedAttributes.size() +" , unmaped:"+unmapedAttributes.size() +" , features:"+features.size();
	}
	
	

	public int count() {
		
		return referentielAttributes.size() + aggregatedAttributes.size() + unmapedAttributes.size() + features.size();
	}
	

	// TODO : performance
	public AggregatedAttribute attributeByFeatureId(Integer featureId) {
		if (null == featureId) {
			return null;
		}
		
		AggregatedAttribute ret = unmapedAttributes.stream().filter(a -> a.getIcecatTaxonomyIds().contains(Long.valueOf(featureId))).findFirst()
				.orElse(null);
				return ret;
	}
	
	public void addReferentielAttribute(ReferentielKey key, String value) {
		referentielAttributes.put(key, value);
	}


		
	public Map<String, String> referentielAttributesAsStringKeys() {
		// TODO Auto-generated method stub
		return referentielAttributes.entrySet().stream().collect(Collectors.toMap(
					e -> e.getKey().toString(),
				Map.Entry::getValue
					
				));
	}
	
	



	public Map<String, AggregatedAttribute> getAggregatedAttributes() {
		return aggregatedAttributes;
	}
	public void setAggregatedAttributes(Map<String, AggregatedAttribute> aggregatedAttributes) {
		this.aggregatedAttributes = aggregatedAttributes;
	}
	public Set<AggregatedFeature> getFeatures() {
		return features;
	}

	public void setFeatures(Set<AggregatedFeature> features) {
		this.features = features;
	}

	public Set<AggregatedAttribute> getUnmapedAttributes() {
		return unmapedAttributes;
	}

	public void setUnmapedAttributes(Set<AggregatedAttribute> unmapedAttributes) {
		this.unmapedAttributes = unmapedAttributes;
	}

	public Map<ReferentielKey, String> getReferentielAttributes() {
		return referentielAttributes;
	}

	public void setReferentielAttributes(Map<ReferentielKey, String> referentielAttributes) {
		this.referentielAttributes = referentielAttributes;
	}









}
