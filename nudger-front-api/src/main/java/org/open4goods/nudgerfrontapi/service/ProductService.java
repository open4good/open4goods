package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductView;
import org.open4goods.nudgerfrontapi.dto.ReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ImpactScoreDto;
import org.open4goods.nudgerfrontapi.dto.product.OfferDto;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProductService {


    public ProductService() {

    }

    public ProductView getProduct(long gtin) throws ResourceNotFoundException {
        return new ProductView(new ProductViewRequest(gtin));
    }

    public List<ReviewDto> getReviews(long gtin) throws ResourceNotFoundException {
        return new ArrayList<>();
    }

    public List<OfferDto> getOffers(long gtin) throws ResourceNotFoundException {
        return new ArrayList<>();
    }

    private OfferDto toOfferDto(AggregatedPrice price) {

    	// TODO : handle the numll parameter (matching currency code)
        return new OfferDto(price.getDatasourceName(), price.getOfferName(), price.getPrice(),
               null, price.getUrl());
    }

    public ImpactScoreDto getImpactScore(long gtin) throws ResourceNotFoundException {

        Map<String, Double> scores = null;
		double avg = 0;
		return new ImpactScoreDto(scores, avg);
    }

    public void createReview(long gtin, String captchaResponse, HttpServletRequest request)
            throws ResourceNotFoundException, SecurityException {
    }
}
