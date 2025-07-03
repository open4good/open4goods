# Model Module

This module contains the core domain objects used across the Open4goods platform. It defines data structures for products, vertical configurations, resources and other shared types. No Spring components are declared here: the package only provides plain Java classes for reuse by the API and service modules.

## Directory structure

```
model
├─ src/main/java/org/open4goods/model     # domain classes
├─ src/main/resources                     # configuration files
└─ src/test/java                          # unit tests
```

## Packages

The main packages are:

- `org.open4goods.model.product` – product entities and helpers
- `org.open4goods.model.vertical` – configuration of product categories ("verticals")
- `org.open4goods.model.resource` – information about images, PDFs and other resources
- `org.open4goods.model.ai` – annotations used for AI generated content



## Building

From this directory you can build the module alone:

```bash
mvn clean install
```

Tests are executed automatically. To run them explicitly:

```bash
mvn test
```

When working from the repository root use:

```bash
mvn -pl model -am clean install
```

## Usage

Add the module as a dependency to other Maven projects within this repository. Example:

```xml
<dependency>
    <groupId>org.open4goods</groupId>
    <artifactId>model</artifactId>
    <version>${project.version}</version>
</dependency>
```
