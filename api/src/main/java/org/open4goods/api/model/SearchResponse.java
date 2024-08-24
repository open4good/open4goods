package org.open4goods.api.model;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.data.DataFragment;

public class SearchResponse {


	private List<SearchResult> results = new ArrayList<>();

	private Long recordsTotal = 0L;
	private Long recordsFiltered = 0L;

	public void add(final DataFragment p) {
		final SearchResult r = new SearchResult();
		r.setBrand(p.getReferentielAttributes().get(ReferentielKey.BRAND) );
		r.setBrandUid(p.getReferentielAttributes().get(ReferentielKey.MODEL) );
		r.setDateIndexed(p.getLastIndexationDate());
		r.setPrice(p.getPrice());
		r.setUrl(getAffiliatedUrl(p));
		results.add(r);
	}



	private String getAffiliatedUrl(final DataFragment p) {
		return p.affiliatedUrlIfPossible();
	}

	public List<SearchResult> getResults() {
		return results;
	}
	public void setResults(final List<SearchResult> results) {
		this.results = results;
	}
	public Long getRecordsTotal() {
		return recordsTotal;
	}
	public void setRecordsTotal(final Long recordsTotal) {
		this.recordsTotal = recordsTotal;
	}
	public Long getRecordsFiltered() {
		return recordsFiltered;
	}
	public void setRecordsFiltered(final Long recordsFiltered) {
		this.recordsFiltered = recordsFiltered;
	}

}
