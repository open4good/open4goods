package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.Localisable;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
    	this.repository = repository;
    }

    public ProductDto getProduct(long gtin, Locale local, Set<String> includes) throws ResourceNotFoundException {
        Product p = repository.getById(gtin);

    	ProductDto pdto = new ProductDto();

    	/////////////////////////////////////////////
    	// Handling global / high level attributes
    	/////////////////////////////////////////////
    	pdto.setGtin(gtin );

    	/////////////////////////////////////////////
    	// Handling requested components
    	/////////////////////////////////////////////
    	for (String include : includes) {
    		ProductDtoComponent component = ProductDtoComponent.valueOf(include);

    		switch (component) {
			case aiReview : {
				// AI Review component
				pdto.setAiReview(getAiReview(p,local));
				break;
			}
			default:
				throw new IllegalArgumentException("undefined component value : " + include);
			}
    	}
    	return pdto;
    }


    /**
     * AI Review component mapping
     * @param p
     * @param local
     * @return
     */
    private ProductAiReviewDto getAiReview(Product p, Locale local) {
        if (p == null || p.getReviews() == null) {
            return null;
        }

        AiReviewHolder holder = p.getReviews().i18n(local.getLanguage());
        if (holder == null) {
            return null;
        }

        return new ProductAiReviewDto(
                holder.getReview(),
                holder.getSources(),
                holder.isEnoughData(),
                holder.getTotalTokens(),
                holder.getCreatedMs());
    }











	public Page<ProductReviewDto> getReviews(long gtin, Pageable pageable) throws ResourceNotFoundException {
        List<ProductReviewDto> reviews = new ArrayList<>();
        return new PageImpl<>(reviews, pageable, 0);
    }


    public void createReview(long gtin, String captchaResponse, HttpServletRequest request)
            throws ResourceNotFoundException, SecurityException {
        // Ensure the product exists
        repository.getById(gtin);
        logger.info("AI review generation requested for product {}", gtin);
    }
}
