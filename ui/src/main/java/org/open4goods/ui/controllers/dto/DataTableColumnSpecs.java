package org.open4goods.ui.controllers.dto;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The Class DataTableColumnSpecs.
 *
 * @author pavan.solapure
 */
public class DataTableColumnSpecs {

	/** The index. */
	private int index;

	/** The data. */
	private String data;

	/** The name. */
	private String name;

	/** The searchable. */
	private boolean searchable;

	/** The orderable. */
	private boolean orderable;

	/** The search. */
	private String search;

	/** The regex. */
	private boolean regex;

	/** The sort dir. */
	private String sortDir;


	/**
	 * Instantiates a new data table column specs.
	 *
	 * @param request the request
	 * @param i the i
	 */
	public DataTableColumnSpecs(HttpServletRequest request, int i) {
		setIndex(i);
		prepareColumnSpecs(request, i);
	}


	/**
	 * Prepare column specs.
	 *
	 * @param request the request
	 * @param i the i
	 */
	private void prepareColumnSpecs(HttpServletRequest request, int i) {

		setData(request.getParameter("columns["+ i +"][data]"));
		setName(request.getParameter("columns["+ i +"][name]"));
		setOrderable(Boolean.parseBoolean(request.getParameter("columns["+ i +"][orderable]")));
		setRegex(Boolean.parseBoolean(request.getParameter("columns["+ i +"][search][regex]")));
		setSearch(request.getParameter("columns["+ i +"][search][value]"));
		setSearchable(Boolean.parseBoolean(request.getParameter("columns["+ i +"][searchable]")));

		if (null != request.getParameter("order[0][column]")) {
			int sortableCol = Integer.parseInt(request.getParameter("order[0][column]"));
			String sortDir = request.getParameter("order[0][dir]");
	
			if(i == sortableCol) {
				setSortDir(sortDir);
			}
		}
	}


	public int getIndex() {
		return index;
	}


	public void setIndex(int index) {
		this.index = index;
	}


	public String getData() {
		return data;
	}


	public void setData(String data) {
		this.data = data;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public boolean isSearchable() {
		return searchable;
	}


	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}


	public boolean isOrderable() {
		return orderable;
	}


	public void setOrderable(boolean orderable) {
		this.orderable = orderable;
	}


	public String getSearch() {
		return search;
	}


	public void setSearch(String search) {
		this.search = search;
	}


	public boolean isRegex() {
		return regex;
	}


	public void setRegex(boolean regex) {
		this.regex = regex;
	}


	public String getSortDir() {
		return sortDir;
	}


	public void setSortDir(String sortDir) {
		this.sortDir = sortDir;
	}






}