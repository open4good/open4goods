---
title: "Java quickstart"
description: "Query the Product Data API price facet from Java using the OkHttp client or Spring WebClient."
tags:
  - java
  - quickstart
  - price
scope: public
---

# Java quickstart

This guide shows how to call the price facet from Java using the standard HTTP client (Java 11+) and OkHttp.

## Prerequisites

- Java 11 or higher
- A valid API key (`pdapi_...`) from [Dashboard → API Keys](/dashboard/api-keys)

## Using the built-in HTTP client (Java 11+)

No extra dependencies needed.

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PriceClient {

    private static final String API_BASE = "https://api.product-data-api.com";
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;

    public PriceClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPrice(String gtin) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE + "/api/v1/products/" + gtin + "/price"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else if (response.statusCode() == 404) {
            return null; // Product not found - no credits consumed
        } else {
            throw new RuntimeException("API error " + response.statusCode() + ": " + response.body());
        }
    }

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("PRODUCT_DATA_API_KEY");
        PriceClient client = new PriceClient(apiKey);

        String result = client.getPrice("0885909950805");
        if (result != null) {
            System.out.println(result);
        } else {
            System.out.println("Product not found");
        }
    }
}
```

## Using OkHttp

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

```java
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PriceClientOkHttp {

    private static final String API_BASE = "https://api.product-data-api.com";
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey;

    public PriceClientOkHttp(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getPrice(String gtin) throws Exception {
        Request request = new Request.Builder()
            .url(API_BASE + "/api/v1/products/" + gtin + "/price")
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 404) return null;
            if (!response.isSuccessful()) {
                throw new RuntimeException("API error " + response.code());
            }
            return response.body() != null ? response.body().string() : null;
        }
    }
}
```

## Handling the response

Parse the JSON response with your preferred library (Jackson, Gson, etc.):

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
JsonNode payload = mapper.readTree(jsonResponse);

boolean billable = payload.at("/meta/billable").asBoolean();
int creditsConsumed = payload.at("/meta/creditsConsumed").asInt();
int creditsRemaining = payload.at("/meta/creditsRemaining").asInt();

if (billable) {
    double bestPrice = payload.at("/data/bestPrice/price").asDouble();
    String currency = payload.at("/data/bestPrice/currency").asText();
    String merchant = payload.at("/data/bestPrice/merchant").asText();
    System.out.printf("Best price: %.2f %s from %s%n", bestPrice, currency, merchant);
} else {
    System.out.println("No fresh data available. Credits consumed: " + creditsConsumed);
}
```

## Setting the API key

Never hardcode the API key. Use an environment variable:

```bash
export PRODUCT_DATA_API_KEY="pdapi_YOUR_KEY_HERE"
```

Then read it in Java:

```java
String apiKey = System.getenv("PRODUCT_DATA_API_KEY");
```

## Next steps

- [Price facet reference](/docs/products/price) - full response schema
- [Error handling](/docs/errors) - how to handle 400, 401, 402, 404
- [Python quickstart](/docs/products/price/documentation/python)
