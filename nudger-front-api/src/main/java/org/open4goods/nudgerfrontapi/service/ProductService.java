package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
// Placeholder until we integrate with the real external product repository.
public class ProductService {


    public ProductService() {

    }

    public ProductDto getProduct(long gtin) throws ResourceNotFoundException {
        return new ProductDto(null, gtin);
    }

    public List<ProductReviewDto> getReviews(long gtin) throws ResourceNotFoundException {
        return new ArrayList<>();
    }


    public void createReview(long gtin, String captchaResponse, HttpServletRequest request)
            throws ResourceNotFoundException, SecurityException {
    }
}
