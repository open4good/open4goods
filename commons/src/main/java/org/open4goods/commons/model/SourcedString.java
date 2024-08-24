package org.open4goods.commons.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * A kind of string that encapsulates pair if datasources and values
 * @author Goulven.Furet
 * TODO : maintain changes
 */
public class SourcedString {

	/**
	 * Computed with bestValueElection
	 */
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String bestValue;
	
	/**
	 * History, by datasource name
	 */
	
	@Field(index = false, store = false, type = FieldType.Object)
	private Map<String,List<SourcedStringHistory>> history = new HashMap<>();

	
	@Override
	public String toString() {
		return history.size() + " : "+ bestValue ;
	}
	
	
	/**
	 * Shortcut to a unique source
	 * @return
	 */
	public String source() {
		return history.keySet().stream().findFirst().orElse(null);  
	}
	
	/**
	 * Add a value, with historisation, datasource ventilation and best value computing
	 * @param value
	 * @param datasourceName
	 * @param timestamp
	 * @return 
	 */
	public void addValue(String value, String datasourceName, Long timestamp) {
		// Adding to history
		doVersion(value, datasourceName, timestamp);
		
		// compute the best attribute
		doBestValueElection();
	}


	private void doVersion(String value, String datasourceName, Long timestamp) {
		List<SourcedStringHistory> historyItem = history.get(datasourceName);
		
		// Existing item
		if (null != historyItem) {
			
			// Finding history last value
			Optional<SourcedStringHistory> last = historyItem.stream().findFirst();			
			// update history
			if (last.isPresent()) {				
				// Not equal to the previous version
				if (!last.get().getValue().equals(value)) {
						// Adding a version
					historyItem.add(new SourcedStringHistory(value,timestamp));
				} else {
					// Just updating the timestamp
					last.get().setTimestamp(timestamp);
				}
			} else {
				historyItem.add(new SourcedStringHistory(value,timestamp));
			}
		}

		// First time wee see this item
		else {			
			history.get(datasourceName).add(new SourcedStringHistory(value,timestamp));
		}
	}
	
	
	/**
	 * Determine the best value, terms frequency based
	 */
	public void doBestValueElection( ) {		
		String bValue = null;
		int freq = 0;
		for (SourcedStringHistory value : history.values().stream().findFirst().get() ) {
			int count = Collections.frequency(new ArrayList<String>(getAsValueMap().values()), value);			
			if (count > freq) {
				freq = count;
				bValue = value.getValue();
			}
		}		
		this.bestValue = bValue;		
	}
	
	
	/**
	 * Return the last attribute value for a given datasource
	 * @param datasourceNAme
	 * @return
	 */
	public String getValue(String datasourceName ) {
		Optional<SourcedStringHistory> item = history.get(datasourceName).stream().findFirst();		
		if (item.isEmpty()) {
			return null;
		} else {
			return history.get(datasourceName).stream().findFirst().get().getValue();
		}		
	}

	/**
	 * Return the versionned implementation to an handy datasource / value vap
	 * @return 
	 */
	public Map<String, String> getAsValueMap( ) {
		Map<String,String> byIds = new HashMap<>();
		
		for (Entry<String, List<SourcedStringHistory>> provider : history.entrySet()) {
			String val = getValue(provider.getKey());
			byIds.put(provider.getKey() , val);
		}
		return byIds;
	}


	public String getBestValue() {
		return bestValue;
	}


	public void setBestValue(String bestValue) {
		this.bestValue = bestValue;
	}
	
	
}
