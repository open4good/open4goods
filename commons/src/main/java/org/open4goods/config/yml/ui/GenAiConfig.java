package org.open4goods.config.yml.ui;

public class GenAiConfig {
	private boolean enabled = true;
	private int maxPerVerticals = 100;
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public int getMaxPerVerticals() {
		return maxPerVerticals;
	}
	public void setMaxPerVerticals(int maxPerVerticals) {
		this.maxPerVerticals = maxPerVerticals;
	}

	
	
}
