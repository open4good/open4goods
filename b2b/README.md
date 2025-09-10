# B2B API

This module provides the REST API for B2B clients.

## Overall code structure

The core code lives in `src/main/java/org/open4goods/b2b` and follows a hexagonal architecture:

- `controller/api` – exposes REST endpoints (e.g., `ProductController`).
- `service` – business logic and orchestration.
- `service/facets` – computes optional facets (`PriceFacetService`).
- `dto/product` – data transfer objects exposed via the API (`ProductDto`).
- `model/facets` – definitions of facets returned by the API (`PriceFacet`, etc.).

## Facet definition

A *facet* is an optional piece of information that a client can request through the `include` parameter. Available facets are listed in `model.AvailableFacets`.

Each facet includes:

1. a model class in `model/facets` describing the returned data;
2. a service implementing `FacetServiceInterface` that produces it;
3. an annotated field in `ProductDto`;
4. mapping logic in `ProductAccessService`.

### Existing facets

| Name  | Description                                         | Model class  | Service             |
|-------|-----------------------------------------------------|--------------|---------------------|
| Price | Gross price and currency of the most relevant offer | `PriceFacet` | `PriceFacetService` |

`PriceFacet` exposes two fields:

- `price` (`float`) – gross amount;
- `currency` (`String`) – ISO 4217 currency code.

### Adding a new facet

1. Add the entry to `model.AvailableFacets`.
2. Create the class under `model/facets` extending `AbstractFacet`.
3. Implement the service in `org.open4goods.b2b.service.facets` and implement `FacetServiceInterface`.
4. Add an annotated field in `ProductDto`.
5. Map the facet in `ProductAccessService`.

