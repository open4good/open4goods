package org.open4goods.commons.config.yml;

public class RemoteCacheFileConfig {

	private String importFile;

	private Integer daysInCache = 100;

	public String getImportFile() {
		return importFile;
	}

	public void setImportFile(final String importFile) {
		this.importFile = importFile;
	}

	public Integer getDaysInCache() {
		return daysInCache;
	}

	public void setDaysInCache(final Integer daysInCache) {
		this.daysInCache = daysInCache;
	}



}
