package org.open4goods.ui.config.yml;

import java.util.ArrayList;
import java.util.List;

public class TeamConfig {

	/**
	 * Return the core teamp members
	 */
	private List<TeamConfigMember> cores = new ArrayList<>();
	
	/**
	 * Return the contributor
	 */
	private List<TeamConfigMember> contributors = new ArrayList<>();
	
	
	public List<TeamConfigMember> getCores() {
		return cores;
	}
	public void setCores(List<TeamConfigMember> core) {
		this.cores = core;
	}
	public List<TeamConfigMember> getContributors() {
		return contributors;
	}
	public void setContributors(List<TeamConfigMember> contributors) {
		this.contributors = contributors;
	} 
	
	
	
}
