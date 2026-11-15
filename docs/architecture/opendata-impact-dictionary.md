# Dictionnaire des colonnes : datasets impact (GOU-2250)

Un CSV zippé par verticale, `impact-{vertical}.zip`, produit par le passage hebdomadaire de `api` et décrit dans `manifest.json` (lignes, sha256, taux de remplissage par colonne). Licence ODbL-1.0. Les critères sont **bruts** : ni score agrégé (/20), ni sous-score normalisé, ni percentile, ni rang (décision du 15/11, publiés après le lot 1 de l'audit Impact Score). `DATA_QUALITY` n'est jamais publié. Une cellule vide signifie " valeur absente " : jamais de valeur par défaut ni d'imputation.

Ce dictionnaire est dérivé des YAML `verticals/` et `attributes/` ; le manifest reprend l'unité et le libellé de chaque colonne (`unit`, `description`).

## Colonnes communes

| Colonne | Définition |
|---|---|
| `gtin` | Code-barres du produit |
| `brand` | Marque |
| `model` | Modèle |
| `name` | Nom d'offre le plus court |
| `vertical` | Identifiant de la verticale Nudger |
| `nudger_url` | URL canonique de la fiche produit (vide si le produit n'a pas de slug) |
| `eprel_registration_number` | Numéro d'enregistrement EPREL, si présent |
| `offers_count` | Nombre d'offres marchandes |
| `last_updated` | Dernière modification, ISO 8601 UTC |

Pour chaque critère : une colonne de valeur `{critere}` (suffixée de l'unité quand elle existe, ex. `warranty_year`) et une colonne `{critere}_source` (sources qui portent la valeur publiée, séparées par ` | `). La date de la source n'est pas conservée dans l'index : elle n'est pas publiée.

## air-conditioner (`impact-air-conditioner.zip`)

| Colonne | Unité | Définition | Source |
|---|---|---|---|
| `classe_energy` | - | Classe énergétique | Attribut `CLASSE_ENERGY` (EPREL, Icecat, marchands) |
| `classe_energy_source` | - | Origine de la valeur de Classe énergétique | Sources de l'attribut `CLASSE_ENERGY` |
| `power_consumption_typical` | - | Consommation électrique (typique/max) | Attribut `POWER_CONSUMPTION_TYPICAL` (EPREL, Icecat, marchands) |
| `power_consumption_typical_source` | - | Origine de la valeur de Consommation électrique (typique/max) | Sources de l'attribut `POWER_CONSUMPTION_TYPICAL` |
| `power_consumption_off` | - | Consommation électrique (arrêt/veille) | Attribut `POWER_CONSUMPTION_OFF` (EPREL, Icecat, marchands) |
| `power_consumption_off_source` | - | Origine de la valeur de Consommation électrique (arrêt/veille) | Sources de l'attribut `POWER_CONSUMPTION_OFF` |
| `refrigerant_gwp` | - | PRG du refrigerant | Attribut `REFRIGERANT_GWP` (EPREL, Icecat, marchands) |
| `refrigerant_gwp_source` | - | Origine de la valeur de PRG du refrigerant | Sources de l'attribut `REFRIGERANT_GWP` |
| `repairability_index` | - | Indice de réparabilité | Attribut `REPAIRABILITY_INDEX` (EPREL, Icecat, marchands) |
| `repairability_index_source` | - | Origine de la valeur de Indice de réparabilité | Sources de l'attribut `REPAIRABILITY_INDEX` |
| `warranty_year` | Year(s) | Durée de garantie | Attribut `WARRANTY` (EPREL, Icecat, marchands) |
| `warranty_source` | - | Origine de la valeur de Durée de garantie | Sources de l'attribut `WARRANTY` |
| `weight_kg` | kg | Poids | Attribut `WEIGHT` (EPREL, Icecat, marchands) |
| `weight_source` | - | Origine de la valeur de Poids | Sources de l'attribut `WEIGHT` |

## dishwasher (`impact-dishwasher.zip`)

| Colonne | Unité | Définition | Source |
|---|---|---|---|
| `classe_energy` | - | Classe énergétique | Attribut `CLASSE_ENERGY` (EPREL, Icecat, marchands) |
| `classe_energy_source` | - | Origine de la valeur de Classe énergétique | Sources de l'attribut `CLASSE_ENERGY` |
| `warranty_year` | Year(s) | Durée de garantie | Attribut `WARRANTY` (EPREL, Icecat, marchands) |
| `warranty_source` | - | Origine de la valeur de Durée de garantie | Sources de l'attribut `WARRANTY` |
| `repairability_index` | - | Indice de réparabilité | Attribut `REPAIRABILITY_INDEX` (EPREL, Icecat, marchands) |
| `repairability_index_source` | - | Origine de la valeur de Indice de réparabilité | Sources de l'attribut `REPAIRABILITY_INDEX` |
| `minavailabilitysparepartsyears_year` | Year(s) | Disponibilité des pièces détachées | Attribut `MINAVAILABILITYSPAREPARTSYEARS` (EPREL, Icecat, marchands) |
| `minavailabilitysparepartsyears_source` | - | Origine de la valeur de Disponibilité des pièces détachées | Sources de l'attribut `MINAVAILABILITYSPAREPARTSYEARS` |
| `weight_kg` | kg | Poids | Attribut `WEIGHT` (EPREL, Icecat, marchands) |
| `weight_source` | - | Origine de la valeur de Poids | Sources de l'attribut `WEIGHT` |
| `water_consumption_l_cycle` | L/cycle | Consommation d'eau par cycle | Attribut `WATER_CONSUMPTION` (EPREL, Icecat, marchands) |
| `water_consumption_source` | - | Origine de la valeur de Consommation d'eau par cycle | Sources de l'attribut `WATER_CONSUMPTION` |
| `noise_level_db` | dB | Niveau sonore | Attribut `NOISE_LEVEL` (EPREL, Icecat, marchands) |
| `noise_level_source` | - | Origine de la valeur de Niveau sonore | Sources de l'attribut `NOISE_LEVEL` |
| `energy_consumption_100_cycles_kwh_100_cycles` | kWh/100 cycles | Consommation d'énergie pour 100 cycles | Attribut `ENERGY_CONSUMPTION_100_CYCLES` (EPREL, Icecat, marchands) |
| `energy_consumption_100_cycles_source` | - | Origine de la valeur de Consommation d'énergie pour 100 cycles | Sources de l'attribut `ENERGY_CONSUMPTION_100_CYCLES` |
| `power_consumption_standby_w` | W | Consommation en veille | Attribut `POWER_CONSUMPTION_STANDBY` (EPREL, Icecat, marchands) |
| `power_consumption_standby_source` | - | Origine de la valeur de Consommation en veille | Sources de l'attribut `POWER_CONSUMPTION_STANDBY` |
| `power_consumption_off` | - | Consommation électrique (arrêt/veille) | Attribut `POWER_CONSUMPTION_OFF` (EPREL, Icecat, marchands) |
| `power_consumption_off_source` | - | Origine de la valeur de Consommation électrique (arrêt/veille) | Sources de l'attribut `POWER_CONSUMPTION_OFF` |

## oven (`impact-oven.zip`)

| Colonne | Unité | Définition | Source |
|---|---|---|---|
| `classe_energy` | - | Classe énergétique | Attribut `CLASSE_ENERGY` (EPREL, Icecat, marchands) |
| `classe_energy_source` | - | Origine de la valeur de Classe énergétique | Sources de l'attribut `CLASSE_ENERGY` |
| `oven_energy_consumption_cycle_kwh_cycle` | kWh/cycle | Consommation d'énergie par cycle | Attribut `OVEN_ENERGY_CONSUMPTION_CYCLE` (EPREL, Icecat, marchands) |
| `oven_energy_consumption_cycle_source` | - | Origine de la valeur de Consommation d'énergie par cycle | Sources de l'attribut `OVEN_ENERGY_CONSUMPTION_CYCLE` |
| `oven_energy_efficiency_index` | - | Indice d'efficacité énergétique | Attribut `OVEN_ENERGY_EFFICIENCY_INDEX` (EPREL, Icecat, marchands) |
| `oven_energy_efficiency_index_source` | - | Origine de la valeur de Indice d'efficacité énergétique | Sources de l'attribut `OVEN_ENERGY_EFFICIENCY_INDEX` |
| `warranty_year` | Year(s) | Durée de garantie | Attribut `WARRANTY` (EPREL, Icecat, marchands) |
| `warranty_source` | - | Origine de la valeur de Durée de garantie | Sources de l'attribut `WARRANTY` |
| `weight_kg` | kg | Poids | Attribut `WEIGHT` (EPREL, Icecat, marchands) |
| `weight_source` | - | Origine de la valeur de Poids | Sources de l'attribut `WEIGHT` |

## refrigerator (`impact-refrigerator.zip`)

| Colonne | Unité | Définition | Source |
|---|---|---|---|
| `classe_energy` | - | Classe énergétique | Attribut `CLASSE_ENERGY` (EPREL, Icecat, marchands) |
| `classe_energy_source` | - | Origine de la valeur de Classe énergétique | Sources de l'attribut `CLASSE_ENERGY` |
| `noise_level_db` | dB | Niveau sonore | Attribut `NOISE_LEVEL` (EPREL, Icecat, marchands) |
| `noise_level_source` | - | Origine de la valeur de Niveau sonore | Sources de l'attribut `NOISE_LEVEL` |
| `warranty_year` | Year(s) | Durée de garantie | Attribut `WARRANTY` (EPREL, Icecat, marchands) |
| `warranty_source` | - | Origine de la valeur de Durée de garantie | Sources de l'attribut `WARRANTY` |
| `weight_kg` | kg | Poids | Attribut `WEIGHT` (EPREL, Icecat, marchands) |
| `weight_source` | - | Origine de la valeur de Poids | Sources de l'attribut `WEIGHT` |

## smartphones (`impact-smartphones.zip`)

| Colonne | Unité | Définition | Source |
|---|---|---|---|
| `repairability_index` | - | Indice de réparabilité | Attribut `REPAIRABILITY_INDEX` (EPREL, Icecat, marchands) |
| `repairability_index_source` | - | Origine de la valeur de Indice de réparabilité | Sources de l'attribut `REPAIRABILITY_INDEX` |
| `warranty_year` | Year(s) | Durée de garantie | Attribut `WARRANTY` (EPREL, Icecat, marchands) |
| `warranty_source` | - | Origine de la valeur de Durée de garantie | Sources de l'attribut `WARRANTY` |
| `weight_kg` | kg | Poids | Attribut `WEIGHT` (EPREL, Icecat, marchands) |
| `weight_source` | - | Origine de la valeur de Poids | Sources de l'attribut `WEIGHT` |
| `classe_energy` | - | Classe énergétique | Attribut `CLASSE_ENERGY` (EPREL, Icecat, marchands) |
| `classe_energy_source` | - | Origine de la valeur de Classe énergétique | Sources de l'attribut `CLASSE_ENERGY` |

## tv (`impact-tv.zip`)

| Colonne | Unité | Définition | Source |
|---|---|---|---|
| `diagonale_pouces` | " | Diagonale (en pouces) | Attribut `DIAGONALE_POUCES` (EPREL, Icecat, marchands) |
| `diagonale_pouces_source` | - | Origine de la valeur de Diagonale (en pouces) | Sources de l'attribut `DIAGONALE_POUCES` |
| `classe_energy_hdr` | - | Classe énergétique (HDR) | Attribut `CLASSE_ENERGY_HDR` (EPREL, Icecat, marchands) |
| `classe_energy_hdr_source` | - | Origine de la valeur de Classe énergétique (HDR) | Sources de l'attribut `CLASSE_ENERGY_HDR` |
| `classe_energy_sdr` | - | Classe énergétique (SDR) | Attribut `CLASSE_ENERGY_SDR` (EPREL, Icecat, marchands) |
| `classe_energy_sdr_source` | - | Origine de la valeur de Classe énergétique (SDR) | Sources de l'attribut `CLASSE_ENERGY_SDR` |
| `minguaranteedsupportyears_year` | Year(s) | Durée garantie du support | Attribut `MINGUARANTEEDSUPPORTYEARS` (EPREL, Icecat, marchands) |
| `minguaranteedsupportyears_source` | - | Origine de la valeur de Durée garantie du support | Sources de l'attribut `MINGUARANTEEDSUPPORTYEARS` |
| `minavailabilitysparepartsyears_year` | Year(s) | Disponibilité des pièces détachées | Attribut `MINAVAILABILITYSPAREPARTSYEARS` (EPREL, Icecat, marchands) |
| `minavailabilitysparepartsyears_source` | - | Origine de la valeur de Disponibilité des pièces détachées | Sources de l'attribut `MINAVAILABILITYSPAREPARTSYEARS` |
| `minavailabilitysoftwareupdatesyears_year` | Year(s) | Disponibilité des mises à jour logicielles | Attribut `MINAVAILABILITYSOFTWAREUPDATESYEARS` (EPREL, Icecat, marchands) |
| `minavailabilitysoftwareupdatesyears_source` | - | Origine de la valeur de Disponibilité des mises à jour logicielles | Sources de l'attribut `MINAVAILABILITYSOFTWAREUPDATESYEARS` |
| `power_consumption_standby_networkd_w` | W | Consommation électrique (arrêt/veille/réseau) | Attribut `POWER_CONSUMPTION_STANDBY_NETWORKD` (EPREL, Icecat, marchands) |
| `power_consumption_standby_networkd_source` | - | Origine de la valeur de Consommation électrique (arrêt/veille/réseau) | Sources de l'attribut `POWER_CONSUMPTION_STANDBY_NETWORKD` |
| `power_consumption_sdr_w` | W | Consommation électrique (basse définition) | Attribut `POWER_CONSUMPTION_SDR` (EPREL, Icecat, marchands) |
| `power_consumption_sdr_source` | - | Origine de la valeur de Consommation électrique (basse définition) | Sources de l'attribut `POWER_CONSUMPTION_SDR` |
| `power_consumption_hdr_w` | W | Consommation électrique (haute définition) | Attribut `POWER_CONSUMPTION_HDR` (EPREL, Icecat, marchands) |
| `power_consumption_hdr_source` | - | Origine de la valeur de Consommation électrique (haute définition) | Sources de l'attribut `POWER_CONSUMPTION_HDR` |
| `power_consumption_off` | - | Consommation électrique (arrêt/veille) | Attribut `POWER_CONSUMPTION_OFF` (EPREL, Icecat, marchands) |
| `power_consumption_off_source` | - | Origine de la valeur de Consommation électrique (arrêt/veille) | Sources de l'attribut `POWER_CONSUMPTION_OFF` |
| `repairability_index` | - | Indice de réparabilité | Attribut `REPAIRABILITY_INDEX` (EPREL, Icecat, marchands) |
| `repairability_index_source` | - | Origine de la valeur de Indice de réparabilité | Sources de l'attribut `REPAIRABILITY_INDEX` |
| `warranty_year` | Year(s) | Durée de garantie | Attribut `WARRANTY` (EPREL, Icecat, marchands) |
| `warranty_source` | - | Origine de la valeur de Durée de garantie | Sources de l'attribut `WARRANTY` |

## washing-machine (`impact-washing-machine.zip`)

| Colonne | Unité | Définition | Source |
|---|---|---|---|
| `classe_energy` | - | Classe énergétique | Attribut `CLASSE_ENERGY` (EPREL, Icecat, marchands) |
| `classe_energy_source` | - | Origine de la valeur de Classe énergétique | Sources de l'attribut `CLASSE_ENERGY` |
| `power_consumption_off` | - | Consommation électrique (arrêt/veille) | Attribut `POWER_CONSUMPTION_OFF` (EPREL, Icecat, marchands) |
| `power_consumption_off_source` | - | Origine de la valeur de Consommation électrique (arrêt/veille) | Sources de l'attribut `POWER_CONSUMPTION_OFF` |
| `power_consumption_typical` | - | Consommation électrique (typique/max) | Attribut `POWER_CONSUMPTION_TYPICAL` (EPREL, Icecat, marchands) |
| `power_consumption_typical_source` | - | Origine de la valeur de Consommation électrique (typique/max) | Sources de l'attribut `POWER_CONSUMPTION_TYPICAL` |
| `water_consumption_l_cycle` | L/cycle | Consommation d'eau par cycle | Attribut `WATER_CONSUMPTION` (EPREL, Icecat, marchands) |
| `water_consumption_source` | - | Origine de la valeur de Consommation d'eau par cycle | Sources de l'attribut `WATER_CONSUMPTION` |
| `weight_kg` | kg | Poids | Attribut `WEIGHT` (EPREL, Icecat, marchands) |
| `weight_source` | - | Origine de la valeur de Poids | Sources de l'attribut `WEIGHT` |
| `repairability_index` | - | Indice de réparabilité | Attribut `REPAIRABILITY_INDEX` (EPREL, Icecat, marchands) |
| `repairability_index_source` | - | Origine de la valeur de Indice de réparabilité | Sources de l'attribut `REPAIRABILITY_INDEX` |
| `warranty_year` | Year(s) | Durée de garantie | Attribut `WARRANTY` (EPREL, Icecat, marchands) |
| `warranty_source` | - | Origine de la valeur de Durée de garantie | Sources de l'attribut `WARRANTY` |
| `noise_level_db` | dB | Niveau sonore | Attribut `NOISE_LEVEL` (EPREL, Icecat, marchands) |
| `noise_level_source` | - | Origine de la valeur de Niveau sonore | Sources de l'attribut `NOISE_LEVEL` |
