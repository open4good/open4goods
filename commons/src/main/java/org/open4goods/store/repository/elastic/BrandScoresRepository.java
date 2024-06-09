package org.open4goods.store.repository.elastic;

import java.util.List;

import org.open4goods.model.data.BrandScore;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface  BrandScoresRepository extends ElasticsearchRepository<BrandScore, String> {
	
	List<BrandScore> findByDatasourceNameAndBrandName(String datasourceName, String brandName);

	List<BrandScore> findByDatasourceNameAndBrandNameIsStartingWith(String datasourceName, String brandName);

	List<BrandScore> findByDatasourceNameAndBrandNameIsContaining(String datasourceName, String brandName);

	List<BrandScore> findByDatasourceNameAndBrandNameIsLike(String datasourceName, String brandName);

}
