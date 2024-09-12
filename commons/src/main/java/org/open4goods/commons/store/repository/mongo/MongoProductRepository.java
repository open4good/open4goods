package org.open4goods.commons.store.repository.mongo;

import org.open4goods.commons.model.product.MongoProduct;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 
 * @author goulven
 *
 */

public interface  MongoProductRepository extends MongoRepository<MongoProduct, Long> {



}
