package org.open4goods.nudgerfrontapi.service;

import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;

public interface ProductViewUseCase {
    ProductViewResponse render(ProductViewRequest request);
}
