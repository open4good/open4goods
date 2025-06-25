package org.open4goods.nudgerfrontapi.dto;

import java.util.List;

import org.open4goods.model.product.Product;

public record SearchResponse(long total,
                             int page,
                             int size,
                             List<Product> items) {
}
