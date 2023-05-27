package org.open4goods.ui.controllers.ui;

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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

		request.getParameterMap();
		
		// Handle checkboxes values		
		String[] checkboxes = request.getParameterValues("checkboxes[]");
		if (null != checkboxes) {
			for (String checkbox : checkboxes) {
				String attr[] = checkbox.split("-");
								
				// TODO : should have consts shared with javascript (vertical-home)
				if (attr[0].equals("condition")) {
					vRequest.addTermFilter("price.offers.productState.keyword",attr[1]);
				} else if (attr[0].equals("brand")) {
					vRequest.addTermFilter("attributes.referentielAttributes.BRAND.keyword",attr[1]);					
				} else if (attr[0].equals("countries")) {
					vRequest.addTermFilter("gtinInfos.country.keyword",attr[1]);
				}  				
				else {
					vRequest.addTermFilter(attr[0],attr[1]);				
				}
			}
		}
				
		
		
		// Handle numeric sliders value		
		String[] slidersValue = request.getParameterValues("sliders[]");
		for (String slider : slidersValue) {
			String[] sliderValues = slider.split(":");
			
			if (sliderValues[0].equals("slider-price-minPrice-price")) {
				// TODO : should have consts shared with javascrippt (vertical-home)
				vRequest.setMinPrice(Double.valueOf(Math.floor(Double.valueOf(sliderValues[1]).doubleValue())).intValue() );
				vRequest.setMaxPrice(Double.valueOf(Math.ceil(Double.valueOf(sliderValues[2]).doubleValue() )).intValue() );				
			} else if (sliderValues[0].equals("slider-offers")) {
				// TODO : should have consts shared with javascrippt (vertical-home)
				vRequest.setMinOffers(Double.valueOf(Math.floor(Double.valueOf(sliderValues[1]).doubleValue())).intValue() );
				vRequest.setMaxOffers(Double.valueOf(Math.ceil(Double.valueOf(sliderValues[2]).doubleValue() )).intValue() );				
			} else {
				//TODO(gof) : put back when ecoscore computed
				//vRequest.getNumericFilters().add(new NumericRangeFilter(sliderValues[0] , Double.valueOf(sliderValues[1]), Double.valueOf(sliderValues[2])));
			}
		}
		
		// Sorting
		if (!pagination.isSortByEmpty()) {
			String sortKey = pagination.getSortBy().getSortBys().keySet().stream().findFirst().get();
			String sortOrder = pagination.getSortBy().getSortBys().get(sortKey).toString();
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