package org.open4goods.commons.config.yml.datasource;

public class CapuleRobotstxtProperties {

	/**
	 * Should the crawler obey Robots.txt protocol? More info on Robots.txt is
	 * available at http://www.robotstxt.org/
	 */
	private boolean enabled = true;

	/**
	 * user-agent name that will be used to determine whether some servers have
	 * specific rules for this agent name.
	 */
	private String userAgentName = "crawler4j";

	/**
	 * Whether to ignore positive user-agent discrimination. There are websties that use
	 * a white-list system where they explicitly allow Googlebot but disallow all other
	 * bots by a "User-agent: * Disallow: /" rule. Setting this setting to true
	 * will ignore the user-agent and apply the "Allow" rule to all user-agents.
	 * This can still be overridden when a robots.txt explicitly disallows the configured
	 * User-agent, as such a rule supersedes the generic rule.
	 */
	private boolean ignoreUADiscrimination = false;

	/**
	 * The maximum number of hosts for which their robots.txt is cached.
	 */
	private int cacheSize = 500;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public String getUserAgentName() {
		return userAgentName;
	}

	public void setUserAgentName(final String userAgentName) {
		this.userAgentName = userAgentName;
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(final int cacheSize) {
		this.cacheSize = cacheSize;
	}

	public void setIgnoreUADiscrimination(final boolean ignore) {
		ignoreUADiscrimination = ignore;
	}

	public boolean getIgnoreUADiscrimination() {
		return ignoreUADiscrimination;
	}
}
