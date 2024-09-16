package org.open4goods.commons.model;
public class EcoScoreRanking {
	
	// The position in the whole vertical
	private long globalPosition;
	// Number of items in ther vertical when the position was computed
	private long globalCount;
	// The best item id in the whole vertical
	private long globalBest;
	// The better item id in the whole vertical (item just after)
	private long globalBetter;
		
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

}
