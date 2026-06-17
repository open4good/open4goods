---
title: "Vérification de validité de code-barres"
description: "Validez n'importe quel GTIN, EAN-13, UPC-A ou ISBN et obtenez des métadonnées forensiques GS1 ainsi qu'une fiche produit issue de l'index nudger.fr."
tags:
  - barcode
  - gtin
  - validation
  - gratuit
scope: public
---

# Vérification de validité de code-barres

Le facet `barcode.check` valide un code-barres et retourne des métadonnées forensiques GS1, ainsi qu'une fiche produit lorsque le GTIN est présent dans l'index nudger.fr. Ce facet est **gratuit** (0 crédit) et disponible à la fois comme point de terminaison public sans authentification (limité par IP) et comme point de terminaison authentifié par clé API.

## Point de terminaison public (sans authentification)

Utilisez ce point de terminaison pour les vérifications anonymes - aucune clé API requise. Les requêtes sont limitées par adresse IP.

```http
GET /api/v1/barcodes/check?barcode={barcode}
```

### Paramètres

| Paramètre | Type | Requis | Description |
|---|---|---|---|
| `barcode` | string | Oui | Valeur brute du code-barres (GTIN-8, GTIN-12, GTIN-13, GTIN-14, ISBN-10, ISBN-13, ISSN) |

### Exemple de requête

```bash
curl "https://api.product-data-api.com/api/v1/barcodes/check?barcode=3017620422003"
```

## Point de terminaison authentifié

Pour des limites de débit plus élevées et un suivi des accès, transmettez votre clé API.

```http
GET /api/v1/barcodes/{barcode}/check
Authorization: Bearer pdapi_VOTRE_CLE_ICI
```

### Exemple de requête

```bash
curl -H "Authorization: Bearer pdapi_VOTRE_CLE_ICI" \
  "https://api.product-data-api.com/api/v1/barcodes/3017620422003/check"
```

## Schéma de réponse

Les deux points de terminaison retournent la même structure JSON.

```json
{
  "barcode": "3017620422003",
  "forensics": {
    "valid": true,
    "type": "GTIN_13",
    "gs1Prefix": "301",
    "issuingCountryCode": "FR",
    "issuingCountryName": "France",
    "flagUrl": "https://flagcdn.com/fr.svg",
    "gs1Class": "GTIN",
    "gs1ClassLabel": "Article de commerce standard",
    "packagingIndicator": null,
    "isbnRegistrationGroup": null,
    "normalizedGtin14": "03017620422003",
    "normalizedGtin13": "3017620422003",
    "checkDigit": 3
  },
  "product": {
    "gtin": "3017620422003",
    "title": "Nutella - Ferrero - 400g",
    "coverImageUrl": "https://www.nudger.fr/api/image/...",
    "offersCount": 14,
    "bestPrice": 3.49,
    "currency": "EUR",
    "productUrl": "https://www.nudger.fr/fr/product/3017620422003"
  }
}
```

### Champs `forensics`

| Champ | Type | Description |
|---|---|---|
| `valid` | boolean | Si le code-barres passe la validation du chiffre de contrôle |
| `type` | string | Type de code-barres détecté (`GTIN_8`, `GTIN_12`, `GTIN_13`, `GTIN_14`, `ISBN_10`, `ISBN_13`, `ISSN`) |
| `gs1Prefix` | string | 3 premiers chiffres du préfixe GS1 |
| `issuingCountryCode` | string | Code pays ISO 3166-1 alpha-2 de l'organisation GS1 émettrice |
| `issuingCountryName` | string | Nom du pays en clair |
| `flagUrl` | string | URL vers un SVG de drapeau (flagcdn.com) |
| `gs1Class` | string | Classe GS1 du code-barres (`GTIN`, `ISBN_BOOKLAND`, `ISMN_MUSIC`, `ISSN_PERIODICAL`, `RESTRICTED_INTERNAL`, `COUPON`, `UNKNOWN`) |
| `gs1ClassLabel` | string | Libellé lisible de la classe GS1 |
| `packagingIndicator` | integer | Pour les GTIN-14 uniquement : indicateur de niveau d'emballage (premier chiffre, 1-8) |
| `isbnRegistrationGroup` | string | Pour les ISBN-13 uniquement : groupe d'enregistrement (4e chiffre après le préfixe 978/979) |
| `normalizedGtin14` | string | Code-barres complété à gauche pour atteindre 14 chiffres |
| `normalizedGtin13` | string | Code-barres normalisé en 13 chiffres (forme canonique EAN-13, null pour les GTIN-14 avec indicateur d'emballage ≠ 0) |
| `checkDigit` | integer | Le chiffre de contrôle (dernier chiffre) |

### Champ `product`

Le champ `product` est `null` si le GTIN n'est pas présent dans l'index nudger.fr. Quand il est présent :

| Champ | Type | Description |
|---|---|---|
| `gtin` | string | GTIN canonique |
| `title` | string | Nom d'affichage du produit |
| `coverImageUrl` | string | URL de l'image de couverture (peut être null) |
| `offersCount` | integer | Nombre d'offres de prix actives |
| `bestPrice` | number | Meilleur prix disponible dans la devise `currency` |
| `currency` | string | Code de devise ISO 4217 |
| `productUrl` | string | Lien direct vers la page produit sur nudger.fr |

## Facturation

- **Crédits consommés :** 0 (toujours gratuit)
- **Facturable :** jamais
- **Limitation de débit :** par IP pour le point de terminaison public ; par clé pour le point de terminaison authentifié

## Référence des classes GS1

| Classe | Description | Plage de préfixes |
|---|---|---|
| `ISBN_BOOKLAND` | Livres (978-979) | 978, 979 |
| `ISMN_MUSIC` | Partitions musicales (979-0) | 979 + 4e chiffre = 0 |
| `ISSN_PERIODICAL` | Périodiques / magazines | 977 |
| `RESTRICTED_INTERNAL` | Usage interne / en magasin uniquement | 20-29, 40-49, 200-299 |
| `COUPON` | Coupons et bons de réduction | 980-999 |
| `GTIN` | Article de commerce standard | Tous les autres préfixes |

## Liens rapides

- [Démarrage rapide Java](/docs/barcodes/check/documentation/java)
- [Démarrage rapide Python](/docs/barcodes/check/documentation/python)
- [Bac à sable interactif](/docs/barcodes/check/playground)
