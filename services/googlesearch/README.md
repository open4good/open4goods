# GoogleSearch Service

This service is part of the [open4goods](https://github.com/open4good/open4goods) project. It provides a Spring Boot-based integration with the Google Custom Search API.

## Overview

The **GoogleSearch Service** allows you to perform searches against the Google Custom Search API using externalized configuration. It also integrates with Spring Boot Actuator for health checks and metrics.

## Features

- **Custom Search API Integration:** Execute searches using a configurable API key, search engine ID (cx), and search URL.
- **Health Check:** The service implements a health indicator that checks if the necessary properties are configured and if the last search call was successful.
- **Metrics:** Each search increments an actuator metric (`google.search.count`).

## Configuration

Configuration properties can be set in your `application.yml` or `application-test.yml`. For example:

```yaml
googlesearch:
  apiKey: "YOUR_API_KEY"
  cx: "YOUR_CX"
  searchUrl: "https://www.googleapis.com/customsearch/v1"
```

## How to Use

1. **Include the Dependency:**

   Make sure to include the `googlesearch` module as a dependency in your project (it is built as a JAR).

2. **Autowire the Service:**

   In your Spring Boot application, autowire the `GoogleSearchService`:
   
   ```java
   @Autowired
   private GoogleSearchService googleSearchService;
   ```

3. **Perform a Search:**

   Create a search request and execute the search:
   
   ```java
   GoogleSearchRequest request = new GoogleSearchRequest("Spring Boot", 5);
   try {
       GoogleSearchResponse response = googleSearchService.search(request);
       response.getResults().forEach(result ->
           System.out.println("Title: " + result.getTitle() + ", Link: " + result.getLink())
       );
   } catch (Exception e) {
       // Handle error
       e.printStackTrace();
   }
   ```

## Testing

A sample unit test is provided in `src/test/java/org/open4goods/googlesearch/GoogleSearchServiceTest.java`. The test configuration is bootstrapped using an `application-test.yml`.

## Maven Build

The Maven POM is configured with the necessary dependencies and plugins. To build the service, run:

```bash
mvn clean install
```

## License

This project is licensed under the terms of the [MIT License](LICENSE).
