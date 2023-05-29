package org.open4goods.model;

public class CacheResourceConfig {

	private String url;

	private Boolean unzip = false;

	//	private Boolean untar = false;

	private Integer refreshInDays = 7;

	/**
	 * If set, will be used to target file to cache if multiple extracted files
	 */
	private String extractedFileName;



	//	public String getId() {
	//		return IdHelper.getHashedName(getUrl());
	//	}


	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public Boolean getUnzip() {
		return unzip;
	}

	public void setUnzip(final Boolean unzip) {
		this.unzip = unzip;
	}



	public Integer getRefreshInDays() {
		return refreshInDays;
	}

	public void setRefreshInDays(final Integer refreshInDays) {
		this.refreshInDays = refreshInDays;
	}


	public String getExtractedFileName() {
		return extractedFileName;
	}


	public void setExtractedFileName(final String extractedFileName) {
		this.extractedFileName = extractedFileName;
	}




}
