package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductImagesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductOffersDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProductService {


	private ProductRepository repository;

    public ProductService(ProductRepository repository) {
    	this.repository = repository;
    }

    public ProductDto getProduct(long gtin, Locale local, java.util.Set<String> includes)  {
        Product p = null;
        try {
            p = repository.getById(gtin);
        } catch (ResourceNotFoundException e) {
            e.printStackTrace();
        }

        ProductAiReviewDto aiReview = null;
        ProductOffersDto offers = null;
        ProductImagesDto images = null;

        for (String include : includes) {
            ProductDtoComponent component = ProductDtoComponent.valueOf(include);
            switch (component) {
                case aiReview -> aiReview = getAiReview(p, local);
                case offers -> offers = new ProductOffersDto(List.of());
                case images -> images = new ProductImagesDto(List.of());
                default -> throw new IllegalArgumentException("undefined component value : " + include);
            }
        }

        return new ProductDto(gtin, aiReview, offers, images, null);
    }


    /**
     * AI Review component mapping
     * @param p
     * @param local
     * @return
     */
    private ProductAiReviewDto getAiReview(Product p, Locale local) {
        // TODO : Implement real mapping
        return new ProductAiReviewDto(null, Map.of(), false, null, null);
    }











	public Page<ProductReviewDto> getReviews(long gtin, Pageable pageable) throws ResourceNotFoundException {
        List<ProductReviewDto> reviews = new ArrayList<>();
        return new PageImpl<>(reviews, pageable, 0);
    }


    public void createReview(long gtin, String captchaResponse, HttpServletRequest request)
            throws ResourceNotFoundException, SecurityException {
    }
}
