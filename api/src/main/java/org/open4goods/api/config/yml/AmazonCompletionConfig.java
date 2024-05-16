package org.open4goods.api.config.yml;


public class AmazonCompletionConfig {

	private String accessKey;
	private String secretKey;
	private String partnerTag;
	private String host;        
	private String region;
	// Wait time between 2 calls
//	https://webservices.amazon.com/paapi5/documentation/troubleshooting/api-rates.html
	private Long sleepDuration = 1100L;	
	private int maxCallsPerBatch = 8640;
	
	
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
	public int getMaxCallsPerBatch() {
		return maxCallsPerBatch;
	}
	public void setMaxCallsPerBatch(int maxCallsPerBatch) {
		this.maxCallsPerBatch = maxCallsPerBatch;
	}
	
	
	
    
}
