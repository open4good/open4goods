package org.open4goods.config.yml;

/**
 * Configuration for an XWiki connexion
 * @author goulven
 *
 */
public class GithubConfiguration {

	  private String accessToken;
	  private String organization;
	  private String repo;
	public String getAccessToken() {
		return accessToken;
	}
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	public String getRepo() {
		return repo;
	}
	public void setRepo(String repo) {
		this.repo = repo;
	}
	  
	  
	  
}