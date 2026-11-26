package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.helper.IdHelper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;

/**
 * Attribute configurations of a vertical, with the lookup indexes derived from them.
 * <p>
 * A vertical config is shared by the fragment worker and the completion threads. The indexes (by key, by synonym,
 * by Icecat feature id) are built once from {@link #getConfigs()}, on the first lookup and under a lock, then
 * published as unmodifiable maps in a single volatile write: a reader sees either no index or a complete one.
 * {@link #setConfigs(List)} drops the index, so the next lookup rebuilds it from the new list. Changing the list
 * returned by {@link #getConfigs()}, or an {@link AttributeConfig} in it, after the first lookup is not reflected:
 * replace the list with the setter instead.
 */
public class AttributesConfig {


	/**
	 * The specific configs configurations
	 */
	@JsonMerge
	private List<AttributeConfig> configs = new ArrayList<AttributeConfig>();

	/**
	 * If set, the list of configs values making an attribute to be categorized as a "Feature" (must be uppercase)
	 */
	@JsonMerge
	private Set<String> featuredValues = new HashSet<>();


	/**
	 * If set, the list of attributes names to be excluded from attributes. Will not be aggregated in the product data
	 */
	@JsonMerge
	private Set<String> exclusions = new HashSet<>();


	/**
	 * Lookup indexes derived from {@link #configs}, built on the first lookup. Ignored by Jackson: the YAML mapper
	 * reads private fields, and a copy made through YAML must rebuild its own index.
	 */
	@JsonIgnore
	private volatile Index index;

	/**
	 * Guards the build of {@link #index} and its reset by {@link #setConfigs(List)}. A lock rather than
	 * {@code synchronized}, so that virtual threads waiting for the build do not pin their carrier.
	 */
	@JsonIgnore
	private final ReentrantLock indexLock = new ReentrantLock();


	public AttributesConfig(List<AttributeConfig> configs) {
		this.configs = configs;

	}


	public AttributesConfig() {
	}


	/**
	 * Return the attribute config key name for an attribute name. The name is normalized with
	 * {@link IdHelper#normalizeAttributeName(String)}, like the synonyms of the index.
	 * @param value
	 * @return
	 */
	public String getKeyForValue(final String value) {
		return index().keyByValue().get(IdHelper.normalizeAttributeName(value));
	}


	/**
	 * Get all configs synonyms by provider
	 *
	 * @return an unmodifiable map ProviderKey, Synonym, Translated Key
	 */
	public Map<String, Map<String, String>> synonyms() {
		return index().synonymsByProvider();
	}


	/**
	 * Resolve a product attribute against the Icecat feature ids, then the synonyms of its datasources, then the
	 * synonyms shared by all datasources.
	 *
	 * @param attr the product attribute to resolve
	 * @return the matching attribute config, or {@code null}
	 */
	public AttributeConfig resolveFromProductAttribute(ProductAttribute attr) {

		final Index idx = index();
		final String name = IdHelper.normalizeAttributeName(attr.getName());
		AttributeConfig ret = null;

		// 1 - Checking from icecat taxonomy
		for (SourcedAttribute source : attr.getSource()) {

			if (null != source.getIcecatTaxonomyId()) {
				ret = idx.byIcecatFeatureId().get(source.getIcecatTaxonomyId());
			}

			if (null != ret) {
				break;
			}
		}

		// 2 - Looking by specific datasource name
		if (null == ret) {
			for (SourcedAttribute source : attr.getSource()) {
				Map<String, String> specSynonyms = idx.synonymsByProvider().get(source.getDataSourcename());

				if (null != specSynonyms) {
					ret = idx.byKey().get(specSynonyms.get(name));
					if (null != ret) {
						break;
					}
				}
			}
		}

		// 3 - Looking in the all attributes
		if (null == ret) {
				Map<String, String> specSynonyms = idx.synonymsByProvider().get("all");

				if (null != specSynonyms) {
					ret = idx.byKey().get(specSynonyms.get(name));
				}
		}

		return ret;


	}



	/**
	 * Gets an attribute config by it's id
	 * @param key
	 * @return
	 */
	public AttributeConfig getAttributeConfigByKey(final String key) {
		return index().byKey().get(key);
	}

