package org.open4goods.ui.repository;

import java.util.List;

import org.open4goods.ui.model.CheckedUrl;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for CheckedUrl documents.
 */
@Repository
public interface CheckedUrlRepository extends ElasticsearchRepository<CheckedUrl, String> {

    /**
     * Retrieves all URLs that have a specific lastStatus code.
     * Example usage: repository.getByLastStatus(500)
     *
     * @param lastStatus The HTTP status code
     * @return A list of CheckedUrl documents matching the given status
     */
    List<CheckedUrl> getByLastStatus(int lastStatus);

    
    
    
}
