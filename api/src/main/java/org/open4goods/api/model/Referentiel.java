
package org.open4goods.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.ReferentielService;
import org.open4goods.config.yml.ui.VerticalProperties;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.data.DataFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

import ch.qos.logback.classic.Level;

/**
 * The in-memory referentiel for a given domain
 * @author goulven
 */
public class Referentiel {


	private Logger dedicatedLogger = null;


	public Referentiel(final String logsFolder) {
		super();
		dedicatedLogger = GenericFileLogger.initLogger("referentiel-"+getClass().getSimpleName().toLowerCase(), Level.INFO, logsFolder, false);
	}

	/**
	 * The Map of GTIN<> BrandUid
	 */
	private final Map<Long, Set<String>> gtins = new HashMap<>();

	/**
	 * The Map of GTIN<> BrandUid
	 */
	private final Map<String, Long> brandUids = new HashMap<>();

	private final RadixTree<Long> radixKeys = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());


	private static final Logger logger = LoggerFactory.getLogger(Referentiel.class);


	/**
	 * Performs an efficient startswith (Radix Tree) to find the potential multiple exact (and longest) "graal" refernetiel keys
	 * @param key
	 * @return
	 */
	public Set<Long> getRadixKey(final String key) {

		return StreamSupport.stream(radixKeys.getValuesForKeysStartingWith(key).spliterator(),false).collect(Collectors.toSet());
	}



	public void buildGtinReferentiel(final Collection<DataFragment> fragments, final VerticalProperties segmentProperties) {

		for (final DataFragment dataFragment : fragments) {

			//TODO : This tostring ARE strange....
			final String gtin = dataFragment.getReferentielAttributes().get(ReferentielKey.BRAND.toString());
			final String brandUid = dataFragment.getReferentielAttributes().get(ReferentielKey.MODEL.toString());

			if (!StringUtils.isEmpty(gtin)) {

				if (!StringUtils.isNumeric(gtin)) {
					dedicatedLogger.warn("A non numeric GTIN : {} at {}", gtin, dataFragment.getUrl());
				} else {
					final Long gtinNum = Long.valueOf(gtin);

					if (!gtins.containsKey(gtinNum)) {
						gtins.put(gtinNum, new HashSet<>());
					}

					// Adding classical brandUid
					if (!StringUtils.isEmpty(brandUid)) {
						// Adding BrandUid if set
						gtins.get(gtinNum).add(brandUid);
					}

					// Adding alternate brandUid
					for (final String aId : dataFragment.getAlternateIds()) {
						gtins.get(gtinNum).add(ReferentielService.sanitizeModel(aId, segmentProperties,dedicatedLogger));
					}

				}

			}
		}

		// Reversing the map, to get a brandUid<> Gtin one
		for (final Entry<Long, Set<String>> g : gtins.entrySet()) {

			for (final String bId : g.getValue()) {

				final Long actual = brandUids.get(bId);

				if (null == actual) {

					if (StringUtils.isEmpty(bId) || null == g.getKey()) {
						dedicatedLogger.warn("Cannot add null value in RadixTree for {}:{}",bId, g.getKey());
					} else {
						brandUids.put(bId, g.getKey());
						radixKeys.put(bId, g.getKey());
					}
				} else if (!actual.equals(g.getKey())) {
					dedicatedLogger.warn("Conflict for BRANDUID {} : Could be associated to GTIN {}, {} ", bId, actual,
							g.getKey());
				} else {
					logger.info("Match confirmation for {} : {} ", g.getKey(), g.getValue());
				}
			}
		}

		dedicatedLogger.info("Referentiel ID's Map built ! GTINS's : {}, brandUid : {}", gtins.size(), brandUids.size());

	}

	public Long getGtin(final DataFragment p) {

		final String gtin = p.getReferentielAttributes().get(ReferentielKey.GTIN.toString());
		if (null != gtin && StringUtils.isNumeric(gtin)) {
			//			logger.info("Gtin solved directly for product {}",p);
			return Long.valueOf(gtin);
		}

		// Trying by brandUid

		final String brandUid = p.brandUid();
		final Long byId = brandUids.get(brandUid);

		if (null == byId) {
			//			logger.info("Gtin not solved for {}",p);

			// Trying with a prefix approach


			if (null != brandUid) {
				final Iterable<Long> prefixes = radixKeys.getValuesForKeysStartingWith(brandUid);

				final Set<Long> list = new HashSet<>();
				prefixes.iterator().forEachRemaining(list::add);

				if (list.size() == 1) {
					final Long resolved = list.iterator().next();
					logger.info("Resolution with prefix key : {} >  {}",  brandUid,resolved);
					return resolved;
				} else if (list.size()> 1) {
					logger.warn("Multiple gtins found for {}, {}",  brandUid,StringUtils.join(list,", "));
				}

			}



		} else {
			logger.info("Gtin solved by uid association {}",p);
		}
		return byId;

	}


}
