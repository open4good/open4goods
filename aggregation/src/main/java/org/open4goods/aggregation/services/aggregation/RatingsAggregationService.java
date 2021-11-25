//package org.open4goods.aggregation.services.aggregation;
//
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.open4goods.aggregation.AbstractBatchedAggregationService;
//import org.open4goods.aggregation.tank.AggregatorTank;
//import org.open4goods.config.yml.ui.RatingsConfig;
//import org.open4goods.model.attribute.Cardinality;
//import org.open4goods.model.data.DataFragment;
//import org.open4goods.model.data.Rating;
//import org.open4goods.model.product.AggregatedData;
//import org.open4goods.model.product.SourcedRating;
//import org.open4goods.services.StandardiserService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * This service is in charge of Ratings aggregation. Missions are :
// * <ul>
// * <li>Relativisation of ratings (rescaling through computing min max) </li>
// * <li>Aggregation of comments score</li>
// * </ul>
// * 
// * 
// * @author Goulven.Furet
// *
// */
//public class RatingsAggregationService extends AbstractBatchedAggregationService {
//
//	private static final Logger LOGGER = LoggerFactory.getLogger(RatingsAggregationService.class);
//	
//		
//	public static final String COMMENTS_RATING_TAG = "COMMENTS";
//
//	private static final String COMMENT_KEY = "COMMENT_KEY";
//	
//	private final RatingsConfig ratingsConfig;
//
//	public RatingsAggregationService(final RatingsConfig ratingsConfig,  final String logsFolder) {
//		super(logsFolder);
//		this.ratingsConfig = ratingsConfig;
//	}
//
//	/**
//	 * 
//	 * On the fly, increments cardinality through trasversal Map
//	 * @param d
//	 * @param p
//	 * @param match2
//	 */
//	@Override
//	public void onDataFragment(final DataFragment d, final AggregatedData output) {
//				
//		// Converting to SourcedRatings
//		Set<SourcedRating> ratings = d.getRatings().stream().map(e -> new SourcedRating(e,d)).collect(Collectors.toSet()) ;
//
//		for (Rating r : ratings) {
//			System.out.println(r.getTags());
//			if (r.getTags().contains("TECHNICAL") ) {
//				System.err.println("TECHNICAL");
//			}
//		}
//		
//		// Adding all ratings in output
//		output.getRatings().addAll(ratings);
//
//	}
//
//
//	
//
//	@Override
//	public void onDataFragments(final Set<DataFragment> input, final AggregatedData output) {
//		
//		////////////////////////////////////////////////////////////////
//		// Creating the comments global rating
//		////////////////////////////////////////////////////////////////
//		
//		// Creating a cardinality
//		Cardinality c = new Cardinality();
//		// Increment cardinality for all comments, to make an aggregated comments score
//		input.stream()
//			.map(e->e.getComments())
//			.flatMap(e -> e.stream())
//			.map(e -> e.getRating())
//			.filter(e -> e != null).forEach(r -> {
//
//				// Incrementing
//				c.increment(r);						
//		});
//		
//		
//		if (c.getCount() > 0) {
//			SourcedRating r = new SourcedRating();
//			
//			r.setDate(System.currentTimeMillis());
//			r.setDatasourceName(getClass().getSimpleName()+":"+COMMENTS_RATING_TAG);
//			r.setMax(StandardiserService.DEFAULT_MAX_RATING);
//			r.addTag(COMMENTS_RATING_TAG);
//			r.setValue(c.getAvg());
//			r.setNumberOfVoters(Long.valueOf(c.getCount()));
//			r.setMin(0);
//					
//			output.getRatings().add(r);
//		
//			// For the newly computed comment rating
//			Cardinality gc = (Cardinality) batchDatas.get(COMMENT_KEY);			
//			if (null == gc) {
//				gc = new Cardinality();
//			}		
//			// Incrementing
//			gc.increment(r);
//			
//			batchDatas.put(COMMENT_KEY,gc);	
//			
//		}
//		
//		
//		////////////////////////////////////////////////////////////////		
//		// Incrementing classical cardinalities
//		////////////////////////////////////////////////////////////////
//
//		// For each ratings of datafragments. 	
//		processCardinality(output.getRatings(), batchDatas);
//		
//		
//		
//	
//	}
//
//
//	/**
//	 * Associates cardinality to ratings and operates relativisation
//	 */
//	@Override
//	public void onAggregatedData(AggregatedData data, AggregatorTank tank, Map<String, Object> batchDatas) {
//		data.getRatings().forEach(r -> {						
//			
//				// Associating cardinality
//				r.setCardinality((Cardinality) batchDatas.get(getCardId(r)));
//				// Computing relatives values
//				relativize(r);
//				
//		});
//		
//		// The global comment		
//		SourcedRating sr = data.ratingByTag(COMMENTS_RATING_TAG);
//		if (null != sr) {
//			Cardinality cc = (Cardinality) batchDatas.get(COMMENT_KEY);
//			sr.setCardinality(cc);
//			relativize(sr);
//		}
//	}
//
//	/**
//	 * Computes relativ values
//	 * @param rating
//	 */
//	private void relativize(SourcedRating rating) {
//		
//		// Substracting unused min
//		
//		if (null == rating.getValue()) {
//			LOGGER.warn("Empty value for rating {} ! Consider normalizing in a futur export/import phase",rating);
//			return;
//		}
//		
//		try {
//			// Removing the min range
//			Double minBorn = rating.getCardinality().getMin() - rating.getMin();
//			
//			// Standardizing rating based on real max		
//			final Double max = rating.getCardinality().getMax();
//	
//			final Double value = rating.getValue();
//			rating.setRelValue((value -minBorn) * StandardiserService.DEFAULT_MAX_RATING / (max -minBorn));
//
//		} catch (Exception e) {
//			LOGGER.warn("Relativisation failed",e);
//		}
//
//	}
//
//
//	/**
//	 * Computes and maintains cardinality
//	 * @param ratings
//	 * @param batchDatas
//	 */
//	private void processCardinality(Set<SourcedRating> ratings, Map<String, Object> batchDatas) {
//		ratings.forEach(r -> {
//			
//			if (null == r.getValue()) {
//				LOGGER.warn("Empty value for rating {} ! Consider normalizing in a futur export/import phase",r);
//				return;
//			}
//			
//			// Retrieving cardinality
//			Cardinality c = (Cardinality) batchDatas.get(getCardId(r));			
//			if (null == c) {
//				c = new Cardinality();
//			}
//			
//			// Incrementing
//			c.increment(r);
//					
//			batchDatas.put(getCardId(r),c);
//			
//		});
//	}
//
//	private String getCardId(SourcedRating r) {
//		return "card-"+r.getDatasourceName();
//	}
//
//	@Override
//	public void beforeSecondPass(final AggregatorTank tank) {
//		
//	}
//
//	
//	
//	@Override
//	public void before(final AggregatorTank tank) {
//
//	}
//
//}
