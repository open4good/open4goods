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
	
	
}
