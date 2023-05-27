package org.open4goods.services;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class CapsuleResourceBundle extends ReloadableResourceBundleMessageSource {

	private static final Logger logger = LoggerFactory.getLogger(CapsuleResourceBundle.class);
	
	@Autowired HttpServletRequest servletRequest;
	
	private final Map<String,Long> counters = new ConcurrentHashMap<>();
	
	private final Set<String> unknownKeys = new HashSet<>();

	private final Set<String> failKeys = new HashSet<>();
	
	@Override
	protected String getMessageInternal(String code, Object[] args, Locale locale) {
			
		/////////////////////////
		// Incrementing counters
		/////////////////////////
		logger.debug("Incrementing counters",code);
		Long val = counters.get(code);
		counters.put(code, val == null ? 0L : val +1);			
		
		
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
			return "??"+code+"??";			
		}

		return ret;
				
	}


	
	/**
	 * Clear the i18n counters
	 */
	public void clearCounters() {
		counters.clear();
		unknownKeys.clear();
	}
	
	///////////////////////////
	// Getters
	///////////////////////////	
	
	/**
	 * 
	 * @return the i18n key counters
	 */
	public Map<String, Long> getCounters() {
		return counters;
	}


	/**
	 * 
	 * @return the unknown keys
	 */
	public Set<String> getUnknownKeys() {
		return unknownKeys;
	}
	
	
	/**
	 * 
	 * @return the fail keys
	 */
	public Set<String> getFailKeys() {
		return failKeys;
	}
	
}
