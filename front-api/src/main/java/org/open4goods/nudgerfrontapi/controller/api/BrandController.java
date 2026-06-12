package org.open4goods.nudgerfrontapi.controller.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.open4goods.brand.model.ManufacturingSite;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.nudgerfrontapi.controller.CacheControlConstants;
import org.open4goods.nudgerfrontapi.dto.brand.BrandDto;
import org.open4goods.nudgerfrontapi.dto.brand.ManufacturingDistanceDto;
import org.open4goods.nudgerfrontapi.dto.brand.ManufacturingSiteDistanceDto;
import org.open4goods.nudgerfrontapi.dto.user.UserGeolocDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.BrandMappingService;
import org.open4goods.nudgerfrontapi.service.UserGeolocationService;
import org.open4goods.nudgerfrontapi.utils.HaversineUtil;
import org.open4goods.nudgerfrontapi.utils.IpUtils;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Exposes enriched brand / company intelligence:
 * <ul>
 *   <li>{@code GET /brands/{brandName}} — the brand, its company, manufacturing
 *       chain (filtered by category), scores and x-metas. Static per brand and
 *       cacheable; this is the SSR-friendly, indexable payload.</li>
 *   <li>{@code GET /brands/distance/{gtin}} — the distance from the requesting
 *       user to each manufacturing site of the product. Per-user and NOT
 *       cacheable; intended for client-side hydration after SSR.</li>
 * </ul>
 */
@RestController
@RequestMapping("/brands")
@PreAuthorize("hasAnyAuthority('" + RolesConstants.ROLE_FRONTEND + "', '" + RolesConstants.ROLE_EDITOR + "')")
@Tag(name = "Brand", description = "Enriched brand/company intelligence: manufacturing places, scores, facts, user distance.")
public class BrandController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrandController.class);

    private final BrandMappingService brandMappingService;
    private final ProductRepository productRepository;
    private final UserGeolocationService userGeolocationService;

    public BrandController(BrandMappingService brandMappingService,
            ProductRepository productRepository,
            UserGeolocationService userGeolocationService) {
        this.brandMappingService = brandMappingService;
        this.productRepository = productRepository;
        this.userGeolocationService = userGeolocationService;
    }

    /**
     * Resolve a brand and return its enriched company details, with manufacturing
     * sites optionally scoped to a product category.
     *
     * @param brandName raw brand name (will be resolved to canonical)
     * @param domainLanguage localisation hint
     * @param verticalId optional vertical id to scope manufacturing sites
     * @return the brand DTO, or 404 when the brand is unknown
     */
    @GetMapping("/{brandName}")
    @Operation(summary = "Get enriched brand details",
            description = "Resolve a brand to its company and return manufacturing places, scores and facts.")
    public ResponseEntity<BrandDto> brand(
            @PathVariable("brandName") String brandName,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            @RequestParam(name = "verticalId", required = false) String verticalId) {
        LOGGER.info("Entering brand(brandName={}, verticalId={})", brandName, verticalId);
        BrandDto dto = brandMappingService.mapBrand(brandName, verticalId, toLocale(domainLanguage));
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
                .body(dto);
    }

    /**
     * Compute the distance from the requesting user to each manufacturing site of
     * the given product. Per-user; never cached.
     *
     * @param gtin product gtin
     * @param domainLanguage localisation hint
     * @param ip optional IP override
     * @param request HTTP request used to resolve the client IP
     * @return distances, or 204 when the user IP cannot be resolved
     */
    @GetMapping("/distance/{gtin}")
    @Operation(summary = "Get user-to-manufacturing distance",
            description = "Distance from the requesting user to each manufacturing site of the product.")
    public ResponseEntity<ManufacturingDistanceDto> manufacturingDistance(
            @PathVariable("gtin") long gtin,
            @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
            @RequestParam(name = "ip", required = false) String ip,
            HttpServletRequest request) {
        LOGGER.info("Entering manufacturingDistance(gtin={})", gtin);

        Product product;
        try {
            product = productRepository.getByIdWithoutEmbedding(gtin);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        List<ManufacturingSite> sites = brandMappingService.manufacturingSites(product.brand(), product.getVertical());
        if (sites.isEmpty()) {
            return ResponseEntity.noContent().cacheControl(CacheControlConstants.PRIVATE_NO_STORE_CACHE).build();
        }

        String resolvedIp = StringUtils.hasText(ip) ? ip : IpUtils.getIp(request);
        UserGeolocDto geo = StringUtils.hasText(resolvedIp) ? userGeolocationService.resolve(resolvedIp) : null;
        if (geo == null || geo.latitude() == null || geo.longitude() == null) {
            return ResponseEntity.noContent().cacheControl(CacheControlConstants.PRIVATE_NO_STORE_CACHE).build();
        }

        Locale locale = toLocale(domainLanguage);
        List<ManufacturingSiteDistanceDto> distances = new ArrayList<>();
        for (ManufacturingSite site : sites) {
            Double distanceKm = (site.getLat() == null || site.getLon() == null) ? null
                    : HaversineUtil.distanceKm(geo.latitude(), geo.longitude(), site.getLat(), site.getLon());
            distances.add(new ManufacturingSiteDistanceDto(
                    site.getCountry(),
                    brandMappingService.countryName(site.getCountry(), locale),
                    site.getCity(),
                    site.getType() == null ? null : site.getType().jsonValue(),
                    site.getOperator(),
                    site.getLat(),
                    site.getLon(),
                    distanceKm));
        }
        distances.sort(Comparator.comparing(d -> d.distanceKm() == null ? Double.MAX_VALUE : d.distanceKm()));

        ManufacturingDistanceDto body = new ManufacturingDistanceDto(
                geo.countryIsoCode(), geo.cityName(), geo.latitude(), geo.longitude(), distances);
        return ResponseEntity.ok()
                .cacheControl(CacheControlConstants.PRIVATE_NO_STORE_CACHE)
                .body(body);
    }

    private Locale toLocale(DomainLanguage domainLanguage) {
        if (domainLanguage == null || !StringUtils.hasText(domainLanguage.languageTag())) {
            return Locale.ENGLISH;
        }
        return Locale.forLanguageTag(domainLanguage.languageTag());
    }
}
