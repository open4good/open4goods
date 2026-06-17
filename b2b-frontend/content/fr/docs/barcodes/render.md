---
title: "Référence du facet de rendu de code-barres"
description: "Référence complète de l'API POST /api/v1/barcodes/render - paramètres, schéma de réponse, protection des métadonnées et règles de facturation."
tags:
  - barcode
  - render
  - utility
scope: public
---

# Référence de rendu de code-barres

Le facet utilitaire `barcode.render` offre une génération de code-barres de haute qualité et prête pour l'impression. Il prend en charge les symbologies standard 1D et 2D, les URL temporaires sécurisées et l'injection de métadonnées personnalisées.

## Point de terminaison

```http
POST /api/v1/barcodes/render
Authorization: Bearer pdapi_VOTRE_CLE_ICI
Content-Type: application/json
```

## Symbologies supportées

Le service prend en charge les symbologies suivantes :

### Symbologies 1D
- `ean8` (EAN-8)
- `ean13` (EAN-13)
- `upca` (UPC-A)
- `upce` (UPC-E)
- `code128` (Code 128)
- `gs128` (GS1-128)
- `itf14` (ITF-14)

### Symbologies 2D
- `qr` (Code QR)
- `aztec` (Aztec)
- `datamatrix` (Data Matrix)
- `pdf417` (PDF-417)

## Paramètres

| Paramètre | Type | Requis | Description |
|---|---|---|---|
| `type` | string | Oui | Nom de la symbologie (ex: `ean13`, `qr`) |
| `data` | string | Oui | Contenu à encoder |
| `format` | string | Oui | Format du fichier de sortie (`png` ou `svg`) |
| `width` | integer | Oui | Largeur de l'image en pixels |
| `height` | integer | Oui | Hauteur de l'image en pixels |
| `foreground` | string | Non | Couleur de premier plan (format hexadécimal, ex: `#000000`) |
| `background` | string | Non | Couleur d'arrière-plan (format hexadécimal, ex: `#ffffff`) |
| `rotation` | integer | Non | Angle de rotation (`0`, `90`, `180`, `270`) |
| `showText` | boolean | Non | Afficher le texte lisible (symbologies 1D uniquement) |
| `quietZone` | boolean | Non | Inclure des marges de protection (Quiet Zone) |
| `options` | object | Non | Options avancées de mise en forme (voir ci-dessous) |
| `metadata` | object | Non | Injection de métadonnées personnalisées (voir ci-dessous) |

### Objet `options`

| Champ | Type | Requis | Description |
|---|---|---|---|
| `dpi` | integer | Non | Résolution pour le format PNG (par défaut : 300) |
| `moduleWidthMm` | number | Non | Largeur du module en millimètres (1D uniquement) |
| `barHeightMm` | number | Non | Hauteur de la barre en millimètres (1D uniquement) |
| `fontSize` | number | Non | Taille de la police du texte (1D uniquement) |
| `preset` | string | Non | Préréglage de sortie (ex: `print-safe`) |

### Objet `metadata`

Utilisé pour intégrer des informations de propriété intellectuelle directement dans les fichiers d'images générés.

| Champ | Type | Requis | Description |
|---|---|---|---|
| `copyright` | string | Non | Notice de copyright (max 256 caractères) |
| `author` | string | Non | Auteur du document (max 256 caractères) |
| `description` | string | Non | Brève description de la ressource (max 256 caractères) |

## Exemple de requête

```bash
curl -X POST -H "Authorization: Bearer pdapi_VOTRE_CLE_ICI" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ean13",
    "data": "4006381333931",
    "format": "png",
    "width": 200,
    "height": 100,
    "foreground": "#000000",
    "background": "#ffffff",
    "rotation": 0,
    "showText": true,
    "quietZone": true,
    "options": {
      "dpi": 300,
      "moduleWidthMm": 0.33,
      "barHeightMm": 15.0,
      "fontSize": 8.0,
      "preset": "print-safe"
    },
    "metadata": {
      "copyright": "Copyright 2026 open4goods",
      "author": "Open4Goods B2B API",
      "description": "Product GTIN Barcode"
    }
  }' \
  "https://api.product-data-api.com/api/v1/barcodes/render"
```

## Exemple de réponse

```json
{
  "meta": {
    "requestId": "pdreq_01HXYZ",
    "billable": true,
    "creditsConsumed": 1
  },
  "assetUrl": "https://api.product-data-api.com/api/v1/barcodes/assets/eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": "2026-07-16T12:00:00Z",
  "dimensions": {
    "width": 200,
    "height": 100,
    "dpi": 300
  },
  "contentType": "image/png",
  "warnings": [],
  "inputHash": "sha256_a1b2c3d4..."
}
```

## Export de lots ZIP

Pour générer plusieurs codes-barres en une seule fois et les télécharger sous la forme d'une archive ZIP, utilisez le point de terminaison `/render-zip`.

```http
POST /api/v1/barcodes/render-zip
Authorization: Bearer pdapi_VOTRE_CLE_ICI
Content-Type: application/json
Accept: application/zip
```

Envoyez un tableau JSON contenant les requêtes de rendu :
```json
[
  {
    "type": "ean13",
    "data": "4006381333931",
    "format": "png",
    "width": 200,
    "height": 100
  },
  {
    "type": "qr",
    "data": "https://open4goods.org",
    "format": "svg",
    "width": 150,
    "height": 150
  }
]
```

## Protection des métadonnées

Pour éviter toute utilisation abusive ou copie non autorisée, l'API injecte automatiquement des blocs de métadonnées personnalisées directement dans les en-têtes de fichier :
- **Format PNG** : les métadonnées sont écrites dans les blocs standards `tEXt` (Mots-clés : `Copyright`, `Author`, et `Description`).
- **Format SVG** : les métadonnées sont incluses dans une balise `<metadata>` au début du fichier XML.

## Facturation

- Rendu à la demande : **1** crédit débité par code-barres généré avec succès.
- Exportations par lots : facturation de `N` crédits pour `N` codes-barres générés.
- Non facturable : si la validation échoue ou si une erreur serveur survient, aucun crédit n'est consommé.

## Liens rapides

- [Démarrage rapide Java](/docs/barcodes/render/documentation/java)
- [Démarrage rapide Python](/docs/barcodes/render/documentation/python)
- [Bac à sable (Playground) interactif](/docs/barcodes/render/playground)
