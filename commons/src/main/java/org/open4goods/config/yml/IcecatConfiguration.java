package org.open4goods.config.yml;

public class IcecatConfiguration {

	private String featuresListFileUri;
	private String categoryFeatureListFileUri;
	private String languageListFileUri;
	private String user;
	private String password;
	
	
	
	public String getFeaturesListFileUri() {
		return featuresListFileUri;
	}

	public void setFeaturesListFileUri(String featuresListFileUri) {
		this.featuresListFileUri = featuresListFileUri;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLanguageListFileUri() {
		return languageListFileUri;
	}

	public void setLanguageListFileUri(String languagesListFileUri) {
		this.languageListFileUri = languagesListFileUri;
	}

	public String getCategoryFeatureListFileUri() {
		return categoryFeatureListFileUri;
	}

	public void setCategoryFeatureListFileUri(String categoryFeatureListFileUri) {
		this.categoryFeatureListFileUri = categoryFeatureListFileUri;
	}


	
	
}
