---
title: "Campagne locale et beta, puis bascule de production"
normative: false
audience: PROJECT_SCOPED
lang: fr
---

# Campagne locale et beta, puis bascule de production

Les choix du 2026-09-11 sont portes par ADR-0013 et le contrat
[Project](../../.o4g/project.yml). Le developpement se fait en local et la recette
directement sur beta.nudger.fr. Les agents disposent des ecritures beta, de ses
deploiements, redemarrages, migrations, parametres GitHub et secrets exclusifs a beta.
La production attend un ordre specifique du proprietaire.

## Sequence

La [roadmap generee](../reference/roadmap.md) donne les dependances, phases,
priorites et blockers courants. Le selecteur est la source de l'ordre executable :

```bash
scripts/work/next-workorder.py
scripts/work/wo.py next --phase ALL --all --limit 100
scripts/work/wo.py status <id>
```

| Etape | Resultat attendu |
|---|---|
| Isolation et configuration beta | Cibles verifiees, secrets beta distincts, runtime et retour arriere, independance du depot de configuration |
| Entrees et faisabilite | Sauvegarde figee, conversion explicite, echantillon borne, ressources et dossiers de revue |
| Contrats et moteurs | Normalisation, mappings, stores, prix, politiques, composition des scores/offres/recherche |
| Adaptateurs et lecteurs | Imports fournisseurs et archive, resolution, groupes, recherche lexicale, API et clients |
| Qualification complete | Benchmark reel, catalogue entier, recette beta et repetition de la bascule |
| Nettoyage et candidat final | SSO, retrait du code historique/XWiki, lots de code mort, nouvelle recette du SHA final |
| Ordre de production | Configuration puis promotion du jeu de donnees et des applications compatibles |
| Apres observation | Retraits physiques et archivage prive apres sept jours sains et verification de restauration |

SSO et les travaux sans dependance de donnees avancent independamment. La priorite
departage les lots techniquement disponibles; une dependance incomplete reste bloquante.
Le benchmark complet ne bloque plus l'implementation des stores sur echantillon,
mais conditionne l'import integral et la qualification de capacite.

## Sauvegarde et fraicheur

`PRODUCT_BACKUP_SOURCE_URI` designe l'ensemble fige sur le volume beta prive. Le
controle du 2026-09-11 a trouve quatre archives gzip JSONL, environ 27,4 Go compresses,
et un manifeste annoncant 51 310 975 produits, termine a 04:20:50 UTC. Ce sont des
observations, pas une cible de comptage pour les executions futures. Les sommes SHA-256
et la lecture gzip complete ont valide les archives avant le reimport.

Le nouvel importeur fige un ensemble coherent avec empreintes et reprise par fichier
et ligne, convertit les donnees dont la provenance est etablie, puis enrichit depuis
les fournisseurs. L'ancien endpoint d'import ecrit dans Product et ne constitue pas
le chemin de migration. Les prix agreges historiques restent des minima legacy,
sans marchand invente; les contenus Amazon ambigus restent hors publication.

La fraicheur acceptee est celle d'un snapshot date, suivi de la reprise des collectes.
Les modifications, suppressions et evenements de prix intervenus entre-temps ne sont
pas garantis exhaustifs. Le dossier de bascule donne la date du snapshot et la borne
de l'enrichissement. Les comptes, soldes, cles API et autres donnees transactionnelles
de production restent distincts des donnees de test beta.

## Points a anticiper

Le volume de sauvegarde etait sur un disque occupe a 90 %. L'inventaire de capacite
mesure aussi l'emplacement reel des index, les replicas, les exports figes et la marge
de 30 %. La reconstruction beta peut liberer d'anciens index precis apres un reimport
borne; les archives d'entree restent conservees. Une depense supplementaire revient au
proprietaire. Une topologie insuffisante ne vaut pas qualification reussie.

Les correspondances Icecat/O4G et regles de famille ambigues font l'objet de dossiers
regroupes pour arbitrage. Les permissions de publication par source ont leur propre
inventaire et revue; une API accessible ne prouve pas le droit de redistribution.
Les credentials externes absents, dont le renouvellement AWIN historiquement reporte,
restent des actions explicites. Les restrictions historiques sur l'administration
beta ont ete remplacees; les anciennes notes conservent leur valeur d'historique.

Chaque critere a une preuve identifiee, un resultat et les versions testees. Les
tests simules ne remplacent pas la recette beta attendue. La livraison finale est un
SHA et un jeu de donnees identifies, avec commandes de reimport, criteres d'arret
et retour arriere. De nouveaux WorkOrders invalident cette preparation jusqu'a leur
validation. La fermeture des lots beta n'autorise aucune ecriture de production.
