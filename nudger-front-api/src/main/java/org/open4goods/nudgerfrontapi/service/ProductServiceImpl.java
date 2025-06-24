package org.open4goods.nudgerfrontapi.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.commons.helper.IpHelper;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.Score;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.nudgerfrontapi.dto.ImpactScoreDto;
import org.open4goods.nudgerfrontapi.dto.OfferDto;
import org.open4goods.nudgerfrontapi.dto.ProductViewRequest;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.dto.ReviewDto;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final ReviewGenerationService reviewGenerationService;
    private final VerticalsConfigService verticalsConfigService;
    private final HcaptchaService captchaService;

    public ProductServiceImpl(ProductRepository repository,
                              ReviewGenerationService reviewGenerationService,
                              VerticalsConfigService verticalsConfigService,
                              HcaptchaService captchaService) {
        this.repository = repository;
        this.reviewGenerationService = reviewGenerationService;
        this.verticalsConfigService = verticalsConfigService;
        this.captchaService = captchaService;
    }

    @Override
    public ProductViewResponse getProduct(long gtin) throws ResourceNotFoundException {
        Product p = repository.getById(gtin);
        return new ProductViewResponse(new ProductViewRequest(gtin), null, p.gtin());
    }

    @Override
    public List<ReviewDto> getReviews(long gtin) throws ResourceNotFoundException {
        Product p = repository.getById(gtin);
        return p.getReviews().entrySet().stream()
                .map(e -> new ReviewDto(e.getKey(), e.getValue().getReview(), e.getValue().getCreatedMs()))
                .toList();
    }

    @Override
    public List<OfferDto> getOffers(long gtin) throws ResourceNotFoundException {
        Product p = repository.getById(gtin);
        AggregatedPrices prices = p.getPrice();
        return prices.sortedOffers(ProductCondition.NEW).stream()
                .map(this::toOfferDto)
                .collect(Collectors.toList());
    }

    private OfferDto toOfferDto(AggregatedPrice price) {
        return new OfferDto(price.getDatasourceName(), price.getOfferName(), price.getPrice(),
                price.getCurrency().getCurrencyCode(), price.getUrl());
    }

    @Override
    public ImpactScoreDto getImpactScore(long gtin) throws ResourceNotFoundException {
        Product p = repository.getById(gtin);
        Map<String, Double> scores = p.getScores().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    Score s = e.getValue();
                    return s.getValue() == null ? 0d : s.getValue();
                }));
        double avg = scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new ImpactScoreDto(scores, avg);
    }

    @Override
    public void createReview(long gtin, String captchaResponse, HttpServletRequest request)
            throws ResourceNotFoundException, SecurityException {
        captchaService.verifyRecaptcha(IpHelper.getIp(request), captchaResponse);
        Product product = repository.getById(gtin);
        reviewGenerationService.generateReviewAsync(product,
                verticalsConfigService.getConfigById(product.getVertical()), null);
    }
}
