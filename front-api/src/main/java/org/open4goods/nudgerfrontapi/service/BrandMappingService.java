package org.open4goods.nudgerfrontapi.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.Company;
import org.open4goods.brand.model.CompanyLocation;
import org.open4goods.brand.model.CompanyScore;
import org.open4goods.brand.model.ManufacturingSite;
import org.open4goods.brand.model.SourcedReference;
import org.open4goods.brand.model.XMeta;
import org.open4goods.brand.service.BrandService;
import org.open4goods.nudgerfrontapi.dto.brand.BrandDto;
import org.open4goods.nudgerfrontapi.dto.brand.CompanyDto;
import org.open4goods.nudgerfrontapi.dto.brand.CompanyLocationDto;
import org.open4goods.nudgerfrontapi.dto.brand.CompanyScoreDto;
import org.open4goods.nudgerfrontapi.dto.brand.ManufacturingSiteDto;
import org.open4goods.nudgerfrontapi.dto.brand.SourcedReferenceDto;
import org.open4goods.nudgerfrontapi.dto.brand.XMetaDto;
import org.springframework.stereotype.Service;

/**
 * Maps {@link Brand} / {@link Company} domain objects to their front-end DTOs.
 * Manufacturing sites are filtered by the requested product category (vertical).
 */
@Service
public class BrandMappingService {

    private final BrandService brandService;

    public BrandMappingService(BrandService brandService) {
        this.brandService = brandService;
    }

    /**
     * Resolve a brand and project it to a DTO, filtering manufacturing sites for
     * the given vertical.
     *
     * @param brandName raw brand name
     * @param verticalId vertical id to scope manufacturing sites (nullable = all)
     * @param locale locale used to localize country names
     * @return the brand DTO, or {@code null} when the brand resolves to nothing useful
     */
    public BrandDto mapBrand(String brandName, String verticalId, Locale locale) {
        if (StringUtils.isBlank(brandName)) {
            return null;
        }
        Brand brand = brandService.resolve(brandName);
        if (brand == null || StringUtils.isBlank(brand.getBrandName())) {
            return null;
        }
        return new BrandDto(
                brand.getBrandName(),
                brand.getCompanyName(),
                brand.getOfficialDomains() == null ? List.of() : brand.getOfficialDomains(),
                mapCompany(brand.getCompany(), verticalId, locale));
    }

    private CompanyDto mapCompany(Company company, String verticalId, Locale locale) {
        if (company == null) {
            return null;
        }
        List<ManufacturingSiteDto> manufacturing = new ArrayList<>();
        for (ManufacturingSite site : company.manufacturingFor(verticalId)) {
            manufacturing.add(mapSite(site, locale));
        }
        List<CompanyScoreDto> scores = new ArrayList<>();
        if (company.getScores() != null) {
            for (Map.Entry<String, CompanyScore> entry : company.getScores().entrySet()) {
                scores.add(mapScore(entry.getKey(), entry.getValue()));
            }
        }
        List<XMetaDto> xmetas = new ArrayList<>();
        if (company.getXmetas() != null) {
            for (XMeta meta : company.getXmetas()) {
                xmetas.add(mapXMeta(meta));
            }
        }
        return new CompanyDto(
                company.getId(),
                company.getName(),
                company.getParentCompanyId(),
                mapLocation(company.getHq(), locale),
                manufacturing,
                scores,
                xmetas);
    }

    private CompanyLocationDto mapLocation(CompanyLocation hq, Locale locale) {
        if (hq == null) {
            return null;
        }
        return new CompanyLocationDto(hq.getCountry(), countryName(hq.getCountry(), locale), hq.getCity(),
                hq.getLat(), hq.getLon());
    }

    private ManufacturingSiteDto mapSite(ManufacturingSite site, Locale locale) {
        return new ManufacturingSiteDto(
                site.getCategories() == null ? List.of() : site.getCategories(),
                site.getCountry(),
                countryName(site.getCountry(), locale),
                site.getCity(),
                site.getLat(),
                site.getLon(),
                site.getType() == null ? null : site.getType().jsonValue(),
                site.getOperator(),
                mapSources(site.getSources()));
    }

    private List<SourcedReferenceDto> mapSources(List<SourcedReference> sources) {
        if (sources == null) {
            return List.of();
        }
        List<SourcedReferenceDto> out = new ArrayList<>();
        for (SourcedReference ref : sources) {
            out.add(new SourcedReferenceDto(ref.getUrl(), ref.getLabel(), ref.getRetrievedAt()));
        }
        return out;
    }

    private CompanyScoreDto mapScore(String provider, CompanyScore score) {
        if (score == null) {
            return new CompanyScoreDto(provider, null, null, null, null, null);
        }
        return new CompanyScoreDto(provider, score.getValue(), score.getRating(), score.normalized(),
                score.getUrl(), score.getRetrievedAt());
    }

    private XMetaDto mapXMeta(XMeta meta) {
        return new XMetaDto(
                meta.getKey(),
                meta.getType() == null ? null : meta.getType().jsonValue(),
                meta.getValue(),
                meta.getUrl(),
                meta.getRetrievedAt(),
                meta.getValidUntil(),
                meta.getLang());
    }

    /**
     * Returns manufacturing sites for a brand/vertical sorted by a stable order
     * (factories first, then by country), for distance computation and display.
     *
     * @param brandName raw brand name
     * @param verticalId vertical id (nullable)
     * @return manufacturing sites, never {@code null}
     */
    public List<ManufacturingSite> manufacturingSites(String brandName, String verticalId) {
        List<ManufacturingSite> sites = new ArrayList<>(brandService.manufacturingSites(brandName, verticalId));
        sites.sort(Comparator.comparing(s -> StringUtils.defaultString(s.getCountry())));
        return sites;
    }

    /**
     * @param countryCode ISO-3166-1 alpha-2 code
     * @param locale display locale
     * @return localized country name, or {@code null}
     */
    public String countryName(String countryCode, Locale locale) {
        if (StringUtils.isBlank(countryCode)) {
            return null;
        }
        String name = new Locale("", countryCode).getDisplayCountry(locale == null ? Locale.ENGLISH : locale);
        return StringUtils.isBlank(name) || name.equals(countryCode) ? null : name;
    }
}
