---
title: "Démarrage rapide Java"
description: "Générez des codes-barres prêts à l'impression avec l'API Product Data de open4goods en Java."
tags:
  - java
  - quickstart
  - barcode
scope: public
---

# Démarrage rapide Java - Rendu de code-barres

Ce guide explique comment appeler le point de terminaison de génération de code-barres en Java à l'aide du client HTTP natif (Java 11+) et de la bibliothèque OkHttp.

## Prérequis

- Java 11 ou version supérieure
- Une clé API active (`pdapi_...`) récupérée depuis le [Tableau de bord → Clés API](/dashboard/api-keys)

## Utilisation du client HTTP natif (Java 11+)

Aucune dépendance externe n'est requise.

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
            throw new RuntimeException("Erreur API " + response.statusCode() + " : " + response.body());
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

## Utilisation de OkHttp

Ajoutez la dépendance suivante à votre fichier `pom.xml` :

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
                throw new RuntimeException("Erreur API " + response.code() + " : " + response.body().string());
            }
            return response.body() != null ? response.body().string() : null;
        }
    }
}
```

## Extraction de la réponse

Utilisez Jackson ou Gson pour analyser la réponse JSON reçue afin de récupérer l'URL signée de l'image de code-barres :

```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper mapper = new ObjectMapper();
JsonNode payload = mapper.readTree(jsonResponse);

String assetUrl = payload.get("assetUrl").asText();
String expiresAt = payload.get("expiresAt").asText();

System.out.println("URL de l'image : " + assetUrl);
System.out.println("Expire le : " + expiresAt);
```

## Liens rapides

- [Spécification de rendu](/fr/docs/barcodes/render) - options de symbologies
- [Démarrage rapide Python](/fr/docs/barcodes/render/documentation/python)
- [Bac à sable (Playground) interactif](/fr/docs/barcodes/render/playground)
