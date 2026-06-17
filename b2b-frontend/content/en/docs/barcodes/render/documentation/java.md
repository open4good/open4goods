---
title: "Java quickstart"
description: "Generate print-ready barcodes using the Product Data API barcode.render facet from Java."
tags:
  - java
  - quickstart
  - barcode
scope: public
---

# Java quickstart - Barcode Rendering

This guide shows how to call the barcode rendering facet from Java using the built-in HTTP Client (Java 11+) and OkHttp.

## Prerequisites

- Java 11 or higher
- A valid API key (`pdapi_...`) from [Dashboard → API Keys](/dashboard/api-keys)

## Using the built-in HTTP client (Java 11+)

No extra dependencies are needed.

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BarcodeClient {

    private static final String API_BASE = "https://api.product-data-api.com";
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;

    public BarcodeClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public String renderBarcode(String jsonRequestBody) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_BASE + "/api/v1/barcodes/render"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
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
        BarcodeClient client = new BarcodeClient(apiKey);

        String jsonPayload = """
            {
              "type": "ean13",
              "data": "4006381333931",
              "format": "png",
              "width": 200,
              "height": 100,
              "metadata": {
                "copyright": "Copyright 2026 open4goods"
              }
            }
            """;

        String result = client.renderBarcode(jsonPayload);
        System.out.println(result);
    }
}
```

## Using OkHttp

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

```java
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BarcodeClientOkHttp {

    private static final String API_BASE = "https://api.product-data-api.com";
    private final OkHttpClient client = new OkHttpClient();
    private final String apiKey;

    public BarcodeClientOkHttp(String apiKey) {
        this.apiKey = apiKey;
    }

    public String renderBarcode(String jsonRequestBody) throws Exception {
        RequestBody body = RequestBody.create(
            jsonRequestBody,
            MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url(API_BASE + "/api/v1/barcodes/render")
            .addHeader("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("API error " + response.code() + ": " + response.body().string());
            }
            return response.body() != null ? response.body().string() : null;
        }
    }
}
```

## Parsing the Response

Use Jackson or Gson to parse the returned JSON response to extract the signed `assetUrl`:

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
JsonNode payload = mapper.readTree(jsonResponse);

String assetUrl = payload.get("assetUrl").asText();
String expiresAt = payload.get("expiresAt").asText();

System.out.println("Barcode URL: " + assetUrl);
System.out.println("Expires at: " + expiresAt);
```

## Next steps

- [Barcode render reference](/docs/barcodes/render) - full options and symbologies
- [Python quickstart](/docs/barcodes/render/documentation/python)
- [Live playground](/docs/barcodes/render/playground)
