package org.open4goods.nudgerfrontapi.service;

import java.util.List;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.nudgerfrontapi.dto.ImpactScoreDto;
import org.open4goods.nudgerfrontapi.dto.OfferDto;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.dto.ReviewDto;

import jakarta.servlet.http.HttpServletRequest;

public interface ProductService {
    ProductViewResponse getProduct(long gtin) throws ResourceNotFoundException;
    List<ReviewDto> getReviews(long gtin) throws ResourceNotFoundException;
    List<OfferDto> getOffers(long gtin) throws ResourceNotFoundException;
    ImpactScoreDto getImpactScore(long gtin) throws ResourceNotFoundException;
    void createReview(long gtin, String captchaResponse, HttpServletRequest request) throws ResourceNotFoundException, SecurityException;
}
