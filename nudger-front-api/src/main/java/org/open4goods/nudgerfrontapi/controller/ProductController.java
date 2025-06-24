package org.open4goods.nudgerfrontapi.controller;

import java.time.Duration;
import java.util.List;

import org.open4goods.nudgerfrontapi.dto.ImpactScoreDto;
import org.open4goods.nudgerfrontapi.dto.OfferDto;
import org.open4goods.nudgerfrontapi.dto.ProductViewResponse;
import org.open4goods.nudgerfrontapi.dto.ReviewDto;
import org.open4goods.nudgerfrontapi.service.ProductService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/product/{gtin}")
    @Operation(summary = "Get product view")
    public ResponseEntity<ProductViewResponse> product(@PathVariable long gtin) throws Exception {
        ProductViewResponse body = service.getProduct(gtin);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(body);
    }

    @GetMapping("/product/{gtin}/reviews")
    @Operation(summary = "Get product reviews")
    public ResponseEntity<List<ReviewDto>> reviews(@PathVariable long gtin) throws Exception {
        List<ReviewDto> body = service.getReviews(gtin);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(body);
    }

    @PostMapping("/product/{gtin}/reviews")
    @Operation(summary = "Generate AI review")
    public ResponseEntity<Void> generateReview(@PathVariable long gtin,
                                               @RequestParam("hcaptchaResponse") String captcha,
                                               HttpServletRequest request) throws Exception {
        service.createReview(gtin, captcha, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/product/{gtin}/offers")
    @Operation(summary = "Get product offers")
    public ResponseEntity<List<OfferDto>> offers(@PathVariable long gtin) throws Exception {
        List<OfferDto> body = service.getOffers(gtin);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(body);
    }

    @GetMapping("/product/{gtin}/impact")
    @Operation(summary = "Get product impact score")
    public ResponseEntity<ImpactScoreDto> impact(@PathVariable long gtin) throws Exception {
        ImpactScoreDto body = service.getImpactScore(gtin);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                .body(body);
    }
}
