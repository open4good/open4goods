# Product Repository Service

This service is part of the [open4goods](https://github.com/open4good/open4goods) project. It stores and retrieves aggregated product data from Elasticsearch.

## Overview

Main responsibilities:

- Persist `Product` documents and perform search queries.
- Export and stream results for other modules.
- Handle asynchronous indexation through worker threads.

## Configuration

Indexation parameters are provided by `IndexationConfig`:

| Property | Default | Description |
| --- | --- | --- |
| `productsQueueMaxSize` | 5000 | Queue size for full products. |
| `partialProductsQueueMaxSize` | 5000 | Queue size for partial updates. |
| `datafragmentQueueMaxSize` | 20000 | Queue size for data fragments. |
| `productsbulkPageSize` | 200 | Bulk size for full products. |
| `partialProductsbulkPageSize` | 300 | Bulk size for partial products. |
| `dataFragmentbulkPageSize` | 200 | Bulk size for data fragments. |
| `productWorkers` | 2 | Number of worker threads for full products. |
| `partialProductWorkers` | 2 | Worker threads for partial updates. |
| `dataFragmentworkers` | 2 | Worker threads for data fragment aggregation. |
| `pauseDuration` | 4000 | Pause duration (ms) when queues are empty. |

## Usage Example

```java
@Autowired
private ProductRepository productRepository;

public Product loadProduct(long id) throws ResourceNotFoundException {
    return productRepository.getById(id);
}
```

## Build & Test

Build from this folder:

```bash
mvn clean install
```

Run tests:

```bash
mvn test
```

You can also build from the project root:

```bash
mvn -pl services/product-repository -am clean install
```

For project-wide information see the [main README](../../README.md).
