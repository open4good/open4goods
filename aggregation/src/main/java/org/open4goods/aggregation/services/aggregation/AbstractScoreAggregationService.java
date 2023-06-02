package org.open4goods.aggregation.services.aggregation;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.aggregation.AbstractAggregationService;
import org.open4goods.model.attribute.Cardinality;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Score;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.SourcedScore;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractScoreAggregationService extends AbstractAggregationService{

	private static final Logger LOGGER = LoggerFactory.getLogger(ScoresAggregationService.class);

	
	public AbstractScoreAggregationService(String logsFolder) {
		super(logsFolder);
	}




	@Override
	public void onDataFragment(final Set<DataFragment> input, final Product output) {

		////////////////////////////////////////////////////////////////
		// Creating the comments global Score
		////////////////////////////////////////////////////////////////

		// Creating a cardinality
		Cardinality c = new Cardinality();
		// Increment cardinality for all comments, to make an aggregated comments score
		input.stream()
			.map(e->e.getComments())
			.flatMap(e -> e.stream())
			.map(e -> e.getScore())
			.filter(e -> e != null).forEach(r -> {

				// Incrementing
				c.increment(r);
		});


		if (c.getCount() > 0) {
			SourcedScore r = new SourcedScore();

			r.setDate(System.currentTimeMillis());
			r.setDatasourceName(getClass().getSimpleName()+":"+COMMENTS_Score_TAG);
			r.setMax(StandardiserService.DEFAULT_MAX_Score);
			r.addTag(COMMENTS_Score_TAG);
			r.setValue(c.getAvg());
			r.setNumberOfVoters(Long.valueOf(c.getCount()));
			r.setMin(0);

			output.getScores().add(r);

			// For the newly computed comment Score
			Cardinality gc = (Cardinality) batchDatas.get(COMMENT_KEY);
			if (null == gc) {
				gc = new Cardinality();
			}
			// Incrementing
			gc.increment(r);

			batchDatas.put(COMMENT_KEY,gc);

		}


		////////////////////////////////////////////////////////////////
		// Incrementing classical cardinalities
		////////////////////////////////////////////////////////////////

		// For each Scores of datafragments.
		processCardinality(output.getScores(), batchDatas);




	}


	/**
	 * Associates cardinality to Scores and operates relativisation
	 */
	@Override
	public void onAggregatedData(Product data, AggregatorTank tank, Map<String, Object> batchDatas) {
		data.getScores().forEach(r -> {

				// Associating cardinality
				r.setCardinality((Cardinality) batchDatas.get(getCardId(r)));
				// Computing relatives values
				relativize(r);

		});

		// The global comment
		SourcedScore sr = data.ScoreByTag(COMMENTS_Score_TAG);
		if (null != sr) {
			Cardinality cc = (Cardinality) batchDatas.get(COMMENT_KEY);
			sr.setCardinality(cc);
			relativize(sr);
		}
	}

	/**
	 * Computes relativ values
	 * @param Score
	 */
	private void relativize(SourcedScore Score) {

		// Substracting unused min

		if (null == Score.getValue()) {
			LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",Score);
			return;
		}

		try {
			// Removing the min range
			Double minBorn = Score.getCardinality().getMin() - Score.getMin();

			// Standardizing Score based on real max
			final Double max = Score.getCardinality().getMax();

			final Double value = Score.getValue();
			Score.setRelValue((value -minBorn) * StandardiserService.DEFAULT_MAX_Score / (max -minBorn));

		} catch (Exception e) {
			LOGGER.warn("Relativisation failed",e);
		}

	}


	/**
	 * Computes and maintains cardinality
	 * @param Scores
	 * @param batchDatas
	 */
	private void processCardinality(Set<Score> Scores, ) {
		Scores.forEach(r -> {

			if (null == r.getValue()) {
				LOGGER.warn("Empty value for Score {} ! Consider normalizing in a futur export/import phase",r);
				return;
			}

			// Retrieving cardinality
			Cardinality c = (Cardinality) batchDatas.get(r.getName());
			if (null == c) {
				c = new Cardinality();
			}

			// Incrementing
			c.increment(r);

			batchDatas.put(getCardId(r),c);

		});
	}

}
