package org.open4goods.store.repository.redis;

import org.open4goods.model.product.Product;
import org.springframework.data.repository.CrudRepository;

/**
 * This "blank" repository is useless, but it is the model trhat allows allows "easy" schema recreation for verticals (see VerticalGeneration.createIndex)
 * @author goulven
 *
 */

public interface  RedisProductRepository extends CrudRepository<Product, String> {



}
