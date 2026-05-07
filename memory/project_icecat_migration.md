---
name: Icecat migration plan
description: Agreed 5-phase plan to migrate the icecat integration to production grade, including ES indexes, JAXB codegen, module consolidation, category tooling, and live API completeness
type: project
---

Full 5-phase icecat migration agreed with user (2026-05-07). See ADR once written.

**Why:** Current integration is prototype-quality: 3× duplicated getCachedFile(), in-memory HashMaps for 100k+ features, hand-coded POJOs that break on schema drift, 19 TODOs in live API service, CompletionService in wrong module (api/ instead of services/icecat/), no refresh policy, broken i18n, incomplete live API mapping.

**How to apply:** When working on any icecat-related code, follow this phase order:
- Phase 1: JAXB generation from existing XSD (services/icecat/src/main/resources/xsd/icecat.xsd + icecat.xjb)
- Phase 2: Spring Data Elasticsearch indexes (icecat-feature, icecat-category, icecat-feature-group, icecat-supplier) replacing in-memory HashMaps
- Phase 3: Consolidate all icecat code into services/icecat module (move IcecatCompletionService, IcecatData, IcecatController, IcecatCompletionConfig from api/)
- Phase 4: Admin REST endpoints for manual vertical↔icecat category mapping (no auto-fuzzy matching)
- Phase 5: Complete live API mapping (BulletPoints, SummaryDescription, Variants, ReleaseDate, ProductFamily, fix i18n)

**Constraints from user:**
- Category matching: fully manual, tooled via admin API endpoints
- Language: French only for now, but architecture must support multiple languages later (pass language as parameter everywhere, never hardcode "fr")
- Credentials: Open Icecat free tier only
- ES: icecat module is allowed to depend on services/product-repository or shared ES client
- All 5 phases must be done