	/**
	 * Return the attributeConfig for an attribute, if any
	 *
	 * @param indexedName
	 * @return
	 */
	public AttributeConfig getConfigFor(final String indexedName) {
		return getAttributeConfigByKey(indexedName);
	}

	/**
	 * Returns the lookup indexes, building them on the first call (double-checked locking on a volatile field).
	 *
	 * @return the complete indexes of the current {@link #configs}
	 */
	private Index index() {
		Index current = index;
		if (current == null) {
			indexLock.lock();
			try {
				current = index;
				if (current == null) {
					current = Index.of(configs);
					index = current;
				}
			} finally {
				indexLock.unlock();
			}
		}
		return current;
	}


	public List<AttributeConfig> getConfigs() {
		return configs;
	}


	/**
	 * Replaces the attribute configs. The lookup indexes are rebuilt from this list on the next lookup.
	 *
	 * @param configs the new attribute configs
	 */
	public void setConfigs(List<AttributeConfig> configs) {
		indexLock.lock();
		try {
			this.configs = configs;
			this.index = null;
		} finally {
			indexLock.unlock();
		}
	}


	public Set<String> getFeaturedValues() {
		return featuredValues;
	}


	public void setFeaturedValues(Set<String> featuredValues) {
		this.featuredValues = featuredValues;
	}


	public Set<String> getExclusions() {
		return exclusions;
	}


	public void setExclusions(Set<String> exclusions) {
		this.exclusions = exclusions;
	}


	/**
	 * Immutable lookup indexes of the attribute configs. The maps are unmodifiable views of {@link HashMap}s, so a
	 * {@code null} lookup key returns {@code null} instead of throwing. When two configs share a synonym or an Icecat
	 * feature id, the last one in the list wins. Synonyms are stored in their
	 * {@link IdHelper#normalizeAttributeName(String)} form, so two synonyms differing only by accents or case share
	 * an entry.
	 *
	 * @param byKey              attribute configs by key
	 * @param synonymsByProvider synonym to key, by datasource name ({@code all} for every datasource)
	 * @param keyByValue         key for each synonym, and for the key of each config that declares synonyms
	 * @param byIcecatFeatureId  attribute configs by Icecat feature id
	 */
	private record Index(Map<String, AttributeConfig> byKey, Map<String, Map<String, String>> synonymsByProvider,
			Map<String, String> keyByValue, Map<Integer, AttributeConfig> byIcecatFeatureId) {

		private static Index of(List<AttributeConfig> configs) {
			final Map<String, AttributeConfig> byKey = new HashMap<>();
			final Map<String, Map<String, String>> synonymsByProvider = new HashMap<>();
			final Map<String, String> keyByValue = new HashMap<>();
			final Map<Integer, AttributeConfig> byIcecatFeatureId = new HashMap<>();

			if (null != configs) {
				for (final AttributeConfig ac : configs) {
					if (null == ac) {
						continue;
					}
					byKey.put(ac.getKey(), ac);

					for (Integer id : ac.icecatFeatureIds()) {
						byIcecatFeatureId.put(id, ac);
					}

					// A config without synonyms must not prevent the lookups by key of the whole vertical.
					if (null == ac.getSynonyms()) {
						continue;
					}
					for (final Entry<String, Set<String>> entry : ac.getSynonyms().entrySet()) {
						Map<String, String> providerSynonyms = synonymsByProvider.computeIfAbsent(entry.getKey(),
								k -> new HashMap<>());
						keyByValue.put(ac.getKey(), ac.getKey());
						keyByValue.put(IdHelper.normalizeAttributeName(ac.getKey()), ac.getKey());
						if (null == entry.getValue()) {
							continue;
						}
						for (final String val : entry.getValue()) {
							// Normalized as DataFragment normalizes incoming attribute names
							final String normalized = IdHelper.normalizeAttributeName(val);
							providerSynonyms.put(normalized, ac.getKey());
							// Also build a reverse map
							keyByValue.put(normalized, ac.getKey());
						}
					}
				}
			}

			synonymsByProvider.replaceAll((provider, values) -> Collections.unmodifiableMap(values));
			return new Index(Collections.unmodifiableMap(byKey), Collections.unmodifiableMap(synonymsByProvider),
					Collections.unmodifiableMap(keyByValue), Collections.unmodifiableMap(byIcecatFeatureId));
		}
	}

}
