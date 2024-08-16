package org.open4goods.api.config.yml;


public class BackupConfig {
	
	private String xwikiBackupFile;
	
	private String dataBackupFile;

	/**
	 * Location of the file used by import phase
	 */
	private String importProductPath;
	
	
	public String getXwikiBackupFile() {
		return xwikiBackupFile;
	}

	public void setXwikiBackupFile(String xwikiBackupFile) {
		this.xwikiBackupFile = xwikiBackupFile;
	}

	public String getDataBackupFile() {
		return dataBackupFile;
	}

	public void setDataBackupFile(String dataBackupFile) {
		this.dataBackupFile = dataBackupFile;
	}

	public String getImportProductPath() {
		return importProductPath;
	}

	public void setImportProductPath(String importProductPath) {
		this.importProductPath = importProductPath;
	}
	
	
}
