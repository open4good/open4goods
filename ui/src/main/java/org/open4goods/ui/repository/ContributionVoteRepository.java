package org.open4goods.ui.repository;

import org.open4goods.commons.model.data.ContributionVote;
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
    
    
}
