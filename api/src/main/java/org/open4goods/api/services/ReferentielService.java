
package org.open4goods.api.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.model.Referentiel;
import org.open4goods.config.yml.ui.VerticalProperties;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.UnindexedKeyValTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * This service is in charge of building referentiel
 *
 * @author goulven
 *
 */
public class ReferentielService {

	private static final Logger logger = LoggerFactory.getLogger(ReferentielService.class);
	private Logger unassociatedLogger = null;
	private final String logsFolder;

	public ReferentielService(final String logFolder) {
		super();
		logsFolder = logFolder;
		unassociatedLogger = GenericFileLogger.initLogger("referentiel-unassociated", Level.INFO, logFolder, false);
	}

	/**
	 * In this first pass, we find all primary keys from offers where this attribute
	 * is set. We also build the secondary keys, when both primary and one of
	 * secondary key is known
	 *
	 * @param config
	 * @param stats
	 * @return
	 */
	public Referentiel buildReferentiel(final Collection<DataFragment> fragments, final VerticalProperties segmentProperties) {
		final Referentiel r = new Referentiel(logsFolder);

		// 1 - First pass on all products :
		// >> A Map<Long>,Set<String> to maintain GTIN > MODEL_UID
		// 2 : Reverse the map. Alert if MODEL erasure

		r.buildGtinReferentiel(fragments,segmentProperties);

		return r;
	}

	/**
	 * Apply attributes sanitisation rules
	 * @param dedicatedLogger
	 * @param DataFragment
	 * @param conf
	 */
	public static String sanitizeModel(String val, final VerticalProperties config, final Logger dedicatedLogger) {

		//////////////////////////////////
		// Replacing with replacement chars
		//////////////////////////////////
		// NOTE(gof) : performance could be improved
		for (final String repl : config.getModelTokensRemovals()) {
			val = val.replace(repl, "");
		}

		///////////////////////////////////////////////////
		// Testing the brandUid match expression criteria
		///////////////////////////////////////////////////

		final Set<String> brandUids = IdHelper.extractBrandUids(val);


		if (brandUids.size() != 1) {
			dedicatedLogger.info("{} Could not validate MODEL (through extraction pattern)", val);
			return null;
		}

		//TODO(bug,P1,0.25) : Skip if not the same. Ex : Extracted brandUid mismatch : 32HB4T62 <> 32HB4T62  FULL HD
		if (!brandUids.iterator().next().equals(val)) {
			dedicatedLogger.warn("Extracted brandUid mismatch : {} <> {}",brandUids.iterator().next(),val);
		}

		return brandUids.iterator().next();

	}

	public boolean sanitizeBrandUid(final DataFragment dataFragment, final VerticalProperties config) {

		final Attribute a = dataFragment.getAttribute(ReferentielKey.MODEL.toString());

		///////////////////////////////////
		// Standardisation : brandUid
		///////////////////////////////////

		if (null != a) {
			String val = IdHelper.sanitize(a.getRawValue().toString().toUpperCase()).trim();

			if (StringUtils.isEmpty(val)) {
				logger.info("{} will be removed because MODEL is empty after trimming", dataFragment);
				return false;
			}

			// Applying rules

			//////////////////////////////////
			// 1 - Remove brand from brand Uid
			//////////////////////////////////
			final Attribute brand = dataFragment.getAttribute(ReferentielKey.BRAND.toString());

			if (null != brand && val.startsWith(brand.getRawValue().toString().toUpperCase())) {
				logger.info("Removing BRAND from MODEL");
				val = val.substring(brand.getRawValue().toString().length());
			}


			final String newVal = sanitizeModel(val, config,unassociatedLogger);

			if (!StringUtils.isEmpty(newVal)) {

				if (!newVal.equals(val)) {
					// Adding the old branduid as alternate id
					dataFragment.getAlternateIds().add(new UnindexedKeyValTimestamp(ReferentielKey.BRAND.toString(),val));
				}
				a.setRawValue(newVal);
				dataFragment.addReferentielAttribute(ReferentielKey.MODEL.toString(), newVal);
			} else {
				logger.debug("empty brandUiid");
				return false;
			}
		}

		//		logger.info("No brandUid in {}", dataFragment);
		return true;
	}


	public Map<Long, Set<DataFragment>> buildOffers(final Set<DataFragment> fragments, final Referentiel r) {

		final Map<Long, Set<DataFragment>> ret = new HashMap<>();

		final AtomicLong  associated = new AtomicLong();
		final AtomicLong unAssociated = new AtomicLong();



		for (final DataFragment df : fragments) {

			final Long gtin = r.getGtin(df);

			if (null == gtin) {
				unassociatedLogger.warn("Unassociated product  {}", df);
				unAssociated.incrementAndGet();
			} else {
				//				logger.info("Gtin retrieved for {}", p);
				associated.incrementAndGet();

				if (!ret.containsKey(gtin)) {
					ret.put(gtin, new HashSet<>());
				}
				ret.get(gtin).add(df);
			}
		}

		logger.info("End offers associations. {} are associated, {} are not",associated.get(),unAssociated.get());
		return ret;
	}

}
