package org.open4goods.ui.controllers.ui;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.model.dto.NumericRangeFilter;
import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.ui.controllers.dto.DataTableRequest;
import org.open4goods.ui.controllers.dto.DataTableResults;
import org.open4goods.ui.controllers.dto.PaginationCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(DataTableController.class);
	
	private final org.open4goods.commons.services.SearchService searchService;
	private @Autowired UiService uiService;
	private final VerticalsConfigService verticalService;

	public DataTableController(SearchService searchService, VerticalsConfigService verticalService) {
		this.searchService = searchService;
		this.verticalService = verticalService;
	}


	@RequestMapping(value="/{vertical:[a-z-]+}/paginated", method=RequestMethod.GET)
	public DataTableResults<Product> listUsersPaginated(@PathVariable(name = "vertical") String vertical, HttpServletRequest request, HttpServletResponse response) {


		DataTableRequest<Product> dataTableInRQ = new DataTableRequest<Product>(request);
		PaginationCriteria pagination = dataTableInRQ.getPaginationRequest();

		VerticalConfig vConfig = verticalService.getVerticalForPath(vertical);
		if (null == vConfig) {
			// TODO : Raise 404
			return null;
		}

		VerticalSearchRequest vRequest = new VerticalSearchRequest();
		vRequest.setPageNumber(pagination.getFrom() / pagination.getPageSize() );
		vRequest.setPageSize( pagination.getPageSize() );

		// Handle checkboxes values
		String[] checkboxes = request.getParameterValues("checkboxes[]");
		if (null != checkboxes) {
			for (String checkbox : checkboxes) {
				String[] attr = checkbox.split(":");

				// TODO(p2,design) : should have consts shared with javascript (vertical-home)
                switch (attr[0]) {
                    case "condition" -> vRequest.addTermFilter("price.conditions", attr[1]);
                    case "brand" -> vRequest.addTermFilter("attributes.referentielAttributes.BRAND", attr[1]);
                    case "countries" -> vRequest.addTermFilter("gtinInfos.country", attr[1]);
                    case "trend" -> vRequest.addTermFilter("price.trend", attr[1]);
                    
                    default -> vRequest.addTermFilter(attr[0], attr[1]);
                }
			}
		}



		// Handle numeric sliders value
		String[] slidersValue = request.getParameterValues("sliders[]");
		if (null != slidersValue) {
			
			for (String slider : slidersValue) {
				String[] sliderValues = slider.split(":");
	
				Double min = Double.parseDouble(sliderValues[1]);
				Double max = Double.parseDouble(sliderValues[2]) ;
				Boolean includeUndefined = Boolean.valueOf(sliderValues[3]);
				Double interval;
				try {
					interval = Double.parseDouble(sliderValues[4]);
				} catch (NumberFormatException e) {
					LOGGER.error("Not a parsable interval for {}",slider);
					interval = 50.0;
				}
				
				vRequest.getNumericFilters().add(new NumericRangeFilter(sliderValues[0] ,min, max, interval, includeUndefined));
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