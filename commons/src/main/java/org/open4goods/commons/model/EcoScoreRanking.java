package org.open4goods.commons.model;
// TODO : Some fields should be computed / rendered (eg. id's), not stored but so handy....
// TODO : Add fields
public class EcoScoreRanking {
	
	// The position in the whole vertical
	private long globalPosition;
	// Number of items in ther vertical when the position was computed
	private long globalCount;
	// The best item id in the whole vertical
	private long globalBest;
	// The better item id in the whole vertical (item just after)
	private long globalBetter;
		
	
	
	
	// The position in the specialisation (eg : big TV)
	private long specializedPosition;
	// Number of items in ther vertical when the position was computed
	private long specializedCount;
	// The better item id in the specialisation
	private String specializedBest;
	// The better item id in the specialisation (item just after)
	private String specializedBetter;

	
	
	public long getGlobalPosition() {
		return globalPosition;
	}
	public void setGlobalPosition(long globalPosition) {
		this.globalPosition = globalPosition;
	}
	public long getGlobalCount() {
		return globalCount;
	}
	public void setGlobalCount(long globalCount) {
		this.globalCount = globalCount;
	}
	public long getSpecializedPosition() {
		return specializedPosition;
	}
	public void setSpecializedPosition(long specializedPosition) {
		this.specializedPosition = specializedPosition;
	}
	public long getSpecializedCount() {
		return specializedCount;
	}
	public void setSpecializedCount(long specializedCount) {
		this.specializedCount = specializedCount;
	}
	
	
	public long getGlobalBest() {
		return globalBest;
	}
	public void setGlobalBest(long globalBest) {
		this.globalBest = globalBest;
	}
	public long getGlobalBetter() {
		return globalBetter;
	}
	public void setGlobalBetter(long globalBetter) {
		this.globalBetter = globalBetter;
	}
	public String getSpecializedBest() {
		return specializedBest;
	}
	public void setSpecializedBest(String specializedBest) {
		this.specializedBest = specializedBest;
	}
	public String getSpecializedBetter() {
		return specializedBetter;
	}
	public void setSpecializedBetter(String specializedBetter) {
		this.specializedBetter = specializedBetter;
	}
		
	
	
		
	
	

}
