package org.open4goods.model.vertical;

import java.util.HashMap;
import java.util.Map;

public class ImpactScoreTexts {

	private Map<String,String> criteriasAnalysis = new HashMap<String, String>();
	private String purpose;
	private String criticalReview;
	private String availlableDatas;
	public Map<String, String> getCriteriasAnalysis() {
		return criteriasAnalysis;
	}
	public void setCriteriasAnalysis(Map<String, String> criteriasAnalysis) {
		this.criteriasAnalysis = criteriasAnalysis;
	}
	public String getPurpose() {
		return purpose;
	}
	public void setPurpose(String purpose) {
		this.purpose = purpose;
	}
	public String getCriticalReview() {
		return criticalReview;
	}
	public void setCriticalReview(String criticalReview) {
		this.criticalReview = criticalReview;
	}
	public String getAvaillableDatas() {
		return availlableDatas;
	}
	public void setAvaillableDatas(String availlableDatas) {
		this.availlableDatas = availlableDatas;
	}
	
	
	
}
