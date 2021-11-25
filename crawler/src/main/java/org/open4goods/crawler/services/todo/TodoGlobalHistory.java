package org.open4goods.crawler.services.todo;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Aggregation of todohistory
 * @author goulven
 *
 */
public class TodoGlobalHistory {
	
	Map<Date,TodoHistory> history = new HashMap<>();

	
	
	public void add(TodoHistory history) {
		this.history.put(history.getDate(), history);
		
	}



	public Map<Date, TodoHistory> getHistory() {
		return history;
	}



	public void setHistory(Map<Date, TodoHistory> history) {
		this.history = history;
	}




	

}