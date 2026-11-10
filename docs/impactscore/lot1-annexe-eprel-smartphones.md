# Annexe lot 1 : champs d'une fiche EPREL smartphones et tablettes

- Date : 2026-11-10
- Source : index Elasticsearch `eprel-products` (lecture seule), filtre `productGroup = smartphonestablets20231669` (2 815 fiches : smartphones et tablettes).
- Fiches lues : 300 (tri `eprelRegistrationNumber` croissant, `search_after`, pages de 100, soit les 300 plus petits numéros d'enregistrement). Trois fiches de repérage supplémentaires ont été lues au préalable (303 lectures au total, doublons inclus).
- Échantillon biaisé : ce sont les 300 premiers numéros, non un tirage aléatoire. Sur ces 300 fiches : 192 `SMARTPHONE`, 108 `TABLET`.

## Champs de premier niveau

Mapping de `eprel-products` (88 champs). Ceux utiles à l'EPREL smartphones, type ES entre parenthèses :

- `eprelRegistrationNumber` (keyword, triable) : identifiant unique de la fiche.
- `productGroup` (keyword) : `smartphonestablets20231669` ; `implementingAct` (keyword) : `EU_2023_1669` observé.
- `eprelCategory` (keyword) : tableau vide `[]` sur les 3 fiches de repérage, ne distingue pas smartphone et tablette.
- `modelIdentifier` (keyword), `supplierOrTrademark` (keyword), `trademarkId` (long), `organisation.*` (organisationName, etc.).
- `energyClass` (keyword) : A à G, observé sur les 300 fiches (A 97, B 71, C 17, D 22, E 22, F 42, G 29) ; `energyClassImage` (keyword).
- `status` (keyword), `lastVersion` (boolean), `versionNumber` (double), `versionId` (long), `blocked` (boolean), `orgVerificationStatus`, `trademarkVerificationStatus`.
- `formType` (keyword) : `TYPE_1` observé sur les fiches de repérage.
- `gtinIdentifier`, `numericGtin`, `additionalDetails.gtinIdentifier` (keyword), `additionalDetails.attributes` (objet disabled), `constructedEUID`.
- Dates (long, ms) : `firstPublicationDate(Ts)`, `publishedOnDate(Ts)`, `onMarketStartDate(Ts)`, `onMarketFirstStartDate(Ts)`, `onMarketEndDate(Ts)`, `exportDateTs`, `importedOn`.
- `placementCountries.country`, `contactDetails.*`, `uploadedLabels.*`, `registrantNature`, `visibleToUkMsa`, `skipScaleValidation`, `allowEprelLabelGeneration`, `productModelCoreId`, `energyLabelId`.
- `categorySpecificAttributes` (objet disabled, lisible seulement en `_source`) : détaillé ci-dessous.

Champ distinguant smartphone et tablette : `categorySpecificAttributes.deviceType` (`SMARTPHONE` / `TABLET`). Il n'existe pas de champ de premier niveau équivalent.

## categorySpecificAttributes

45 clés distinctes, toutes présentes sur 300/300 fiches (aucune clé optionnelle observée : les valeurs absentes sont `null`). La colonne type liste les types JSON observés (`null` signifie que la clé existe avec la valeur null sur certaines fiches). Les exemples sont des valeurs non nulles distinctes quand elles existent. Les tableaux (pays, `additionalInfos`) ne sont pas aplatis : `additionalInfos` contient des objets `{orderNumber, language, id, text}`.

| clé | type | présence (n/300) | exemple 1 | exemple 2 |
|---|---|---|---|---|
| `additionalInfos` | array | 300 (non null : 300) | `[{"orderNumber": 1, "language": "EN", "id": 1008, "text": "Supplier's Address: 222 W. M...` | `[]` |
| `availabilityInEUEEACountries` | string | 300 (non null : 300) | `"ALL"` | `"AS_SPECIFIED"` |
| `batteryEnduranceInCycles` | integer | 300 (non null : 300) | `12` | `8` |
| `batteryEndurancePerCycle` | integer | 300 (non null : 300) | `2748` | `2910` |
| `batteryEndurancePerCycleInHours` | number/integer | 300 (non null : 300) | `45.8` | `48.5` |
| `batteryUserReplaceable` | boolean | 300 (non null : 300) | `true` | `false` |
| `chargerReceptacleType` | string | 300 (non null : 300) | `"USB_C"` | `"USB_A"` |
| `chargerRequiredOutputPower` | integer | 300 (non null : 300) | `68` | `33` |
| `contactId` | null | 300 (non null : 0) | - | - |
| `deviceType` | string | 300 (non null : 300) | `"SMARTPHONE"` | `"TABLET"` |
| `disassemblyDepthScore` | number/integer | 300 (non null : 300) | `2.15` | `4.05` |
| `energyClassImageWithScale` | string | 300 (non null : 300) | `"B-Left-MediumGreen-WithAGScale.svg"` | `"A-Left-DarkGreen-WithAGScale.svg"` |
| `energyClassRange` | string | 300 (non null : 300) | `"A_G"` | - |
| `exportDateTS` | integer | 300 (non null : 300) | `1777375943714` | `1777375943715` |
| `fallsWithoutDefect` | integer | 300 (non null : 300) | `270` | `6` |
| `fallsWithoutDefectFullyExtended` | null/integer | 300 (non null : 9) | `15` | `25` |
| `fastenersScore` | number/integer | 300 (non null : 300) | `3.1` | `5` |
| `generatedLabels` | null | 300 (non null : 0) | - | - |
| `guaranteeDuration` | integer | 300 (non null : 300) | `24` | `36` |
| `immersionDepthWater` | null/number/integer | 300 (non null : 123) | `1.5` | `1` |
| `ingressProtectionRating` | string | 300 (non null : 300) | `"IP68"` | `"IP54"` |
| `ingressProtectionRatingSolid` | string | 300 (non null : 300) | `"DUST_TIGHT"` | `"DUST_PROTECTED"` |
| `ingressProtectionRatingSolidNumber` | integer | 300 (non null : 300) | `6` | `5` |
| `ingressProtectionRatingWater` | string | 300 (non null : 300) | `"CONT_IMMERSION"` | `"SPLASHING"` |
| `ingressProtectionRatingWaterNumber` | integer | 300 (non null : 300) | `8` | `4` |
| `isFoldable` | boolean | 300 (non null : 300) | `false` | `true` |
| `minYearsSoftwareUpdates` | integer | 300 (non null : 300) | `7` | `5` |
| `modelAvailabilityInCountries` | array | 300 (non null : 300) | `[]` | `["AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "EL", "HU", "IS", "I...` |
| `modelAvailabilityInEUEEACountries` | array | 300 (non null : 300) | `[]` | `["AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "EL", "HU", "IS", "I...` |
| `modelAvailabilityInOtherCountries` | array | 300 (non null : 300) | `[]` | `["XI"]` |
| `operatingSystem` | string | 300 (non null : 300) | `"ANDROID"` | `"IOS"` |
| `ratedBatteryCapacity` | integer | 300 (non null : 300) | `4850` | `4500` |
| `repairInfoScore` | string | 300 (non null : 300) | `"NO_COST"` | `"NO_COST_PROFESSIONAL_REPAIRERS"` |
| `repairabilityClass` | string | 300 (non null : 300) | `"B"` | `"A"` |
| `repairabilityIndex` | number/integer | 300 (non null : 300) | `3.89` | `4.01` |
| `repeatedFreeFallReliabilityClass` | string | 300 (non null : 300) | `"A"` | `"E"` |
| `screenScratchResistance` | string | 300 (non null : 300) | `"MOHS_4"` | `"MOHS_8"` |
| `shippedWithProtectiveCover` | boolean | 300 (non null : 300) | `true` | `false` |
| `softwareUpdatesScore` | string | 300 (non null : 300) | `"MINIMUM_YEARS_MORE_THAN_7"` | `"MINIMUM_YEARS_5"` |
| `sparePartScore` | string | 300 (non null : 300) | `"PRIORITY"` | `"DISPLAY_BATTERY_BACK_COVER"` |
| `toolsScore` | number/integer | 300 (non null : 300) | `4.3` | `4` |
| `trademarkOwner` | null/string | 300 (non null : 84) | `"HMD Global Oy"` | `"Nubia Technology Co., Ltd."` |
| `webLinkInfoSparePartsAvailability` | string | 300 (non null : 300) | `"https://www.replacebase.eu/genuine-motorola-parts"` | `"https://www.hmd.com/en_int/self-repair"` |
| `webLinkPretaxPrices` | string | 300 (non null : 300) | `"https://www.replacebase.eu/genuine-motorola-parts"` | `"https://www.hmdglobal.com"` |
| `webLinkRepairInstructions` | string | 300 (non null : 300) | `"https://www.replacebase.eu/motorola-guide"` | `"https://www.hmd.com/en_int/self-repair"` |

## Repères pour le lot 3

Toutes les clés ci-dessous sont dans `categorySpecificAttributes.`.

- Classe de réparabilité (A-E) : `repairabilityClass` (string ; A 56, B 134, C 106, D 3, E 1).
- Indice de réparabilité (score /10) : `repairabilityIndex` (nombre, observé de 1 à 5 sur cet échantillon ; l'échelle /10 est celle du règlement, non vérifiée par les données). Sous-scores : `disassemblyDepthScore`, `fastenersScore`, `toolsScore` (nombres), `sparePartScore` et `repairInfoScore` (énumérations).
- Durabilité :
  - cycles de batterie : `batteryEnduranceInCycles` (entier, 4 à 20, valeurs fréquentes 8, 10, 12, 16, 20 ; l'unité n'est pas documentée dans les données, probablement en centaines de cycles : à confirmer avant usage) ;
  - endurance par cycle : `batteryEndurancePerCycle` (entier) et `batteryEndurancePerCycleInHours` (nombre) ;
  - tenue aux chutes : `repeatedFreeFallReliabilityClass` (A à E : A 86, B 48, C 40, D 29, E 97), `fallsWithoutDefect` (entier, 0 à 435), `fallsWithoutDefectFullyExtended` (renseigné sur 9 fiches seulement) ;
  - indice de protection IP : `ingressProtectionRating` (ex. `IP68`), `ingressProtectionRatingSolidNumber` et `ingressProtectionRatingWaterNumber` (entiers) ;
  - autres : `screenScratchResistance` (`MOHS_n`), `guaranteeDuration` (mois, 1 à 60), `batteryUserReplaceable`, `shippedWithProtectiveCover`, `isFoldable`.
- Disponibilité des pièces détachées (années, délais de livraison) : absent en valeurs chiffrées. Seul `sparePartScore` (énumération : `PRIORITY`, `DISPLAY`, `DISPLAY_BATTERY`, `DISPLAY_BATTERY_BACK_COVER`, `DISPLAY_BATTERY_BACK_COVER_CAMERAS`) et les liens `webLinkInfoSparePartsAvailability`, `webLinkPretaxPrices`, `webLinkRepairInstructions` existent. Aucune clé d'années de disponibilité ni de délai de livraison.
- Mises à jour système (années) : `minYearsSoftwareUpdates` (entier : 5 pour 219 fiches, 6 pour 31, 7 pour 49, et une valeur aberrante 48) et `softwareUpdatesScore` (énumération `MINIMUM_YEARS_5`, `MINIMUM_YEARS_6`, `MINIMUM_YEARS_MORE_THAN_7`). Système : `operatingSystem` (`ANDROID` 277, `IOS` 19, `OTHER` 4).
- Résistance à l'eau et à la poussière : `ingressProtectionRating`, `ingressProtectionRatingWater` / `ingressProtectionRatingWaterNumber`, `ingressProtectionRatingSolid` / `ingressProtectionRatingSolidNumber`, `immersionDepthWater` (profondeur en m, renseignée sur 123 fiches).
- Capacité de la batterie : `ratedBatteryCapacity` (entier, 2 790 à 11 740, vraisemblablement en mAh).
- Classe d'efficacité énergétique : `energyClass` au premier niveau du document (A à G) ; dans `categorySpecificAttributes`, seulement `energyClassRange` (`A_G`) et `energyClassImageWithScale` (nom du fichier image, ex. `B-Left-MediumGreen-WithAGScale.svg`).
- Type d'appareil : `deviceType` (`SMARTPHONE` 192, `TABLET` 108 ; pas de valeur " téléphone mobile " observée).
