package org.open4goods.commons.store.repository.elastic;

import java.util.List;

import org.open4goods.commons.model.data.BrandScore;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface  BrandScoresRepository extends ElasticsearchRepository<BrandScore, String> {
	
	
    @Query("""
 		   {
 		      "bool":{
 		         "must":[
 		            {
 		               "term":{
 		                  "datasourceName": "#{#datasourceName}"
 		               }
 		            },
 		            {
 		               "query_string":{
 		                  "query":"*#{#brandName}",
 		                  "fields":["brandName"]
 		               }
 		            }
 		         ]
 		      }
 		   }
     		""")    
	List<BrandScore> findByDatasourceNameAndBrandNameStartingWith(String datasourceName, String brandName);

    @Query("""
		   {
		      "bool":{
		         "must":[
		            {
		               "term":{
		                  "datasourceName": "#{#datasourceName}"
		               }
		            },
		            {
		               "query_string":{
		                  "query":"*#{#brandName}*",
		                  "fields":["brandName"]
		               }
		            }
		         ]
		      }
		   }
    		""")
	List<BrandScore> findByDatasourceNameAndBrandNameLike(String datasourceName, String brandName);

}
