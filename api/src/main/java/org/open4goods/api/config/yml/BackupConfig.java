package org.open4goods.api.config.yml;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@Validated
public class BackupConfig {
	
	/**
	 * Location of the file where xwiki backup must be stored
	 */
	@NotEmpty
	private String xwikiBackupFile;
	
	/**
	 * Location of the folder where products backups files must be stored
	 */
	@NotEmpty
	private String dataBackupFolder;

	/**
	 * Location of the file used by import phase
	 */
	@NotEmpty
	private String importProductPath;
	

	/**
	 * Size of the bulk to use for products importation
	 */
	private  int importBulkSize = 200;

	/**
	 * Number of threads (and of files) to operate export with
	 */
	private int productsExportThreads = 4;

	
	/**
	 * Min size in MB the xwiki backup file should have (Will Raise an healthcheck.down() if this criteria is not met)
	 */
	private int minXwikiBackupFileSizeInMb = 30;
	
	/**
	 * Min size in MB the product backup folder should have (Will Raise an healthcheck.down() if this criteria is not met)
	 */
	
	private int minProductsBackupFolderSizeInMb = 16000;


	/**
	 * Max age the wiki must have, in hours. (Will Raise an healthcheck.down() if this criteria is not met)
	 */
	private int maxWikiBackupAgeInHours = 14;
	

	/**
	 * Max age the products file must have, in hours. (Will Raise an healthcheck.down() if this criteria is not met)
	 */
	private long maxProductsBackupAgeInHours = 1000 * 3600 * 24 * 8;
	
	
	
	
	public String getXwikiBackupFile() {
		return xwikiBackupFile;
	}

	public void setXwikiBackupFile(String xwikiBackupFile) {
		this.xwikiBackupFile = xwikiBackupFile;
	}

	public String getDataBackupFolder() {
		return dataBackupFolder;
	}

	public void setDataBackupFolder(String dataBackupFile) {
		this.dataBackupFolder = dataBackupFile;
	}

	public String getImportProductPath() {
		return importProductPath;
	}

	public void setImportProductPath(String importProductPath) {
		this.importProductPath = importProductPath;
	}

	public int getImportBulkSize() {
		return importBulkSize;
	}

	public void setImportBulkSize(int importBulkSize) {
		this.importBulkSize = importBulkSize;
	}

	public int getProductsExportThreads() {
		return productsExportThreads;
	}

	public void setProductsExportThreads(int productsExportThreads) {
		this.productsExportThreads = productsExportThreads;
	}

	public int getMinXwikiBackupFileSizeInMb() {
		return minXwikiBackupFileSizeInMb;
	}

	public void setMinXwikiBackupFileSizeInMb(int minXwikiBackupFileSizeInMb) {
		this.minXwikiBackupFileSizeInMb = minXwikiBackupFileSizeInMb;
	}

	public int getMinProductsBackupFolderSizeInMb() {
		return minProductsBackupFolderSizeInMb;
	}

	public void setMinProductsBackupFolderSizeInMb(int minProductsBackupFolderSizeInMb) {
		this.minProductsBackupFolderSizeInMb = minProductsBackupFolderSizeInMb;
	}

	public int getMaxWikiBackupAgeInHours() {
		return maxWikiBackupAgeInHours;
	}

	public void setMaxWikiBackupAgeInHours(int maxWikiBackupAgeInHours) {
		this.maxWikiBackupAgeInHours = maxWikiBackupAgeInHours;
	}

	public long getMaxProductsBackupAgeInHours() {
		return maxProductsBackupAgeInHours;
	}

	public void setMaxProductsBackupAgeInHours(long maxProductsBackupAgeInHours) {
		this.maxProductsBackupAgeInHours = maxProductsBackupAgeInHours;
	}
	
	
}
