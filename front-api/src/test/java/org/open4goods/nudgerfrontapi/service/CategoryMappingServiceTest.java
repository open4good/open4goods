package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.Localisable;
import org.open4goods.model.vertical.AiPromptsConfig;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParserConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreCriteria;
import org.open4goods.model.vertical.ImpactScoreTexts;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.ResourcesAggregationConfig;
import org.open4goods.model.vertical.SiteNaming;
import org.open4goods.model.vertical.SubsetCriteria;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.model.vertical.WikiPageConfig;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;

class CategoryMappingServiceTest {

    private CategoryMappingService service;

    @BeforeEach
    void setUp() {
        service = new CategoryMappingService();
    }

    @Test
    void toVerticalConfigDtoMapsBasicFields() {
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setEnabled(true);
        config.setOrder(1);
        config.setGoogleTaxonomyId(404);
        config.setIcecatTaxonomyId(1584);

        ProductI18nElements fr = new ProductI18nElements();
        fr.setVerticalHomeTitle("Téléviseurs");
        fr.setVerticalHomeDescription("Desc");
        fr.setVerticalHomeUrl("televiseurs");
        config.setI18n(Map.of("fr", fr));

        VerticalConfigDto dto = service.toVerticalConfigDto(config, DomainLanguage.fr);

        assertThat(dto.id()).isEqualTo("tv");
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.googleTaxonomyId()).isEqualTo(404);
        assertThat(dto.image()).isNull();
        assertThat(dto.singularName()).isNull();
        assertThat(dto.verticalHomeTitle()).isEqualTo("Téléviseurs");
        assertThat(dto.verticalHomeDescription()).isEqualTo("Desc");
        assertThat(dto.verticalHomeUrl()).isEqualTo("televiseurs");
    }

    @Test
    void toVerticalConfigFullDtoResolvesLocalisedFields() {
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setEnabled(true);
        config.setOrder(2);
        config.setGoogleTaxonomyId(404);
        config.setIcecatTaxonomyId(1584);
        config.setEcoFilters(List.of("eco"));
        config.setTechnicalFilters(List.of("tech"));
        config.setGlobalTechnicalFilters(List.of("global"));
        config.setMatchingCategories(Map.of("all", Set.of("category")));
        config.setExcludingTokensFromCategoriesMatching(Set.of("accessoire"));
        config.setGenerationExcludedFromCategoriesMatching(Set.of("fnac"));
        config.setGenerationExcludedFromAttributesMatching(Set.of("PRICE"));
        config.setRequiredAttributes(Set.of("ENERGY_CLASS"));
        config.setForceNameGeneration(true);
        config.setBrandsAlias(Map.of("LG ELECTRONICS", "LG"));
        config.setBrandsExclusion(Set.of("UNKNOWN"));

        SiteNaming siteNaming = new SiteNaming();
        Localisable<String, String> serverNames = new Localisable<>();
        serverNames.put("fr", "Serveur FR");
        siteNaming.setServerNames(serverNames);
        Localisable<String, String> baseUrls = new Localisable<>();
        baseUrls.put("fr", "https://fr.example.com");
        siteNaming.setBaseUrls(baseUrls);
        config.setNamings(siteNaming);

        config.setResourcesConfig(new ResourcesAggregationConfig());

        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("WEIGHT");
        attributeConfig.setParser(new AttributeParserConfig());
        Localisable<String, String> attributeName = new Localisable<>();
        attributeName.put("fr", "Poids");
        attributeConfig.setName(attributeName);
        Localisable<String, String> attributeUnit = new Localisable<>();
        attributeUnit.put("fr", "kg");
        attributeConfig.setUnit(attributeUnit);
        attributeConfig.setIcecatFeaturesIds(Set.of("10"));
        AttributesConfig attributesConfig = new AttributesConfig();
        attributesConfig.setConfigs(List.of(attributeConfig));
        attributesConfig.setFeaturedValues(Set.of("VALUE"));
        attributesConfig.setExclusions(Set.of("EXCLUDED"));
        config.setAttributesConfig(attributesConfig);

        ImpactScoreCriteria criterion = new ImpactScoreCriteria();
        criterion.setKey("repairability");
        Localisable<String, String> criterionTitle = new Localisable<>();
        criterionTitle.put("fr", "Réparabilité");
        criterion.setTitle(criterionTitle);
        Localisable<String, String> criterionDescription = new Localisable<>();
        criterionDescription.put("fr", "Description FR");
        criterion.setDescription(criterionDescription);
        config.setAvailableImpactScoreCriterias(Map.of("repairability", criterion));

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("repairability", 0.5));
        ImpactScoreTexts impactScoreTexts = new ImpactScoreTexts();
        impactScoreTexts.setPurpose("But");
        Localisable<String, ImpactScoreTexts> texts = new Localisable<>();
        texts.put("fr", impactScoreTexts);
        impactScoreConfig.setTexts(texts);
        config.setImpactScoreConfig(impactScoreConfig);

        VerticalSubset subset = new VerticalSubset();
        subset.setId("premium");
        subset.setGroup("default");
        subset.setCriterias(List.of(new SubsetCriteria("score", SubsetCriteriaOperator.GREATER_THAN, "10")));
        subset.setImage("subset.jpg");
        Localisable<String, String> subsetUrl = new Localisable<>();
        subsetUrl.put("fr", "/premium");
        subset.setUrl(subsetUrl);
        Localisable<String, String> subsetCaption = new Localisable<>();
        subsetCaption.put("fr", "Caption");
        subset.setCaption(subsetCaption);
        Localisable<String, String> subsetTitle = new Localisable<>();
        subsetTitle.put("fr", "Premium");
        subset.setTitle(subsetTitle);
        Localisable<String, String> subsetDescription = new Localisable<>();
        subsetDescription.put("fr", "Description subset");
        subset.setDescription(subsetDescription);
        config.setSubsets(List.of(subset));
        config.setBrandsSubset(subset);

        FeatureGroup featureGroup = new FeatureGroup();
        featureGroup.setIcecatCategoryFeatureGroupId(1);
        featureGroup.setFeaturesId(List.of(10));
        Localisable<String, String> featureGroupName = new Localisable<>();
        featureGroupName.put("fr", "Caractéristiques");
        featureGroup.setName(featureGroupName);
        config.setFeatureGroups(List.of(featureGroup));

        ProductI18nElements fr = new ProductI18nElements();
        PrefixedAttrText url = new PrefixedAttrText();
        url.setPrefix("prefix");
        url.setAttrs(List.of("model"));
        fr.setUrl(url);
        PrefixedAttrText h1 = new PrefixedAttrText();
        h1.setPrefix("H1");
        fr.setH1Title(h1);
        fr.setProductMetaTitle("Meta Title");
        fr.setProductMetaDescription("Meta Description");
        fr.setProductMetaOpenGraphTitle("OG Title");
        fr.setProductMetaOpenGraphDescription("OG Description");
        fr.setProductMetaTwitterTitle("Twitter Title");
        fr.setProductMetaTwitterDescription("Twitter Description");
        fr.setVerticalMetaTitle("Vertical Meta Title");
        fr.setVerticalMetaDescription("Vertical Meta Description");
        fr.setVerticalMetaOpenGraphTitle("Vertical OG Title");
        fr.setVerticalMetaOpenGraphDescription("Vertical OG Description");
        fr.setVerticalMetaTwitterTitle("Vertical Twitter Title");
        fr.setVerticalMetaTwitterDescription("Vertical Twitter Description");
        fr.setVerticalHomeTitle("Téléviseurs");
        fr.setVerticalHomeDescription("Desc");
        fr.setVerticalHomeUrl("televiseurs");
        WikiPageConfig wikiPageConfig = new WikiPageConfig();
        wikiPageConfig.setTitle("Wiki");
        fr.setWikiPages(List.of(wikiPageConfig));
        AiPromptsConfig aiPromptsConfig = new AiPromptsConfig();
        aiPromptsConfig.setRootPrompt("Root prompt");
        fr.setAiConfigs(aiPromptsConfig);
        config.setI18n(Map.of("fr", fr));

        VerticalConfigFullDto dto = service.toVerticalConfigFullDto(config, DomainLanguage.fr);

        assertThat(dto.id()).isEqualTo("tv");
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.verticalHomeTitle()).isEqualTo("Téléviseurs");
        assertThat(dto.verticalHomeDescription()).isEqualTo("Desc");
        assertThat(dto.verticalHomeUrl()).isEqualTo("televiseurs");
        assertThat(dto.productMetaTitle()).isEqualTo("Meta Title");
        assertThat(dto.verticalMetaDescription()).isEqualTo("Vertical Meta Description");
        assertThat(dto.wikiPages()).hasSize(1);
        assertThat(dto.aiConfigs().getRootPrompt()).isEqualTo("Root prompt");
        assertThat(dto.ecoFilters()).containsExactly("eco");
        assertThat(dto.technicalFilters()).containsExactly("tech");
        assertThat(dto.globalTechnicalFilters()).containsExactly("global");
        assertThat(dto.matchingCategories()).containsKey("all");
        assertThat(dto.excludingTokensFromCategoriesMatching()).contains("accessoire");
        assertThat(dto.generationExcludedFromCategoriesMatching()).contains("fnac");
        assertThat(dto.generationExcludedFromAttributesMatching()).contains("PRICE");
        assertThat(dto.requiredAttributes()).contains("ENERGY_CLASS");
        assertThat(dto.forceNameGeneration()).isTrue();
        assertThat(dto.brandsAlias()).containsEntry("LG ELECTRONICS", "LG");
        assertThat(dto.brandsExclusion()).contains("UNKNOWN");
        assertThat(dto.namings().serverName()).isEqualTo("Serveur FR");
        assertThat(dto.namings().baseUrl()).isEqualTo("https://fr.example.com");
        assertThat(dto.attributesConfig().configs()).hasSize(1);
        assertThat(dto.attributesConfig().configs().get(0).name()).isEqualTo("Poids");
        assertThat(dto.attributesConfig().configs().get(0).unit()).isEqualTo("kg");
        assertThat(dto.availableImpactScoreCriterias()).containsKey("repairability");
        assertThat(dto.availableImpactScoreCriterias().get("repairability").title()).isEqualTo("Réparabilité");
        assertThat(dto.impactScoreConfig().texts().getPurpose()).isEqualTo("But");
        assertThat(dto.subsets()).hasSize(1);
        assertThat(dto.subsets().get(0).title()).isEqualTo("Premium");
        assertThat(dto.brandsSubset().url()).isEqualTo("/premium");
        assertThat(dto.featureGroups()).hasSize(1);
        assertThat(dto.featureGroups().get(0).name()).isEqualTo("Caractéristiques");
    }
}
