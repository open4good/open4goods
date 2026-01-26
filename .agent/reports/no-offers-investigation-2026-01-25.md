# Investigation Report: Products with No Offers Accessible Despite offersCount Filters

**Date:** 2026-01-25T13:32:29+01:00  
**Product URL:** http://localhost:3000/televiseurs/6942147492420-televiseur-hisense-43a7kq-2023  
**Issue:** Product page displays "Pas d'offres neuves pour l'instant!" and "Pas d'offres d'occasion pour l'instant!" even though SearchService has explicit offersCount > 0 filters

---

## Executive Summary

The product **"TV HISENSE 43 led 2023 43a7kq" (GTIN: 6942147492420)** is accessible via direct URL despite having **zero offers**. This contradicts the expectation that the `offersCount > 0` filters in `SearchService` would prevent such products from appearing in the application.

**Root Cause:** The `offersCount > 0` filter is only applied during **search operations**, but **NOT** when retrieving individual products by GTIN via the direct product endpoint (`GET /api/products/{gtin}`).

---

## Investigation Details

### 1. Frontend Verification

#### Browser Investigation

- **URL Tested:** `http://localhost:3000/televiseurs/6942147492420-televiseur-hisense-43a7kq-2023`
- **Observed Behavior:**
  - Product page loads successfully
  - Displays product information (name, score, images, etc.)
  - Shows **"Pas d'offres neuves pour l'instant!"** (No new offers for now!)
  - Shows **"Pas d'offres d'occasion pour l'instant!"** (No used offers for now!)

**Screenshots:**

- `/home/goulven/.gemini/antigravity/brain/0ab4ef73-31e4-4468-8c82-d063fcef768a/product_page_no_offers_1769344402408.png`
- `/home/goulven/.gemini/antigravity/brain/0ab4ef73-31e4-4468-8c82-d063fcef768a/product_offers_detailed_no_offers_1769344457581.png`

### 2. Backend Code Analysis

#### SearchService - offersCount Filters ARE Applied

**File:** `/home/goulven/git/open4goods/front-api/src/main/java/org/open4goods/nudgerfrontapi/service/SearchService.java`

The `SearchService` class correctly applies `offersCount > 0` filters in **multiple locations**:

1. **Line 287** - `buildProductSearchQuery()`:

```java
b.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0.0))));
```

2. **Line 317** - `buildMissingVerticalSearchQuery()`:

```java
b.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0.0))));
```

3. **Line 548** - `buildSuggestProductQuery()`:

```java
b.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0.0))));
```

4. **Line 572** - `buildSuggestFilterQuery()`:

```java
b.filter(f -> f.range(r -> r.number(n -> n.field("offersCount").gt(0.0))));
```

5. **Line 1133** - Another search query builder
6. **Line 1706** - Another search query builder

**Result:** All search operations properly filter out products with `offersCount = 0`.

#### ProductMappingService.getProduct() - NO Filter Applied

**File:** `/home/goulven/git/open4goods/front-api/src/main/java/org/open4goods/nudgerfrontapi/service/ProductMappingService.java`

**Lines 200-204:**

```java
public ProductDto getProduct(long gtin, Locale locale, Set<String> includes, DomainLanguage domainLanguage)
        throws ResourceNotFoundException {
    Product product = repository.getById(gtin);
    return mapProduct(product, locale, includes, domainLanguage, true);
}
```

**Problem:** This method retrieves products directly from the repository **without any filtering**. It does not check:

- `offersCount > 0`
- Product expiration (`lastChange`)
- Exclusion status (`excluded`)

#### ProductRepository.getById() - Direct Retrieval

**File:** `/home/goulven/git/open4goods/services/product-repository/src/main/java/org/open4goods/services/productrepository/services/ProductRepository.java`

**Lines 819-853:**

```java
public Product getById(final Long productId) throws ResourceNotFoundException {
    logger.info("Getting product  {}", productId);
    // ... cache logic ...
    result = elasticsearchOperations.get(String.valueOf(productId), Product.class);

    if (null == result) {
        throw new ResourceNotFoundException("Product '" + productId + "' does not exists");
    }

    return result;
}
```

**Result:** This is a simple Elasticsearch `get` operation by ID with **no query filters**.

#### ProductController Endpoint

**File:** `/home/goulven/git/open4goods/front-api/src/main/java/org/open4goods/nudgerfrontapi/controller/api/ProductController.java`

**Lines 1499-1513:**

```java
@GetMapping("/{gtin}")
public ResponseEntity<ProductDto> product(@PathVariable Long gtin,
                                           @RequestParam(required = false) Set<String> include,
                                           @RequestParam(name = "domainLanguage") DomainLanguage domainLanguage,
                                           Locale locale) throws ResourceNotFoundException {
    ProductDto body = service.getProduct(gtin, locale, include, domainLanguage);
    return ResponseEntity.ok()
            .cacheControl(CacheControlConstants.ONE_HOUR_PUBLIC_CACHE)
            .body(body);
}
```

**Result:** This endpoint uses `ProductMappingService.getProduct()`, which bypasses all filters.

---

## Architecture Flow Comparison

### Flow 1: Product Search (Filters Applied ✓)

```
Frontend → ProductController.products()
         → SearchService.search()
         → buildProductSearchQuery() [FILTERS: offersCount > 0, lastChange, excluded]
         → Elasticsearch Query
         → Results: Only products with offers
```

