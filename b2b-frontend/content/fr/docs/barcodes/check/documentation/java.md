---
title: "Démarrage rapide Java"
description: "Validez des codes-barres et récupérez des métadonnées produit via le facet barcode.check de l'API Product Data depuis Java."
tags:
  - java
  - quickstart
  - barcode
  - validation
scope: public
---

# Démarrage rapide Java - Vérification de code-barres

Ce guide montre comment appeler le point de terminaison de vérification de code-barres depuis Java en utilisant le client HTTP intégré (Java 11+).

## Prérequis

- Java 11 ou supérieur
- Aucune clé API requise pour le point de terminaison public (optionnel pour des limites de débit plus élevées)

## Point de terminaison public (sans authentification)

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
            throw new RuntimeException("Erreur API " + response.statusCode() + ": " + response.body());
        }
    }

    public static void main(String[] args) throws Exception {
        BarcodeCheckClient client = new BarcodeCheckClient();
        String result = client.checkBarcode("3017620422003");
        System.out.println(result);
    }
}
```

## Point de terminaison authentifié (limites de débit plus élevées)

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
            throw new RuntimeException("Erreur API " + response.statusCode() + ": " + response.body());
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

## Analyse de la réponse

Utilisez Jackson pour extraire les champs forensiques et la fiche produit :

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

System.out.println("Valide : " + valid);
System.out.println("Type : " + type);
System.out.println("Pays : " + country);
System.out.println("Classe GS1 : " + gs1Class);

JsonNode product = root.path("product");
if (!product.isMissingNode() && !product.isNull()) {
    System.out.println("Produit : " + product.path("title").asText());
    System.out.println("Meilleur prix : " + product.path("bestPrice").asDouble()
        + " " + product.path("currency").asText());
    System.out.println("Offres : " + product.path("offersCount").asInt());
}
```

## Prochaines étapes

- [Référence barcode.check](/docs/barcodes/check) - schéma complet et référence des classes GS1
- [Démarrage rapide Python](/docs/barcodes/check/documentation/python)
- [Bac à sable interactif](/docs/barcodes/check/playground)
