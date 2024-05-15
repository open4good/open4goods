package org.open4goods.api.config.yml;


public class AmazonCompletionConfig {

	private String accessKey;
	private String secretKey;
	private String partnerTag;
	private String host;        
	private String region;
	// Wait time between 2 calls
	private Long sleepDuration = 1000L;
	
	
	public String getAccessKey() {
		return accessKey;
	}
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	public String getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	public String getPartnerTag() {
		return partnerTag;
	}
	public void setPartnerTag(String partnerTag) {
		this.partnerTag = partnerTag;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
	public Long getSleepDuration() {
		return sleepDuration;
	}
	public void setSleepDuration(Long sleepDuration) {
		this.sleepDuration = sleepDuration;
	}
	
	
	
    
}
