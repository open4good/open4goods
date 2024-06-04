package org.open4goods.model.data;

import java.util.HashSet;
import java.util.Set;

public class Brand {

	private String name;

	private Set<String> aka = new HashSet<String>();

	private String logo;;

	
	@Override
	public String toString() {
		return name;
	}
	
	public Brand(String name) {
		this.name = name;
	}
	
	public Brand() {
		super();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<String> getAka() {
		return aka;
	}

	public void setAka(Set<String> aka) {
		this.aka = aka;
	}


	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	
}
