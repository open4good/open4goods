---
title: "Quickstart Java"
description: "Interrogez la facette prix de Product Data API depuis Java avec le client HTTP intégré ou OkHttp."
tags:
  - java
  - quickstart
  - price
scope: public
---

# Quickstart Java

Ce guide montre comment appeler la facette prix depuis Java avec le client HTTP standard (Java 11+) et OkHttp.

## Prérequis

- Java 11 ou supérieur
- Une clé API valide (`pdapi_...`) depuis [Tableau de bord → Clés API](/dashboard/api-keys)

## Avec le client HTTP intégré (Java 11+)

Aucune dépendance supplémentaire nécessaire.

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
            return null; // Produit introuvable - aucun crédit consommé
        } else {
            throw new RuntimeException("Erreur API " + response.statusCode() + ": " + response.body());
        }
    }

    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("PRODUCT_DATA_API_KEY");
        PriceClient client = new PriceClient(apiKey);

        String result = client.getPrice("0885909950805");
        if (result != null) {
            System.out.println(result);
        } else {
            System.out.println("Produit introuvable");
        }
    }
}
```

## Avec OkHttp

Ajoutez à votre `pom.xml` :

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
                throw new RuntimeException("Erreur API " + response.code());
            }
            return response.body() != null ? response.body().string() : null;
        }
    }
}
```

## Lire la clé API

Ne codez jamais la clé en dur. Utilisez une variable d'environnement :

```bash
export PRODUCT_DATA_API_KEY="pdapi_VOTRE_CLÉ_ICI"
```

```java
String apiKey = System.getenv("PRODUCT_DATA_API_KEY");
```

## Étapes suivantes

- [Référence facette prix](/docs/products/price) - schéma complet de la réponse
- [Gestion des erreurs](/docs/errors) - comment gérer 400, 401, 402, 404
- [Quickstart Python](/docs/products/price/documentation/python)
