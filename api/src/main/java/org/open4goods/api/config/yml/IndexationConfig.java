package org.open4goods.api.config.yml;

public class IndexationConfig {
	
	/**
	 * Max size of the blocking queue
	 */
	int queueMaxSize = 5000;
	
	/**
	 * Bulk size  (applied for update, means on fetching and on insertion in elastic cluster)
	 */
	
	int bulkPageSize = 150;
	
	/**
	 * Number ofconccurent workers
	 */
	int workers = 2;
	
	/**
	 * Duration of pause when no elements in the queue
	 */
	int pauseDuration = 4000;
	
	
	public int getBulkPageSize() {
		return bulkPageSize;
	}
	public void setBulkPageSize(int dequeueSize) {
		this.bulkPageSize = dequeueSize;
	}
	public int getWorkers() {
		return workers;
	}
	public void setWorkers(int workers) {
		this.workers = workers;
	}
	public int getPauseDuration() {
		return pauseDuration;
	}
	public void setPauseDuration(int pauseDuration) {
		this.pauseDuration = pauseDuration;
	}
	public int getQueueMaxSize() {
		return queueMaxSize;
	}
	public void setQueueMaxSize(int queueMaxSize) {
		this.queueMaxSize = queueMaxSize;
	}
	


}
