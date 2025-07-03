# Crawler Module

This module runs standalone crawlers that scrape product data from external websites and feed it to the open4goods ingestion pipeline. It exposes a Spring Boot application so crawler nodes can be deployed independently from the API.

## Building

From this directory you can build and test the crawler with Maven:

```bash
mvn clean install        # build and run tests
mvn test                 # run tests only
```

Alternatively, from the repository root run:

```bash
mvn -pl crawler -am clean install
```

(See [AGENTS.md](AGENTS.md) for details.)

## Usage

The crawler can be started as any Spring Boot application. Example using the development profile:

```bash
java -Dspring.profiles.active=dev -jar target/bin/open4goods-crawler.jar
```

Once running, the crawler interface will be available at [http://localhost:8080](http://localhost:8080).

For more information about the embedded crawler4j library and our patches to it, refer to [src/main/java/edu/uci/ics/crawler4j/README.md](src/main/java/edu/uci/ics/crawler4j/README.md).
