package org.open4goods.ui.controllers.ui;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.search.sort.SortOrder;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.controllers.dto.DataTableRequest;
import org.open4goods.ui.controllers.dto.DataTableResults;
import org.open4goods.ui.controllers.dto.PaginationCriteria;
import org.open4goods.ui.controllers.dto.VerticalSearchRequest;
import org.open4goods.ui.controllers.dto.VerticalSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;



/**
 * @author pavan.solapure
 *
 */
@RestController
public class DataTableController {

	@Autowired
	private org.open4goods.ui.services.SearchService searchService;
	
	private @Autowired VerticalsConfigService verticalService;

	
	@RequestMapping(value="/{vertical:[a-z-]+}/paginated", method=RequestMethod.GET)
	public DataTableResults<AggregatedData> listUsersPaginated(@PathVariable(name = "vertical") String vertical, HttpServletRequest request, HttpServletResponse response) {
		
		DataTableRequest<AggregatedData> dataTableInRQ = new DataTableRequest<AggregatedData>(request);
		PaginationCriteria pagination = dataTableInRQ.getPaginationRequest();
		
		VerticalConfig vConfig = verticalService.getLanguageForVerticalPath(vertical);
		if (null == vConfig) {
			// TODO : Raise 404
			return null;
		}

		VerticalSearchRequest vRequest = new VerticalSearchRequest();
		vRequest.setPageNumber(pagination.getFrom() / pagination.getPageSize() );
		vRequest.setPageSize( pagination.getPageSize() );

		
		// Sorting
		if (!pagination.isSortByEmpty()) {
			String sortKey = pagination.getSortBy().getSortBys().keySet().stream().findFirst().get();
			SortOrder sortOrder = SortOrder.valueOf(pagination.getSortBy().getSortBys().get(sortKey).toString());

			vRequest.setSortField(sortKey);
			vRequest.setSortOrder(sortOrder);
		}
		
		
		
		
		VerticalSearchResponse vResults = searchService.verticalSearch(vConfig, vRequest);
		
		DataTableResults<AggregatedData> dataTableResult = new DataTableResults<AggregatedData>();
		dataTableResult.setDraw(dataTableInRQ.getDraw());
		dataTableResult.setData(vResults.getData());

		dataTableResult.setRecordsTotal(vResults.getTotalResults());
		dataTableResult.setRecordsFiltered(vResults.getTotalResults());

		
		
		
		return dataTableResult;
	}
}