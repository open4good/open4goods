package org.open4goods.api.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.data.DataFragment;

public class RealtimeSearchResponse {

	private String query;
	private long queryDate;
	private long duration;

	// The items matching the query but with no gtin's association (id, name)
	private Set<DataFragment> nameMatches = new HashSet<>();

	// The results by gtin's, when matching
	Map<String, Set<DataFragment>> gtinMatches = new HashMap<String, Set<DataFragment>>();


	///////////////
	// UI helper function
	///////////////
	public String name(Set<DataFragment> dfs) {
		for (DataFragment df : dfs) {
			String ret = df.longestName();

			if (!StringUtils.isEmpty(ret)) {
				return ret;
			}
		}
		return "NO-NAME";
	}


	public String brand(Set<DataFragment> dfs) {
		//		return StringUtils.join(dfs.stream().map(e -> e.brand()).collect(Collectors.toSet()));
		return (dfs.stream().filter(e -> e!= null && null != e.brand()) .map(DataFragment::brand).findAny().orElse("NO-BRAND"));
	}


	public String model(Set<DataFragment> dfs) {
		//		return StringUtils.join(dfs.stream().map(e -> e.brandUid()).collect(Collectors.toSet()));
		return (dfs.stream().filter(e -> e!= null && null != e.brandUid()) .map(DataFragment::brandUid).findAny().orElse("NO-MODEL"));
	}







	/**
	 * Constructor
	 *
	 * @param productName
	 */
	public RealtimeSearchResponse(String productName) {
		queryDate = System.currentTimeMillis();
		query = productName;
	}

	public RealtimeSearchResponse() {
	}

	/////////////////////////
	// Helpers with json serial
	////////////////////////

	/**
	 *
	 * @return the total number of results
	 */
	public Integer getTotalResults() {
		Integer ret = nameMatches.size();

		for (Set<DataFragment> c : gtinMatches.values()) {
			ret += c.size();
		}
		return ret;
	}

	/**
	 *
	 * @return true if all the datafragments relate to only one gtin
	 */
	public boolean isMonoGtin() {
		return gtinMatches.size() <= 1;
	}

	/////////////////////////
	// Helpers
	////////////////////////
	public boolean empty() {
		return getTotalResults() == 0;
	}


	/**
	 * Straightforward method to retrieve the gtin we have much data for
	 *
	 * @return
	 */
	public String bestGtin() {
		int bestInt = Integer.MIN_VALUE;
		String bestGtin = null;

		for (Entry<String, Set<DataFragment>> set : gtinMatches.entrySet()) {

			if (set.getValue().size() > bestInt) {
				bestInt = set.getValue().size();
				bestGtin = set.getKey();
			}
		}

		return bestGtin;
	}

	/**
	 * Complete the time
	 *
	 * @return
	 */
	public RealtimeSearchResponse done() {
		duration = System.currentTimeMillis() - queryDate;
		return this;
	}

	/**
	 *
	 * @return All datafragments in this query
	 */
	public Stream<DataFragment> all() {
		return Stream.concat(nameMatches.stream(), gtinMatches.values().stream().flatMap(Set::stream));
	}

	/**
	 * All datafragments relativ to a given gtin (+ the byNames gtins)
	 *
	 * @param gtin
	 * @return
	 */
	public Stream<DataFragment> all(String gtin) {

		if (!gtinMatches.containsKey(gtin)) {
			return nameMatches.stream();
		} else {
			return Stream.concat(nameMatches.stream(), gtinMatches.get(gtin).stream());
		}
	}

	/////////////////////////
	// Getters and setters
	////////////////////////
	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public long getQueryDate() {
		return queryDate;
	}

	public void setQueryDate(long queryDate) {
		this.queryDate = queryDate;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public Set<DataFragment> getNameMatches() {
		return nameMatches;
	}

	public void setNameMatches(Set<DataFragment> nameMatches) {
		this.nameMatches = nameMatches;
	}

	public Map<String, Set<DataFragment>> getGtinMatches() {
		return gtinMatches;
	}

	public void setGtinMatches(Map<String, Set<DataFragment>> gtinMatches) {
		this.gtinMatches = gtinMatches;
	}


}
