---
title: "Java quickstart"
description: "Validate barcodes and retrieve product metadata using the Product Data API barcode.check facet from Java."
tags:
  - java
  - quickstart
  - barcode
  - validation
scope: public
---

# Java quickstart - Barcode Validity Check

This guide shows how to call the free barcode validity check endpoint from Java using the built-in HTTP Client (Java 11+).

## Prerequisites

- Java 11 or higher
- No API key needed for the public endpoint (optional for higher rate limits)

## Public endpoint (no authentication)

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BarcodeCheckClient {

    private static final String API_BASE = "https://api.product-data-api.com";
    private final HttpClient client = HttpClient.newHttpClient();

    public String checkBarcode(String barcode) throws Exception {
        String encodedBarcode = URLEncoder.encode(barcode, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE + "/api/v1/barcodes/check?barcode=" + encodedBarcode))
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("API error " + response.statusCode() + ": " + response.body());
        }
    }

    public static void main(String[] args) throws Exception {
        BarcodeCheckClient client = new BarcodeCheckClient();
        String result = client.checkBarcode("3017620422003");
        System.out.println(result);
    }
}
```

## Authenticated endpoint (higher rate limits)

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BarcodeCheckClientAuth {

    private static final String API_BASE = "https://api.product-data-api.com";
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;

    public BarcodeCheckClientAuth(String apiKey) {
        this.apiKey = apiKey;
    }

    public String checkBarcode(String gtin) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE + "/api/v1/barcodes/" + gtin + "/check"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("API error " + response.statusCode() + ": " + response.body());
        }
    }

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("PRODUCT_DATA_API_KEY");
        BarcodeCheckClientAuth client = new BarcodeCheckClientAuth(apiKey);
        String result = client.checkBarcode("3017620422003");
        System.out.println(result);
    }
}
```

## Parsing the response

Use Jackson to parse the forensics and product teaser fields:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
JsonNode root = mapper.readTree(jsonResponse);

JsonNode forensics = root.get("forensics");
boolean valid = forensics.get("valid").asBoolean();
String type = forensics.get("type").asText();
String country = forensics.path("issuingCountryName").asText(null);
String gs1Class = forensics.path("gs1Class").asText(null);

System.out.println("Valid: " + valid);
System.out.println("Type: " + type);
System.out.println("Country: " + country);
System.out.println("GS1 class: " + gs1Class);

JsonNode product = root.path("product");
if (!product.isMissingNode() && !product.isNull()) {
    System.out.println("Product: " + product.path("title").asText());
    System.out.println("Best price: " + product.path("bestPrice").asDouble()
        + " " + product.path("currency").asText());
    System.out.println("Offers: " + product.path("offersCount").asInt());
}
```

## Next steps

- [Barcode check reference](/docs/barcodes/check) - full response schema and GS1 class reference
- [Python quickstart](/docs/barcodes/check/documentation/python)
- [Live playground](/docs/barcodes/check/playground)
