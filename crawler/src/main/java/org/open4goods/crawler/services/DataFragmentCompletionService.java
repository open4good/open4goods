
package org.open4goods.crawler.services;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.brand.service.BrandScoreService;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.datafragment.ProviderSupportType;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.helper.IdHelper;
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
	private BrandScoreService brandService;


	public DataFragmentCompletionService(BrandScoreService brandService) {
		this.brandService = brandService;
	}



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
		o.getResources().forEach(r -> r.setDatasourceName(o.getDatasourceName()));

		// If data is a computed affiliation link
		if (!StringUtils.isEmpty(datasourceProperties.getAffiliationLinkPrefix())) {
			o.setAffiliatedUrl(datasourceProperties.getAffiliationLinkPrefix() + URLEncoder.encode(o.getUrl(), StandardCharsets.UTF_8));

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
		
		////////////////////////////////
		// Special hook for brand score handlings.
		// Not the better way, but this is the central point.
		// specific service are triggered here, the datafragment will after classicaly fail because of no gtin, and will be discarded of products 
		// *pipeline*
		///////////////////////////////
		
		if (datasourceProperties.isBrandScore()) {
			o.setBrandFragment(true);
			Attribute score = o.getAttributes().stream().filter(e-> e.getName().equals("SCORE")).findFirst().orElse(null);
			if (null != score) {
				LOGGER.info("Found a brand score for brand {} : {}",o.brand(),score.getRawValue());
                                brandService.addBrandScore(o.brand(), datasourceProperties.getName(), datasourceProperties.getInvertScaleBase(), score.getRawValue(), o.affiliatedUrlIfPossible());
			} else {
				LOGGER.warn("Empty brand score found for brand {} ",o.brand());
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

					// A match : must be delocated from classical attributes to
					// referentiel ones
					//

					if (null == dataFragment.getReferentielAttributes().get(key.toString())) {
						dataFragment.addReferentielAttribute(key.toString(), a.getRawValue());
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
				bId = IdHelper.extractModelFromNames(dataFragment);
			} catch (final InvalidParameterException e) {
				LOGGER.info(e.getMessage());
			}
			if (null != bId) {
				dataFragment.addReferentielAttribute( ReferentielKey.MODEL.toString(), bId);
			}

		}

	}







}
