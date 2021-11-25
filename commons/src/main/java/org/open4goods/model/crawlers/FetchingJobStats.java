
package org.open4goods.model.crawlers;

public class FetchingJobStats {

	private String name;

	private long startDate;

	private long queueLength = 0;

	private long numberOfProcessedDatas = 0;

	private long numberOfIndexedDatas = 0;

	private boolean finished = false;

	private boolean shuttingDown = false;


	public void incrementProcessed(final int size) {
		numberOfProcessedDatas += size;
	}

	public void incrementProcessed() {
		numberOfProcessedDatas++;
	}

	public void decrementQueue() {
		queueLength--;
	}

	public FetchingJobStats() {
		super();
	}

	public FetchingJobStats(final String name, final long startDate) {
		super();
		this.name = name;
		this.startDate = startDate;
	}


	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(final boolean finished) {
		this.finished = finished;
	}

	public boolean isShuttingDown() {
		return shuttingDown;
	}

	public void setShuttingDown(final boolean shuttingDown) {
		this.shuttingDown = shuttingDown;
	}

	public long getStartDate() {
		return startDate;
	}

	public void setStartDate(final long startDate) {
		this.startDate = startDate;
	}

	public long getQueueLength() {
		return queueLength;
	}

	public void setQueueLength(final long queueLength) {
		this.queueLength = queueLength;
	}

	public long getNumberOfProcessedDatas() {
		return numberOfProcessedDatas;
	}

	public void setNumberOfProcessedDatas(final long numberOfProcessedPages) {
		numberOfProcessedDatas = numberOfProcessedPages;
	}


	public long getNumberOfIndexedDatas() {
		return numberOfIndexedDatas;
	}

	public void setNumberOfIndexedDatas(final long numberOfIndexedPages) {
		numberOfIndexedDatas = numberOfIndexedPages;
	}






}
