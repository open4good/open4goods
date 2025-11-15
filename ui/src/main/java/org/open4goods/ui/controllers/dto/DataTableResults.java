package org.open4goods.ui.controllers.dto;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * The Class DataTableResults.
 *
 * @author pavan.solapure
 * @param <T> the generic type
 */
public class DataTableResults<T> {

	/** The draw. */
	private Integer draw;

	/** The records filtered. */
	private Long recordsFiltered;

	/** The records total. */
	private Long recordsTotal;

	/** The list of data objects. */
	@SerializedName("data")
	List<T> data;

	public Integer getDraw() {
		return draw;
	}

	public void setDraw(Integer draw) {
		this.draw = draw;
	}

	public Long getRecordsFiltered() {
		return recordsFiltered;
	}

	public void setRecordsFiltered(Long recordsFiltered) {
		this.recordsFiltered = recordsFiltered;
	}

	public Long getRecordsTotal() {
		return recordsTotal;
	}

	public void setRecordsTotal(Long recordsTotal) {
		this.recordsTotal = recordsTotal;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> listOfDataObjects) {
		data = listOfDataObjects;
	}



}
