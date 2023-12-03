package org.open4goods.ui.controllers.dto;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;


/**
 * The Class PaginationCriteria.
 */
public class PaginationCriteria {

	/** The page number. */
	private Integer from;

	/** The page size. */
	private Integer pageSize;

	/** The total records. */
	private Integer totalRecords;

	/** The sort by. */
	private SortBy sortBy;

	/** The filter by. */
	private FilterBy filterBy;


	/**
	 * Gets the page number.
	 *
	 * @return the from
	 */
	public Integer getFrom() {
		return (null == from) ? 0 : from;
	}

	/**
	 * Sets the page number.
	 *
	 * @param from the from pageSize set
	 */
	public void setFrom(Integer pageNumber) {
		from = pageNumber;
	}

	/**
	 * Gets the page size.
	 *
	 * @return the pageSize
	 */
	public Integer getPageSize() {
		return (null == pageSize) ? 10 : pageSize;
	}

	/**
	 * Sets the page size.
	 *
	 * @param pageSize the pageSize pageSize set
	 */
	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * Gets the total records.
	 *
	 * @return the totalRecords
	 */
	public Integer getTotalRecords() {
		return totalRecords;
	}

	/**
	 * Sets the total records.
	 *
	 * @param totalRecords the totalRecords pageSize set
	 */
	public void setTotalRecords(Integer totalRecords) {
		this.totalRecords = totalRecords;
	}

	/**
	 * Gets the sort by.
	 *
	 * @return the sortBy
	 */
	public SortBy getSortBy() {
		return sortBy;
	}

	/**
	 * Sets the sort by.
	 *
	 * @param sortBy the sortBy pageSize set
	 */
	public void setSortBy(SortBy sortBy) {
		this.sortBy = sortBy;
	}

	/**
	 * Gets the filter by.
	 *
	 * @return the filterBy
	 */
	public FilterBy getFilterBy() {
		return filterBy;
	}

	/**
	 * Sets the filter by.
	 *
	 * @param filterBy the filterBy pageSize set
	 */
	public void setFilterBy(FilterBy filterBy) {
		this.filterBy = filterBy;
	}

	/**
	 * Checks if is filter by empty.
	 *
	 * @return true, if is filter by empty
	 */
	public boolean isFilterByEmpty() {
        return null == filterBy || null == filterBy.getMapOfFilters() || filterBy.getMapOfFilters().size() == 0;
    }

	/**
	 * Checks if is sort by empty.
	 *
	 * @return true, if is sort by empty
	 */
	public boolean isSortByEmpty() {
        return null == sortBy || null == sortBy.getSortBys() || sortBy.getSortBys().size() == 0;
    }

	/**
	 * Gets the filter by clause.
	 *
	 * @return the filter by clause
	 */
	public String getFilterByClause() {

		StringBuilder fbsb = null;

		if (!isFilterByEmpty()) {
			Iterator<Entry<String, String>> fbit = filterBy.getMapOfFilters().entrySet().iterator();

			while (fbit.hasNext()) {

				Map.Entry<String, String> pair =  fbit.next();

				if(null == fbsb) {
					fbsb = new StringBuilder();
					fbsb.append(BRKT_OPN);

					fbsb.append(SPACE)
					.append(BRKT_OPN)
					.append(pair.getKey())
					.append(LIKE_PREFIX)
					.append(pair.getValue())
					.append(LIKE_SUFFIX)
					.append(BRKT_CLS);

				} else {

					fbsb.append(filterBy.isGlobalSearch() ? OR : AND)
					.append(BRKT_OPN)
					.append(pair.getKey())
					.append(LIKE_PREFIX)
					.append(pair.getValue())
					.append(LIKE_SUFFIX)
					.append(BRKT_CLS);

				}
			}
			fbsb.append(BRKT_CLS);
		}

		return (null == fbsb) ? BLANK : fbsb.toString();
	}

	/**
	 * Gets the order by clause.
	 *
	 * @return the order by clause
	 */
	public String getOrderByClause() {

		StringBuilder sbsb = null;

		if(!isSortByEmpty()) {
			Iterator<Entry<String, SortOrder>> sbit = sortBy.getSortBys().entrySet().iterator();

			while (sbit.hasNext()) {
				Map.Entry<String, SortOrder> pair =  sbit.next();
				if(null == sbsb) {
					sbsb = new StringBuilder();
					sbsb.append(ORDER_BY).append(pair.getKey()).append(SPACE).append(pair.getValue());
				} else {
					sbsb.append(COMMA).append(pair.getKey()).append(SPACE).append(pair.getValue());
				}
			}
		}

		return (null == sbsb) ? BLANK : sbsb.toString();
	}

	/** The Constant BLANK. */
	private static final String BLANK = "";

	/** The Constant SPACE. */
	private static final String SPACE = " ";

	/** The Constant LIKE_PREFIX. */
	private static final String LIKE_PREFIX = " LIKE '%";

	/** The Constant LIKE_SUFFIX. */
	private static final String LIKE_SUFFIX = "%' ";

	/** The Constant AND. */
	private static final String AND = " AND ";

	/** The Constant OR. */
	private static final String OR = " OR ";

	/** The Constant ORDER_BY. */
	private static final String ORDER_BY = " ORDER BY ";

	private static final String BRKT_OPN = " ( ";

	private static final String BRKT_CLS = " ) ";

	/** The Constant COMMA. */
	private static final String COMMA = " , ";

	/** The Constant PAGE_NO. */
	public static final String PAGE_NO = "start";

	/** The Constant PAGE_SIZE. */
	public static final String PAGE_SIZE = "length";

	/** The Constant DRAW. */
	public static final String DRAW = "draw";

}