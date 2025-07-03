# Product Repository

The **Product Repository** service handles indexing and retrieval of products in an Elasticsearch cluster. It provides queue based workers for ingesting full product documents and partial updates so that writes do not block user facing requests.

## Purpose

- Index products into Elasticsearch for fast search and filtering
- Retrieve products by identifiers or search criteria
- Process full updates and partial updates via background worker threads

## Directory Structure

```
services/product-repository
├── pom.xml
├── src
│   └── main
│       ├── java
│       │   └── org
│       │       └── open4goods
│       │           └── services
│       │               └── productrepository
│       │                   ├── config
│       │                   │   └── IndexationConfig.java
│       │                   ├── repository
│       │                   │   └── ElasticProductRepository.java
│       │                   ├── services
│       │                   │   └── ProductRepository.java
│       │                   └── workers
│       │                       ├── FullProductIndexationWorker.java
│       │                       └── PartialProductIndexationWorker.java
│       └── resources
```

## Configuration

The `IndexationConfig` class exposes properties controlling worker counts, queue sizes and bulk sizes used when indexing data. Adjust these values in your Spring configuration:

```java
IndexationConfig config = new IndexationConfig();
config.setProductWorkers(2);                // number of threads for full products
config.setPartialProductWorkers(2);         // number of threads for partial updates
config.setProductsQueueMaxSize(5000);       // queue capacity for full products
```

## Example Usage

Create a repository instance with a configured `IndexationConfig` and enqueue products for indexing. All writes are processed asynchronously by the workers.

```java
IndexationConfig config = new IndexationConfig();
ProductRepository repo = new ProductRepository(config);

// index a product
repo.save(product);

// export all indexed products as a stream
Stream<Product> all = repo.exportAll();
```

## How to Build

From this directory:

```bash
mvn clean install
```

Or from the repository root:

```bash
mvn -pl services/product-repository -am clean install
```

## Testing

Run the tests with:

```bash
mvn test
```

Contributions are welcome! Please ensure that code changes include documentation and tests where applicable.
