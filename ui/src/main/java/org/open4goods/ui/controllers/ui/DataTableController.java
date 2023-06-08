package org.open4goods.ui.controllers.ui;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.dto.NumericRangeFilter;
import org.open4goods.model.dto.VerticalSearchRequest;
import org.open4goods.model.dto.VerticalSearchResponse;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.controllers.dto.DataTableRequest;
import org.open4goods.ui.controllers.dto.DataTableResults;
import org.open4goods.ui.controllers.dto.PaginationCriteria;
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
	private org.open4goods.services.SearchService searchService;

	private @Autowired VerticalsConfigService verticalService;


	@RequestMapping(value="/{vertical:[a-z-]+}/paginated", method=RequestMethod.GET)
	public DataTableResults<Product> listUsersPaginated(@PathVariable(name = "vertical") String vertical, HttpServletRequest request, HttpServletResponse response) {


		DataTableRequest<Product> dataTableInRQ = new DataTableRequest<Product>(request);
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
		if (null != slidersValue) {
			
			for (String slider : slidersValue) {
				String[] sliderValues = slider.split(":");
	
				Double min = Double.valueOf(Math.floor(Double.parseDouble(sliderValues[1])));
				Double max = Double.valueOf(Math.ceil(Double.parseDouble(sliderValues[2]) ));
				vRequest.getNumericFilters().add(new NumericRangeFilter(sliderValues[0] ,min, max));
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

		DataTableResults<Product> dataTableResult = new DataTableResults<Product>();
		dataTableResult.setDraw(dataTableInRQ.getDraw());
		dataTableResult.setData(vResults.getData());

		dataTableResult.setRecordsTotal(vResults.getTotalResults());
		dataTableResult.setRecordsFiltered(vResults.getTotalResults());

		return dataTableResult;
	}
}