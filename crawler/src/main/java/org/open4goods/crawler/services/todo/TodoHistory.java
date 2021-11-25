package org.open4goods.crawler.services.todo;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * *An history of todo
 * @author goulven
 *
 */
public class TodoHistory {
	// The total durations of work per category
	private Map<String, Double>  estimatedDurations = new HashMap<>();
	
	private Map<String, Integer>  tasksCount = new HashMap<>();
	
	private Integer totalTasks = 0;
	
	private Double totalDuration = 0.0;

	private Date date;
	
	public Map<String, Double> getEstimatedDurations() {
		return estimatedDurations;
	}

	public void setEstimatedDurations(Map<String, Double> estimatedDurations) {
		this.estimatedDurations = estimatedDurations;
	}

	public Map<String, Integer> getTasksCount() {
		return tasksCount;
	}

	public void setTasksCount(Map<String, Integer> tasksCount) {
		this.tasksCount = tasksCount;
	}

	public Integer getTotalTasks() {
		return totalTasks;
	}

	public void setTotalTasks(Integer totalTasks) {
		this.totalTasks = totalTasks;
	}

	public Double getTotalDuration() {
		return totalDuration;
	}

	public void setTotalDuration(Double totalDuration) {
		this.totalDuration = totalDuration;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}
	
	
	
}