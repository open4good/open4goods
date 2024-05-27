package org.open4goods.api.config.yml;

public class IcecatCompletionConfig {

	private String iceCatUrlPrefix="https://live.icecat.biz/api?UserName=openIcecat-live&Language=fr&GTIN=";
	private Integer politnessDelayMs=500;
	public String getIceCatUrlPrefix() {
		return iceCatUrlPrefix;
	}
	public void setIceCatUrlPrefix(String iceCatUrlPrefix) {
		this.iceCatUrlPrefix = iceCatUrlPrefix;
	}
	public Integer getPolitnessDelayMs() {
		return politnessDelayMs;
	}
	public void setPolitnessDelayMs(Integer politnessDelayMs) {
		this.politnessDelayMs = politnessDelayMs;
	}
	
	
}
