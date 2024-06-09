package org.open4goods.store.repository.elastic;

import java.awt.print.Book;
import java.awt.print.Pageable;
import java.util.Collection;
import java.util.List;

import org.open4goods.model.data.BrandScore;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import co.elastic.clients.elasticsearch.ml.Page;

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
