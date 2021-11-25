package org.open4goods.model.product;

public class AggregatedFeature {
	
	private String name;
	

	
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {

		if (obj instanceof AggregatedFeature) {
			return name.equals(((AggregatedFeature)obj).name);
		}
		return false;
	}
	
	
	public AggregatedFeature(String name) {
		super();
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	
	
	
	
	
	

}
