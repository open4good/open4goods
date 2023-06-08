
package org.open4goods.crawler.services;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.ProviderSupportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service is in charge of dataFragment marking and extraction
 *
 * @author goulven
 *
 */
public class DataFragmentCompletionService {


	private static final Logger LOGGER = LoggerFactory.getLogger(DataFragmentCompletionService.class);


	/**
	 * DataFragment completion
	 * @param o
	 * @param datasourceProperties
	 * @param dedicatedLogger
	 */
	public void complete(final DataFragment o, final String datasourceConfigName, final DataSourceProperties datasourceProperties, final Logger dedicatedLogger) {

		if (null == o) {
			LOGGER.error("Cannot complete a null dataFragment");
			return;
		}
		// Setting the datasourceName
		o.setDatasourceName(datasourceProperties.getName());

		// Setting the last update date		
		long currentTimeMillis = System.currentTimeMillis();

		// Last indexation date
		o.setLastIndexationDate(currentTimeMillis);

		// If has a price, ensure price has the date
		if (null != o.getPrice() && null == o.getPrice().getTimeStamp()) {
			o.getPrice().setTimeStamp(currentTimeMillis);
		}
		
		
		// Setting the creation date
		o.setCreationDate(currentTimeMillis);
		
//		// Setting the data type
//		o.setProviderType(datasourceProperties.getType());

		// Sets the datasourceConfigType
		if (null != datasourceProperties.getCsvDatasource()) {
			o.setProviderSupportType(ProviderSupportType.CSV);
		} else if (null != datasourceProperties.getWebDatasource()) {
			o.setProviderSupportType(ProviderSupportType.WEB);
		} else if (null != datasourceProperties.getApiDatasource()) {
			o.setProviderSupportType(ProviderSupportType.API);
		}
		else {
			LOGGER.error("Cannot determine support type for {}", o);
		}

		// Sets the datasource config name
		o.setDatasourceConfigName(datasourceConfigName);

		// Set the referentielData flag
		o.setReferentielData(datasourceProperties.getReferentiel());

		// Extract referentiel attributes
		extractReferentielAttributes(o, datasourceProperties,dedicatedLogger);

		// Apply sanitisation rules
		extractBrandUid(o, datasourceProperties,dedicatedLogger);

		// Tagging resources
		o.getResources().stream().forEach(d -> d.addTag(o.getDatasourceName()));

		// If data is a computed affiliation link
		if (!StringUtils.isEmpty(datasourceProperties.getAffiliationLinkPrefix())) {
			o.setAffiliatedUrl(datasourceProperties.getAffiliationLinkPrefix() + URLEncoder.encode(o.getUrl()));

			// The suffix
			if (!StringUtils.isEmpty(datasourceProperties.getAffiliationLinkSuffix())) {
				o.setAffiliatedUrl(o.getAffiliatedUrl() +  datasourceProperties.getAffiliationLinkSuffix());
			}

		}

		// Default product state if defined
		if (null != datasourceProperties.getDefaultItemCondition()) {
			if (null == o.getProductState()) {
				o.setProductState(datasourceProperties.getDefaultItemCondition());
			}
		}
	
	}




	public void extractReferentielAttributes(final DataFragment dataFragment, final DataSourceProperties datasourceConfig, final Logger dedicatedLogger) {

		////////////////////////
		// Extracting classical vendorUid
		// from names
		////////////////////////

		final Iterator<Attribute> attrIterator = dataFragment.getAttributes().iterator();

		final Set<Attribute> toRemove = new HashSet<>();

		while (attrIterator.hasNext()) {
			final Attribute a = attrIterator.next();

			try {

				final String nu = a.getName().toUpperCase();
				final ReferentielKey key = DataSourceProperties.getDefaultReferentielAttributes(). get(nu);

				if (null != key) {

					if (a.multivalued()) {
						dedicatedLogger.warn("Referentiel attributes translation can only apply to mono valued. Problem with {}",
								a);
						continue;
					}
					// A match : must be delocated from classical attributes to
					// referentiel ones
					//

					if (null == dataFragment.getReferentielAttributes().get(key.toString())) {
						dataFragment.addReferentielAttribute(key.toString(), a.getRawValue().toString());
						dedicatedLogger.info("Adding referentiel attribute {}:{} from attribute {}",key,a.getRawValue(),a.getName());
					} else {
						dedicatedLogger.info("Referentiel attribute {}:{} not added because attr with value {} already exists ",key,a.getRawValue(),a.getName(),dataFragment.getReferentielAttributes().get(key));
					}

				} else if (nu.contains("ean") || nu.contains("gtin") || nu.contains("gencod")) {
					dedicatedLogger.warn("Could consider adding referentiel attribute {}:{} from attribute name {}",key,a.getRawValue(),a.getName());
				}
			} catch (final NoSuchElementException e) {
				dedicatedLogger.warn("Missing value when extracting referentiel attributes : {} > {} ", a.getName(),
						e.getMessage());
			}
		}

		// Removing referentiel attributes
		dataFragment.getAttributes().removeAll(toRemove);

	}

	/**
	 * Apply attributes sanitisation rules
	 *
	 * @param dataFragment
	 * @param conf
	 * @param dedicatedLogger
	 */
	public void extractBrandUid(final DataFragment dataFragment, final DataSourceProperties conf, final Logger dedicatedLogger) {
		final Attribute a = dataFragment.getAttribute(ReferentielKey.MODEL.toString());

		// ////////////////////////////////
		// // Extracting the brand Uid
		// ////////////////////////////////

		if (conf.getExtractBrandUidFromName()) {

			if (null != a) {
				dedicatedLogger.warn("Already have MODEL {}, will skip extraction of this attribute from the name",a.getRawValue());
				return;
			}

			String bId = null;
			try {
				bId = IdHelper.extractBrandUidsFromNames(dataFragment);
			} catch (final InvalidParameterException e) {
				LOGGER.info(e.getMessage());
			}
			if (null != bId) {
				dataFragment.addReferentielAttribute( ReferentielKey.MODEL.toString(), bId);
			}

		}

	}







}
