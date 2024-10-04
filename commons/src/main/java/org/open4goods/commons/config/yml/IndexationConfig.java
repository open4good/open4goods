package org.open4goods.commons.config.yml;

public class IndexationConfig {
	
	/**
	 * Max size of the blocking queue for products
	 */
	int productsQueueMaxSize = 15000;

	/**
	 * Max size of the blocking queue for products
	 */
	int datafragmentQueueMaxSize = 50000;

	
	
	
	/**
	 * Bulk size  (applied for update, means on fetching and on insertion in elastic cluster)
	 */
	
	int bulkPageSize = 200;
	
	/**
	 * Number ofconccurent workers
	 */
	int productWorkers = 2;
	
	int dataFragmentworkers = 3;
	
	
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
	public int getProductWorkers() {
		return productWorkers;
	}
	public void setProductWorkers(int workers) {
		this.productWorkers = workers;
	}
	public int getPauseDuration() {
		return pauseDuration;
	}
	public void setPauseDuration(int pauseDuration) {
		this.pauseDuration = pauseDuration;
	}
	public int getProductsQueueMaxSize() {
		return productsQueueMaxSize;
	}
	public void setProductsQueueMaxSize(int queueMaxSize) {
		this.productsQueueMaxSize = queueMaxSize;
	}
	public int getDataFragmentworkers() {
		return dataFragmentworkers;
	}
	public void setDataFragmentworkers(int dataFragmentworkers) {
		this.dataFragmentworkers = dataFragmentworkers;
	}
	public int getDatafragmentQueueMaxSize() {
		return datafragmentQueueMaxSize;
	}
	public void setDatafragmentQueueMaxSize(int datafragmentQueueMaxSize) {
		this.datafragmentQueueMaxSize = datafragmentQueueMaxSize;
	}
	


}
