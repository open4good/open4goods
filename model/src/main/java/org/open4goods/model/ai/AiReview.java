package org.open4goods.model.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class AiReview {
	
	private String description;
	
	private String pros;
	
	private String cons;
	
	private String review;
	
	private String dataQuality;
	
	private List<AiSource> sources = new ArrayList<>();

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPros() {
		return pros;
	}

	public void setPros(String pros) {
		this.pros = pros;
	}

	public String getCons() {
		return cons;
	}

	public void setCons(String cons) {
		this.cons = cons;
	}

	public String getReview() {
		return review;
	}

	public void setReview(String review) {
		this.review = review;
	}

	public String getDataQuality() {
		return dataQuality;
	}

	public void setDataQuality(String dataQuality) {
		this.dataQuality = dataQuality;
	}

	public List<AiSource> getSources() {
		return sources;
	}

	public void setSources(List<AiSource> sources) {
		this.sources = sources;
	}



	
	
		
}
