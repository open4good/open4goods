package org.open4goods.commons.services;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A really custom reloadable resource bundle. It :
 * <ul>
 * <li> Export texts in tagged span when authenticated users </li>
 * <li> Count tags access (to maintain clean files, without unused values) </li>
 * <li> Listen for product and context obects, allowing spel inside properties </li>
 * </ul>
 *
 *
 * @author Goulven.Furet
 *
 */
public class ResourceBundle extends ReloadableResourceBundleMessageSource implements HealthIndicator{

	private static final Logger logger = LoggerFactory.getLogger(ResourceBundle.class);

	@Autowired HttpServletRequest servletRequest;


	private final Set<String> unknownKeys = new HashSet<>();


	@Override
	protected String getMessageInternal(String code, Object[] args, Locale locale) {

		/////////////////////////
		// Incrementing counters
		/////////////////////////

		////////////////////////////////////////////
		// 	Getting original 18n translated message
		//////////////////////////////////////////////
		String ret = super.getMessageInternal(code, args, locale);


		///////////////////
		// An unknown key
		///////////////////
		if (null == ret) {
			logger.warn("{} i18n key does not exists",code);
			unknownKeys.add(code);
			return "???"+code+"???";
		}

		return ret;

	}




	/**
	 *
	 * @return the unknown keys
	 */
	public Set<String> getUnknownKeys() {
		return unknownKeys;
	}
	/**
	 * Custom healthcheck, 
	 */
	@Override
	public Health health() {
		
		if (unknownKeys.size() == 0) {
			Builder health = Health.up()
					;
			return health.build();
		} else {
			return Health.down().withDetail("missing_translation", unknownKeys).build();
		}
		
	}

}
