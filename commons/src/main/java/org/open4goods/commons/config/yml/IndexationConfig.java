package org.open4goods.commons.config.yml;

public class IndexationConfig {
	
	/**
	 * Max size of the blocking queue for products
	 */
	int productsQueueMaxSize = 5000;
	
	/**
	 * Max size of the blocking queue for partial products
	 */
	int partialProductsQueueMaxSize = 5000;

	/**
	 * Max size of the blocking queue for products
	 */
	int datafragmentQueueMaxSize = 20000;
	
	/**
	 * Bulk size  (applied for update of datafragments, means on fetching and processing in elastic cluster)
	 */
	
	int dataFragmentbulkPageSize = 200;

	int productsbulkPageSize = 200;

	int partialProductsbulkPageSize = 300;
	
	/**
	 * Number of conccurent workers for product (only save in es)
	 */
	int productWorkers = 2;
	
	int partialProductWorkers = 2;
	
	
	/**
	 * Number of concurent workers for datafragments aggregation (means retrieve from cluster, update and save bck)
	 */
	int dataFragmentworkers = 2;
	
	
	/**
	 * Duration of pause when no elements in the queue
	 */
	int pauseDuration = 4000;
	
	
	public int getDataFragmentBulkPageSize() {
		return dataFragmentbulkPageSize;
	}
	public void setDataFragmentBulkPageSize(int dequeueSize) {
		this.dataFragmentbulkPageSize = dequeueSize;
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
	public int getProductsbulkPageSize() {
		return productsbulkPageSize;
	}
	public void setProductsbulkPageSize(int productsbulkPageSize) {
		this.productsbulkPageSize = productsbulkPageSize;
	}
	public int getDataFragmentbulkPageSize() {
		return dataFragmentbulkPageSize;
	}
	public int getPartialProductsQueueMaxSize() {
		return partialProductsQueueMaxSize;
	}
	public void setPartialProductsQueueMaxSize(int partialProductsQueueMaxSize) {
		this.partialProductsQueueMaxSize = partialProductsQueueMaxSize;
	}
	public void setDataFragmentbulkPageSize(int dataFragmentbulkPageSize) {
		this.dataFragmentbulkPageSize = dataFragmentbulkPageSize;
	}
	public int getPartialProductsbulkPageSize() {
		return partialProductsbulkPageSize;
	}
	public void setPartialProductsbulkPageSize(int partialProductsbulkPageSize) {
		this.partialProductsbulkPageSize = partialProductsbulkPageSize;
	}
	public int getPartialProductWorkers() {
		return partialProductWorkers;
	}
	public void setPartialProductWorkers(int partialProductWorkers) {
		this.partialProductWorkers = partialProductWorkers;
	}
	


}
