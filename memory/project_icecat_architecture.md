---
name: Icecat integration architecture notes
description: Current state analysis of icecat integration - key files, problems, and decisions
type: project
---

**Current key files:**
- services/icecat/services/IcecatService.java (426 lines) - orchestrator, in-memory maps
- services/icecat/services/loader/FeatureLoader.java - loads features/groups/brands XML
- services/icecat/services/loader/CategoryLoader.java - loads categories, does text-minification hack
- api/services/completion/IcecatCompletionService.java (381 lines) - live API, WRONG MODULE
- api/model/IcecatData.java - live API JSON response model, WRONG MODULE
- api/controller/api/IcecatController.java - WRONG MODULE
- services/icecat/config/yml/IcecatConfiguration.java - DB file config (prefix: icecat-feature-config)
- api/config/yml/IcecatCompletionConfig.java - live API config (prefix: icecatCompletionConfig), WRONG MODULE
- services/icecat/src/main/resources/xsd/icecat.xsd - full XSD (1283 lines, already correct)
- services/icecat/src/main/resources/xjb/icecat.xjb - JAXB binding file (exists)

**ES pattern:** Spring Data Elasticsearch via ElasticsearchRepository<Entity, Id> - see services/product-repository/repository/ElasticProductRepository.java as reference.

**Critical bugs:**
- getCachedFile() duplicated in IcecatService, FeatureLoader, CategoryLoader
- CategoryLoader minification done by text grep (breaks on format changes)
- IcecatCompletionService uses IOUtils.toString(new URL(url)) - no timeout
- i18n in live API: hardcoded "fr" in URL, wrong language handling in feature names
- AttributeConfig.icecatFeaturesIds is Set<String> but IDs are Integer
- No refresh policy - files cached forever
- IcecatService not annotated @Service - managed as manual bean in ApiConfig

**What icecat offers that is NOT mapped yet:**
- BulletPoints / GeneratedBulletPoints
- SummaryDescription
- ProductFamily / ProductSeries (IDs)
- Variants (GTINs + differing features)
- ReleaseDate / EndOfLifeDate (format: DD-MM-YYYY)
- ReasonsToBuy (marketing content)
- FeatureLogos
- MeasuresList reference file
- FeatureValuesVocabularyList
- TaxonomyDescriptions (not yet in JSON API as of research)
- FeaturesGroups group structure (only flat feature values extracted)
