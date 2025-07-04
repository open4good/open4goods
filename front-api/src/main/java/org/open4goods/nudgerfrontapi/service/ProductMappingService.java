package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiReviewDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductAiTextsDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductBaseDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductDto.ProductDtoComponent;
import org.open4goods.nudgerfrontapi.dto.product.ProductNamesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductOffersDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductResourcesDto;
import org.open4goods.nudgerfrontapi.dto.product.ProductReviewDto;
import org.open4goods.nudgerfrontapi.dto.search.AggregationRequestDto;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Maps {@link Product} domain entities to DTOs consumed by the frontend API.
 * Handles localisation and filtering of the returned fields.
 */

@Service
public class ProductMappingService {

    private static final Logger logger = LoggerFactory.getLogger(ProductMappingService.class);

    private final ProductRepository repository;

    public ProductMappingService(ProductRepository repository) {
    	this.repository = repository;
    }

    /**
     *
     * @param gtin
     * @param local
     * @param includes
     * @return
     */
    public ProductDto getProduct(long gtin, Locale local, Set<String> includes)
            throws ResourceNotFoundException {
        Product p = repository.getById(gtin);

        ProductDto pdto = mapProduct(p, local, includes);
        return pdto;
    }

	private ProductDto mapProduct(  Product p, Locale local, Set<String> includes) {
        ProductBaseDto base = null;
        ProductNamesDto names = null;
        ProductResourcesDto resources = null;
        ProductAiTextsDto aiTexts = null;
        ProductAiReviewDto aiReview = null;
        ProductOffersDto offers = null;

        if (null != includes) {
                for (String include : includes) {
                        ProductDtoComponent component = ProductDtoComponent.valueOf(include);

                        switch (component) {
                                case base -> base = mapBase(p);
                                case names -> names = mapNames(p, local);
                                case resources -> resources = mapResources(p);
                                case aiReview -> aiReview = mapAiReview(p, local);
                                case offers -> offers = mapOffers(p, local);
                                default -> throw new IllegalArgumentException("Missing component mapper for: " + include);
                        }
                }
        }

        return new ProductDto(
                p.getId(),
                base,
                names,
                resources,
                aiTexts,
                aiReview,
                offers);
        }


    private ProductOffersDto mapOffers(Product p, Locale local) {
                return null;
        }

        private ProductBaseDto mapBase(Product p) {
                return new ProductBaseDto(
                                p.getId(),
                                p.getCreationDate(),
                                p.getLastChange(),
                                p.getVertical(),
                                p.getExternalIds(),
                                p.getGoogleTaxonomyId());
        }

        private ProductNamesDto mapNames(Product p, Locale local) {
                if (p.getNames() == null) {
                        return null;
                }
                return new ProductNamesDto(
                                p.getNames().getH1Title().i18n(local.getLanguage()),
                                p.getNames().getMetaDescription().i18n(local.getLanguage()),
                                p.getNames().getProductMetaOpenGraphTitle().i18n(local.getLanguage()),
                                p.getNames().getProductMetaOpenGraphDescription().i18n(local.getLanguage()),
                                p.getNames().getProductMetaTwitterTitle().i18n(local.getLanguage()),
                                p.getNames().getProductMetaTwitterDescription().i18n(local.getLanguage()));
        }

        private ProductResourcesDto mapResources(Product p) {
                return new ProductResourcesDto(
                                p.unprocessedImagesUrl(),
                                p.videos().stream().map(r -> r.getUrl()).toList(),
                                p.pdfs().stream().map(r -> r.getUrl()).toList(),
                                p.externalCover());
        }


	/**
     * AI Review component mapping
     * @param p
     * @param local
     * @return
     */
    private ProductAiReviewDto mapAiReview(Product p, Locale local) {
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


    public Page<ProductDto> getProducts(Pageable pageable, Locale locale, Set<String> includes,
                                        AggregationRequestDto aggregation) {

                // TODO implement aggregation handling once search service is ready
                SearchHits<Product> response = repository.get(pageable);
                List<ProductDto> items = response
                                .map(e -> e.getContent())
                                .map(e -> mapProduct(e, locale, includes))
                                .toList();

		return  new PageImpl<ProductDto>(items, pageable, response.getTotalHits());
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
