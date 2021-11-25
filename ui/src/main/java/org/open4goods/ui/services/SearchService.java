package org.open4goods.ui.services;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.ui.controllers.dto.VerticalSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service in charge of the search in AggregatedDatas and in DataFragments
 *
 * 
 * @author Goulven.Furet
 *
 */
public class SearchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);


	static final String ALL_VERTICAL_NAME = "all";

	
	private AggregatedDataRepository aggregatedDataRepository;


	
	@Autowired
	public SearchService(AggregatedDataRepository aggregatedDataRepository) {
		this.aggregatedDataRepository = aggregatedDataRepository;
	}
	
	/**
	 * Operates a search on each vertical, and on datafragments if no results in verticals
	 * TODO(P1,security,0.75) : enable results limitation
	 * @param query
	 * @return
	 */
	public VerticalSearchResponse globalSearch(String initialQuery) {
		
		String query = sanitize(initialQuery).trim();
		
		String frags[] = query.split(" ");
		String q = Stream.of(frags)
				.map(e -> "names.offerNames:"+e)				
				. collect(Collectors.joining(" AND "));
			
		String translatedVerticalQuery = null;
		if (StringUtils.isEmpty(query)) {
			translatedVerticalQuery="_exists_:price.minPrice"; 
			
		} else {			
			translatedVerticalQuery="_exists_:price.minPrice AND ("+q+")"; 
		}
		
		VerticalSearchResponse vsr = new VerticalSearchResponse();			
		// TODO(gof) : Page from conf
		vsr.setData(aggregatedDataRepository.searchValidPrices(translatedVerticalQuery,ALL_VERTICAL_NAME,0,50) .collect(Collectors.toList()));
		vsr.setVerticalName(ALL_VERTICAL_NAME);	
		
		return vsr;
	}

	/**
	 * 
	 * @param categorie
	 * @return all AggregatedData for a categorie
	 */
	public VerticalSearchResponse categorieSearch(String categorie) {
		String translatedVerticalQuery="datasourceCategories:\""+categorie+"\" AND _exists_:price.minPrice"; 
		
		
		
		VerticalSearchResponse vsr = new VerticalSearchResponse();
		
		//TODO : Handle attributes and limits
//		 CategoryStatResults statsResult = aggregatedDataRepository.stats(translatedVerticalQuery);
		
		
		// TODO(gof) : Page from conf
		vsr.setData(aggregatedDataRepository.searchValidPrices(translatedVerticalQuery,ALL_VERTICAL_NAME,0,1000) .collect(Collectors.toList()));
		vsr.setVerticalName(ALL_VERTICAL_NAME);	
		
		return vsr;
	}
	
	public String sanitize(String q) {
		return q.replace("(", " ")
				.replace(")", " ")
				.replace("[", " ")
				.replace("]", " ")
				.replace("+", " ")
				.replace("-", " ")
				.replace(":", " ");
				
				
				
	}

}