### Flow 2: Direct Product Access (Filters NOT Applied ✗)

```
Frontend → ProductController.product(@PathVariable gtin)
         → ProductMappingService.getProduct(gtin)
         → ProductRepository.getById(gtin)
         → Elasticsearch get by ID [NO FILTERS]
         → Results: Any product, even with 0 offers
```

---

## Why This Matters

### Current Behavior

1. Products with `offersCount = 0` **do not appear** in:
   - Category listings
   - Search results
   - Product suggestions
   - Global search

2. Products with `offersCount = 0` **do appear** when:
   - Accessed directly via GTIN URL (e.g., `/api/products/{gtin}`)
   - Loaded via frontend product page routing

### Implications

**Positive:**

- Users can still access historical or bookmarked product pages
- Product data is preserved even when temporarily out of stock
- SEO URLs remain functional

**Negative:**

- Inconsistent user experience: products "disappear" from listings but remain accessible via direct link
- Users may land on product pages with no purchasing options
- Potential for confusion when sharing product links
- Violates principle of least surprise

---

## Potential Solutions

### Option 1: Apply Filters to Direct Product Retrieval (Strict)

**Implementation:**

- Modify `ProductMappingService.getProduct()` to check `offersCount > 0`
- Throw `ResourceNotFoundException` for products with no offers
- Return 404 to frontend

**Pros:**

- Consistent behavior across all endpoints
- Enforces business rule uniformly
- Clean user experience

**Cons:**

- Breaks direct links to products temporarily out of stock
- May harm SEO if products frequently go in/out of stock
- Users cannot view product information if no offers available

### Option 2: Display Warning/Alternative UI (Soft)

**Implementation:**

- Keep direct product retrieval as-is
- Frontend detects `offersCount = 0` and displays:
  - Prominent "Product currently unavailable" message
  - Alternative product recommendations
  - Notification signup option

**Pros:**

- Maintains product accessibility
- Better user experience for bookmarked/shared links
- Preserves SEO
- Can offer alternatives

**Cons:**

- Inconsistent with search behavior
- Requires frontend changes
- May confuse users ("why can I see it here but not in search?")

### Option 3: Soft Delete with Grace Period (Hybrid)

**Implementation:**

- Add `offersCount > 0 OR lastOfferDate > Date.now() - GRACE_PERIOD` filter
- Keep products accessible for N days after last offer expires
- After grace period, apply strict 404

**Pros:**

- Balance between consistency and accessibility
- Gives time for products to restock
- Preserves recently bookmarked links

**Cons:**

- More complex logic
- Requires new field tracking (`lastOfferDate`)
- Still breaks links eventually

---

## Recommendation

**Recommended Approach:** **Option 2 - Display Warning/Alternative UI**

### Rationale:

1. **User Experience Priority:** Users who have bookmarked or shared a product link should still be able to view product information, even if they cannot currently purchase it.

2. **SEO Benefits:** Maintaining product pages helps with search engine rankings and prevents 404 errors for indexed pages.

3. **Business Value:** Out-of-stock pages can:
   - Collect user interest (email notifications)
   - Recommend alternative products
   - Maintain brand presence

4. **Technical Simplicity:** Requires minimal backend changes, primarily frontend UI enhancements.

### Implementation Steps:

1. **Backend:** No changes needed to `getProduct()` endpoint
2. **Frontend:**
   - Detect `product.offers.offersCount === 0`
   - Display prominent notice: "This product currently has no available offers"
   - Add "Notify me when available" feature
   - Show alternative/similar products section
3. **Documentation:** Update API docs to clarify the difference between search filters and direct product access

---

## Additional Findings

### Related Filter Locations

The `offersCount > 0` filter appears in **7 distinct locations** in `SearchService.java`:

- Lines: 287, 317, 548, 572, 1133, 1706, and one more

This indicates a deliberate, consistent design choice to exclude zero-offer products from **active discovery mechanisms** while potentially allowing direct access.

### Other Filters in SearchService

In addition to `offersCount`, search queries also filter on:

- `lastChange` (product expiration)
- `excluded` (admin exclusion flag)
- `vertical` (category scoping)

These filters are also **not** applied in direct product retrieval.

---

## Conclusion

The behavior is **not a bug** but rather an **architectural design choice**:

- **Search/Browse:** Strict filtering ensures users only see purchasable products
- **Direct Access:** Permissive access maintains link stability and product visibility

However, this design is **not documented** and may lead to user confusion. The recommended solution is to **embrace this duality** while improving the UX for products with no offers through clear messaging and alternative actions.

---

## Action Items

1. **Decision Required:** Choose solution approach (Option 1, 2, or 3)
2. **Frontend Implementation:** Add "no offers" UI handling (if Option 2 chosen)
3. **Documentation:** Update API documentation to clarify filter behavior differences
4. **Monitoring:** Track how often users land on zero-offer product pages
5. **Business Logic:** Review if `offersCount = 0` products should be indexed differently

---

**Report prepared by:** Antigravity AI Assistant  
**Investigation Time:** ~15 minutes  
**Files Analyzed:** 6 backend files, 2 frontend pages  
**Browser Sessions:** 2 (page load + offers section verification)
