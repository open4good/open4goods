package org.open4goods.ui.controllers.dto;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The Class DataTableRequest.
 * Thanks pageSize https://www.opencodez.com/java/datatable-with-spring-boot.htm#comments
 * @author pavan.solapure
 */
public class DataTableRequest<T> {

	/** The unique id. */
	private String uniqueId;

	/** The draw. */
	private Integer draw;

	/** The start. */
	private Integer start;

	/** The length. */
	private Integer length;

	/** The search. */
	private String search;

	/** The regex. */
	private boolean regex;

	/** The columns. */
	private List<DataTableColumnSpecs> columns;

	/** The order. */
	private DataTableColumnSpecs order;

	/** The is global search. */
	private boolean isGlobalSearch;

	/**
	 * Instantiates a new data table request.
	 *
	 * @param request the request
	 */
	public DataTableRequest(HttpServletRequest request) {
		prepareDataTableRequest(request);
	}

	/**
	 * Prepare data table request.
	 *
	 * @param request the request
	 */
	private void prepareDataTableRequest(HttpServletRequest request) {

		Enumeration<String> parameterNames = request.getParameterNames();

		if(parameterNames.hasMoreElements()) {

			this.setStart(Integer.parseInt(request.getParameter(PaginationCriteria.PAGE_NO)));
			this.setLength(Integer.parseInt(request.getParameter(PaginationCriteria.PAGE_SIZE)));
			this.setUniqueId(request.getParameter("_"));
			this.setDraw(Integer.valueOf(request.getParameter(PaginationCriteria.DRAW)));

			this.setSearch(request.getParameter("search[value]"));
			this.setRegex(Boolean.parseBoolean(request.getParameter("search[regex]")));

			String orderParameter = request.getParameter("order[0][column]");
			// Theres is an "unsorted" state
			int sortableCol = -1;
			if (null != orderParameter) {				
				 sortableCol = Integer.parseInt(orderParameter);
			} 

			List<DataTableColumnSpecs> columns = new ArrayList<DataTableColumnSpecs>();

			if(!StringUtils.isEmpty(this.getSearch())) {
				this.setGlobalSearch(true);
			}

			maxParamsToCheck = getNumberOfColumns(request);

			for(int i=0; i < maxParamsToCheck; i++) {
				if(null != request.getParameter("columns["+ i +"][data]")
						&& !"null".equalsIgnoreCase(request.getParameter("columns["+ i +"][data]"))
						&& !StringUtils.isEmpty(request.getParameter("columns["+ i +"][data]"))) {
					DataTableColumnSpecs colSpec = new DataTableColumnSpecs(request, i);
					if(i == sortableCol) {
						this.setOrder(colSpec);
					}
					columns.add(colSpec);

					if(!StringUtils.isEmpty(colSpec.getSearch())) {
						this.setGlobalSearch(false);
					}
				}
			}

			if(columns.size()>0) {
				this.setColumns(columns);
			}
		}
	}

	private int getNumberOfColumns(HttpServletRequest request) {
		Pattern p = Pattern.compile("columns\\[[0-9]+\\]\\[data\\]");
		@SuppressWarnings("rawtypes")
		Enumeration params = request.getParameterNames();
		List<String> lstOfParams = new ArrayList<String>();
		while(params.hasMoreElements()){
			String paramName = (String)params.nextElement();
			Matcher m = p.matcher(paramName);
			if(m.matches())	{
				lstOfParams.add(paramName);
			}
		}
		return lstOfParams.size();
	}

	/**
	 * Gets the pagination request.
	 *
	 * @return the pagination request
	 */
	public PaginationCriteria getPaginationRequest() {

		PaginationCriteria pagination = new PaginationCriteria();
		pagination.setFrom(this.getStart());
		pagination.setPageSize(this.getLength());

		SortBy sortBy = null;
		if(null != this.getOrder()) {
			sortBy = new SortBy();
			sortBy.addSort(this.getOrder().getData(), SortOrder.valueOf(this.getOrder().getSortDir().toUpperCase()));
		}

		FilterBy filterBy = new FilterBy();
		filterBy.setGlobalSearch(this.isGlobalSearch());
		for(DataTableColumnSpecs colSpec : this.getColumns()) {
			if(colSpec.isSearchable()) {
				if(!StringUtils.isEmpty(this.getSearch()) || !StringUtils.isEmpty(colSpec.getSearch())) {
					filterBy.addFilter(colSpec.getData(), (this.isGlobalSearch()) ? this.getSearch() : colSpec.getSearch());
				}
			}
		}

		pagination.setSortBy(sortBy);
		pagination.setFilterBy(filterBy);

		return pagination;
	}

	/** The max params pageSize check. */
	private int maxParamsToCheck = 0;

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}



	public Integer getDraw() {
		return draw;
	}

	public void setDraw(Integer draw) {
		this.draw = draw;
	}

	public Integer getStart() {
		return start;
	}

	public void setStart(Integer start) {
		this.start = start;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
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

	public List<DataTableColumnSpecs> getColumns() {
		return columns;
	}

	public void setColumns(List<DataTableColumnSpecs> columns) {
		this.columns = columns;
	}

	public DataTableColumnSpecs getOrder() {
		return order;
	}

	public void setOrder(DataTableColumnSpecs order) {
		this.order = order;
	}

	public boolean isGlobalSearch() {
		return isGlobalSearch;
	}

	public void setGlobalSearch(boolean isGlobalSearch) {
		this.isGlobalSearch = isGlobalSearch;
	}

	public int getMaxParamsToCheck() {
		return maxParamsToCheck;
	}

	public void setMaxParamsToCheck(int maxParamsToCheck) {
		this.maxParamsToCheck = maxParamsToCheck;
	}





}