package org.open4goods.ui.repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.model.data.ContributionVote;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Spring repository for contribution vote
 */
public interface  ContributionVoteRepository extends ElasticsearchRepository<ContributionVote, String> {

	
	
	  /**
     * Counts all occurrences of ContributionVote from a given date based on the "ts" field.
     * 
     * @param ts the timestamp from which to start counting
     * @return the number of occurrences
     */
    long countByTsGreaterThanEqual(long ts);
    
    
    /**
     * Counts and aggregates occurrences of ContributionVote by "vote" from a given "ts".
     * 
     * @param ts the timestamp from which to start counting and aggregating
     * @return a map where the key is the vote value and the value is the count of occurrences
     */
    @Query("{\n" +
            "  \"size\": 0,\n" +
            "  \"query\": {\n" +
            "    \"range\": {\n" +
            "      \"ts\": {\n" +
            "        \"gte\": ?0\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"aggs\": {\n" +
            "    \"vote_aggregation\": {\n" +
            "      \"terms\": {\n" +
            "        \"field\": \"vote.keyword\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}")
    Map<String, Long> countAndAggregateByvoteFromTs(long ts);

    default Map<String, Long> processAggregationResults(Map<String, Object> rawResult) {
        // Extract the aggregation results
        Map<String, Object> aggregations = (Map<String, Object>) rawResult.get("aggregations");
        Map<String, Object> voteAgg = (Map<String, Object>) aggregations.get("vote_aggregation");
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) voteAgg.get("buckets");

        // Convert the list of buckets to a Map<String, Long>
        return buckets.stream()
                .collect(Collectors.toMap(
                        bucket -> (String) bucket.get("key"),
                        bucket -> ((Number) bucket.get("doc_count")).longValue()
                ));
    }
}
