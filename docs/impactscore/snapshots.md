# Instantané mensuel de l'ImpactScore (GOU-1896)

Un document agrégé par verticale et par mois, pour l'historique de la méthode, le baromètre trimestriel et l'open data.

- **Stockage** : index Elasticsearch `impactscore-snapshots` (créé au premier écrit), id `<verticalId>_<yyyy-MM>`. Réécrire le même mois remplace le document : pas de doublon.
- **Écriture** : `BatchService.batch(vertical)`, après le scoring réussi. Le 1er du mois (UTC) l'instantané est (ré)écrit ; les autres jours, seulement s'il manque pour le mois en cours (batch raté le 1er). Un échec d'écriture est journalisé et n'arrête pas le batch.
- **Contenu** (`ImpactScoreSnapshot`) : date, version et révision de méthode, effectifs (cohorte avec offres, notés, non notés par raison, exclus par cause), percentiles p0/10/25/50/75/90/100 de la note sur 20, couverture moyenne et par critère, correspondance EPREL, top 10 des GTIN. Une donnée absente reste absente (`null`, map vide), jamais zéro.
- **Pas de donnée personnelle** : agrégats, plus les GTIN du top.
- **Lecture** : `GET /impactscore/snapshots?verticalId=&from=yyyy-MM&to=yyyy-MM` (front-api), du plus ancien au plus récent.
- **Open data (décision CTO, objectif 2.2)** : licence **Licence Ouverte Etalab 2.0** (licence de référence de l'État, compatible CC-BY, sans partage à l'identique) ; format **JSON**, les documents tels qu'exposés par `GET /impactscore/snapshots`. Le CSV à plat et la publication (data.gouv.fr ou fichier statique) se décident au lot 4. Les GTIN du top ne sont pas des données personnelles.
