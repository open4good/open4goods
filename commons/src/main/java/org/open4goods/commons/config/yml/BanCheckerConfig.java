package org.open4goods.commons.config.yml;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for ban checking
 */
public class BanCheckerConfig {
	/**
	 * List of IP to reject with a 403
	 */
	private Set<String> ips = new HashSet<>();
	/**
	 * List of uas to reject with a 403
	 */

	public Set<String> getIps() {
		return ips;
	}
	public void setIps(Set<String> ips) {
		this.ips = ips;
	}

	

}
