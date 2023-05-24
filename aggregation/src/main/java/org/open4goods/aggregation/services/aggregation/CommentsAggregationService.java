//
//package org.open4goods.aggregation.services.aggregation;
//
//import org.open4goods.aggregation.AbstractAggregationService;
//import org.open4goods.config.yml.CommentsAggregationConfig;
//import org.open4goods.model.data.Comment;
//import org.open4goods.model.data.DataFragment;
//import org.open4goods.model.data.aggregated.AggregatedComment;
//import org.open4goods.model.product.AggregatedComments;
//import org.open4goods.model.product.AggregatedData;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// *
// * @author goulven
// *
// */
//public class CommentsAggregationService extends AbstractAggregationService {
//
//	private CommentsAggregationConfig commentsAggregationConfig;
//
//	
//	public CommentsAggregationService( final String logsFolder, CommentsAggregationConfig commentsAggregationConfig) {
//
//		super(logsFolder);
//		this.commentsAggregationConfig = commentsAggregationConfig;
//	}
//
//	private static final Logger logger = LoggerFactory.getLogger(CommentsAggregationService.class);
//
//	@Override
//	public void onDataFragment(DataFragment input, AggregatedData output) {
//			
//			
//			// Before, instanciate the holder
//			AggregatedComments comments = new AggregatedComments();
//			
//				
//				// Classical comments
//				for (Comment c : input.getComments()) {					
//					AggregatedComment ac = getAggregatedComment(input, c);					
//					// Adding
//					comments.getComments().add(ac);
//				}		
//						
//			
//			// Setting in AggregatedData
//			output.setComments(comments);
//			
//			
//			
//			
//			
//	}
//
//	private AggregatedComment getAggregatedComment(DataFragment df, Comment c) {
//		// Instanciating the aggregated comment
//		AggregatedComment ac = new AggregatedComment(c, df);
//		ac.setLanguage(c.getDescription().getLanguage());
//		
//		// Description textualisation
//		ac.setDescription(c.getDescription().getText());
//		
//		// Title textualisation
//		ac.setTitle(c.getTitle().getText());
//		return ac;
//	}
//
//}
