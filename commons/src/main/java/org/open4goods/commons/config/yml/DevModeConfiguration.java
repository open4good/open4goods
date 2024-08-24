package org.open4goods.commons.config.yml;

public class DevModeConfiguration {

	private String devModeProductEndpoint = "https://beta.api.nudger.fr/devmode/products";
	
	public String getDevModeProductEndpoint() {
		return devModeProductEndpoint;
	}
	
	public void setDevModeProductEndpoint(String devModeProductEndpoint) {
		this.devModeProductEndpoint = devModeProductEndpoint;
	}
}
