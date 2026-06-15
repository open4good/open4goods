---
title: "Analyse de marché DePIN GPU & positionnement d'Infera"
description: "**Version :** 2026-04-26"
tags:
  - documentation
  - vue-content
  - structural
  - frontend
owner: platform
audience: all
language: en
component: frontend
maturity: draft
security_classification: public
doc_url: /docs/apps/frontend/content/fr/docs/infera_depin_market_analysis
doc_path: apps/frontend/content/fr/docs/infera_depin_market_analysis.md
rag_chunking: heading
metadata_last_synced: 2026-05-08
---
# Analyse de marché DePIN GPU & positionnement d'Infera

**Version :** 2026-04-26  
**Document :** analyse stratégique et comparative pour un produit DePIN d'inférence IA  
**Base projet :** `pvc.md`, fourni dans la conversation  
**Angle demandé :** approfondissement de la section **B. Les places de marché GPU décentralisées ou “DePIN”**, avec extension à l'ensemble du paysage concurrentiel et table de positionnement.

---

## 1. Résumé exécutif

Infera ne doit pas être positionné comme une simple marketplace GPU décentralisée. Ce marché existe déjà, il est encombré, et plusieurs acteurs y disposent d'un avantage d'antériorité, de liquidité de compute, de marque crypto ou de capital. La fenêtre stratégique la plus crédible est plus précise : **devenir une couche d'orchestration d'inférence souveraine, OpenAI-compatible, distribuée, pilotée par des politiques de confiance, de résidence et de conformité**.

Le cœur de la proposition n'est donc pas “louer des GPU moins chers”, mais :

1. **remplacer la dépendance à une API centralisée** pour les usages compatibles avec des modèles open-weight ;
2. **rendre le compute hétérogène acceptable pour les entreprises** grâce à une politique d'exécution vérifiable ;
3. **proposer plusieurs niveaux de confiance** : Public Grid, Trusted Grid, HDS/SecNumCloud Grid ;
4. **ajouter une couche d'auditabilité** aujourd'hui peu visible chez les concurrents : localisation, type de nœud, métriques de SLA, empreinte énergétique, non-rétention, logs d'audit et preuves d'exécution ;
5. **masquer complètement la complexité Web3 côté client**.

La meilleure formulation de catégorie est :

> **Infera = Sovereign Distributed Inference Control Plane.**  
> Une API d'inférence compatible OpenAI qui route les requêtes vers des pools GPU distribués selon des politiques de coût, latence, résidence, sécurité, conformité et sobriété.

Le marché montre quatre familles concurrentes :

| Famille | Exemples | Ce qu'elles vendent vraiment | Menace pour Infera | Angle de différenciation Infera |
|---|---|---|---|---|
| APIs IA centralisées / frontier | OpenAI, Anthropic, Google, Mistral | Qualité modèle, simplicité API, écosystème | Très forte sur qualité et DX | Souveraineté, modèles open-weight, routage, coût, contrôle |
| Clouds IA européens | Scaleway, OVHcloud, OUTSCALE, Mistral deployments | API ou infra IA dans un cloud européen centralisé | Très forte sur crédibilité B2B | Distribution multi-pools, résilience, edge/régionalisation, neutralité fournisseur |
| Marketplaces GPU / DePIN compute | Akash, Nosana, io.net, Aethir, Render, Prime Intellect, Gensyn | Compute distribué, GPU moins cher, protocole | Forte sur supply et narratif DePIN | API-first, conformité EU, tiers de confiance, auditabilité, no-Web3 UX |
| APIs open-source managées | Together, Fireworks, Hyperbolic, Hugging Face providers | Accès simple aux modèles open-source | Forte sur simplicité développeur | Souveraineté régionale, policy routing, HDS/SecNumCloud, métrique énergie |

La thèse la plus importante : **le vide n'est pas dans le GPU. Il est dans la confiance opérable.**

---

## 2. Point de départ : hypothèse produit Infera

Le brief décrit Infera comme une plateforme d'intermédiation entre entreprises consommatrices d'inférence IA et fournisseurs de GPU, notamment particuliers/gamers et clouders souverains. Le produit expose une API “drop-in replacement” de l'API OpenAI, tandis que le Router Infera gère les certificats, l'état réseau, la facturation, la distribution des requêtes et la collecte de métriques.

Le design du brief est structuré autour de trois tiers :

| Tier | Description | Usage visé | Lecture marché |
|---|---|---|---|
| **Public Grid** | Docker-compose ou agent simple sur OS standard fournisseur | Données publiques, tests, RP, batch bas risque | Très utile pour créer de la supply, mais non vendable à des clients régulés |
| **Trusted Grid** | OS embarqué/Live USB immuable, accès hôte verrouillé, attestation cible | B2B standard, PME, ETI, corpus internes non critiques | Offre cœur : là où Infera peut créer une catégorie |
| **HDS / SecNumCloud Grid** | Matériel certifié, datacenters certifiés, stack durcie, contrats adaptés | Santé, banque, secteur public, défense | Doit être traité comme une offre curée, pas comme une extension automatique du Public Grid |

Les hypothèses produit de `pvc.md` sont pertinentes : beaucoup d'usages d'entreprise n'exigent pas un modèle frontier propriétaire ; les modèles open-weight quantifiés peuvent couvrir RAG, classification, extraction, enrichissement documentaire, batch, support interne, synthèse, analyse de tickets, génération structurée, copilotes métiers et agents simples. Le produit ne doit cependant pas promettre une substitution universelle à GPT-5/Claude/Gemini sur tous les usages.

---

## 3. Ce qui a changé sur le marché

### 3.1. L'API d'inférence devient interchangeable

Le marché a convergé vers des schémas compatibles OpenAI. Les runtimes open source d'inférence - **vLLM**, **SGLang** et **Text Generation Inference (TGI)** - exposent tous des APIs compatibles OpenAI ou compatibles Chat Completions. Cette standardisation rend crédible l'idée de débrancher la couche d'inférence dans une application existante, à condition de conserver les mêmes primitives : `/chat/completions`, streaming, embeddings, tool calling si possible, batch, clés API et métriques.

Sources : vLLM OpenAI-compatible server ; SGLang OpenAI-compatible APIs ; Hugging Face TGI Messages API.[^vllm][^sglang][^tgi]

### 3.2. La demande européenne se durcit

Le besoin de souveraineté n'est plus seulement narratif. Le **EU AI Act** est entré en vigueur le 1er août 2024 ; les obligations sur les modèles GPAI sont devenues applicables le 2 août 2025 ; l'application générale est prévue au 2 août 2026, avec certaines exceptions.[^eu-ai-act][^eu-gpai]

En France, l'hébergement de données de santé à caractère personnel doit être réalisé par un hébergeur certifié HDS, et la certification HDS est devenue un critère d'achat pour les DSI santé.[^hds] Pour SecNumCloud, l'ANSSI rappelle qu'une offre hébergée sur un cloud qualifié ne devient pas automatiquement qualifiée elle-même ; le périmètre d'architecture, d'exploitation, de gouvernance, de contrat et d'administration compte.[^anssi-secnum]

Conséquence : **Infera doit séparer strictement “routage vers un datacenter certifié” et “service certifié”.** Le HDS/SecNumCloud Grid devra être conçu comme une offre dédiée, auditée, contractualisée, possiblement opérée avec partenaires certifiés, pas comme une simple option de routing.

### 3.3. Les clouds européens attaquent déjà le même besoin

Scaleway propose des Generative APIs compatibles OpenAI, hébergées en Europe, avec facturation au token et positionnement souverain.[^scaleway-genapi] OVHcloud AI Endpoints propose une API serverless pour plus de 40 modèles, indique que les données ne sont ni réutilisées ni conservées, et met en avant des APIs standards populaires comme OpenAI.[^ovh-ai] Mistral propose des déploiements via clouds managés ou localement, avec open-weight models déployables sur infra compatible.[^mistral-deploy]

Ces offres sont des benchmarks importants : elles sont centralisées, mais elles parlent déjà le langage des DSI. Infera doit être **au moins aussi simple** côté développeur et procurement.

### 3.4. Les marketplaces GPU ont évolué vers l'API managée

Le marché DePIN initialement “compute marketplace” glisse vers des produits plus hauts dans la stack :

- AkashML met en avant une inférence IA haute performance, low-latency, basée sur Akash Network, avec compatibilité API drop-in et datacenters globaux.[^akashml]
- io.net Intelligence documente une API compatible OpenAI et une offre Confidential Inference avec attestation hardware, signatures de réponses et vérification de nonce.[^ionet-api][^ionet-conf]
- Hyperbolic combine marketplace GPU, clusters, serverless inference et OpenAI-compatible API.[^hyperbolic]
- Nosana documente des déploiements d'inférence via vLLM/LMDeploy sur réseau décentralisé.[^nosana]

Conclusion : **les concurrents DePIN montent déjà vers le control plane**. Infera doit donc aller directement au niveau API + trust + compliance, pas commencer par une marketplace brute.

---

## 4. B. Les places de marché GPU décentralisées ou “DePIN” - version mise à jour

Ici, on ne remplace pas toujours l'API OpenAI directement ; on remplace souvent le cloud GPU. Le client loue ou consomme du compute dans un réseau distribué, puis déploie vLLM, SGLang, TGI, Ray, Kubernetes, Docker ou une couche managée par-dessus. La maturité varie fortement selon que l'acteur vend :

1. une **API d'inférence prête à consommer** ;
2. une **marketplace de GPU** ;
3. une **infrastructure de clusters** ;
4. un **protocole de vérification / settlement** ;
5. une **couche d'agrégation** non nécessairement blockchain.

### 4.1. Tableau DePIN / GPU marketplace

| Projet | Ce qu'il fait | Maturité pour remplacer une API OpenAI | Lecture stratégique pour Infera |
|---|---|---:|---|
| **Akash / AkashML** | Akash est un marché décentralisé de compute ; AkashML ajoute une couche d'inférence managée, low-latency, API-compatible, avec une promesse de migration “drop-in”. | **Élevée** | C'est le concurrent DePIN le plus proche d'un remplacement API direct. Infera doit le battre sur souveraineté européenne, tiers de confiance, HDS/SecNumCloud et auditabilité énergétique. |
| **io.net / io Intelligence** | Réseau GPU distribué avec API Intelligence compatible OpenAI, modèles open-source, agents et Confidential Inference avec attestation. | **Élevée** | Très fort signal : la vérifiabilité devient un argument produit. Infera doit intégrer attestation, signature de réponses et policy routing dès le Trusted Grid. |
| **Hyperbolic** | Marketplace GPU + clusters + serverless inference + endpoints OpenAI-compatible ; positionnement “open-access AI cloud”. | **Élevée côté DX, moyenne côté DePIN pur** | Très bon benchmark produit/API. Infera ne doit pas l'affronter sur “developer cheap inference” mais sur souveraineté, résidence, conformité et supply européenne. |
| **Nosana** | Réseau de compute décentralisé orienté jobs/deployments IA ; exemples vLLM/LMDeploy ; demande souvent plus d'opérations côté utilisateur. | **Moyenne** | Bon benchmark supply décentralisée. Moins fort comme API B2B standard. Infera doit être plus packagé, plus self-care et plus compatible DSI. |
| **Aethir** | Distributed cloud GPU orienté enterprise-grade, bare metal, IA/gaming, SLA/SLO, GPUs haut de gamme. | **Moyenne à élevée pour compute, faible à moyenne pour API** | Concurrent sérieux pour capacité premium. Infera peut s'en différencier via API-first, routage multi-niveaux et conformité française/européenne. |
| **Render Network** | Réseau GPU historiquement rendu 3D ; Compute Clients pour IA, entraînement, inférence, fine-tuning et generative imaging. | **Faible à moyenne comme API directe** | Plutôt substrat / écosystème que substitut API. Peut devenir fournisseur de capacité indirect, mais pas benchmark principal de l'expérience B2B. |
| **Spheron** | Agrégateur GPU multi-providers avec API/dashboard ; se présente comme aggregated GPU cloud, pas blockchain pure. | **Moyenne à élevée pour compute** | Montre que l'achat réel porte sur l'agrégation, l'API et le dashboard. Infera doit assumer une UX Web2, même si la supply est DePIN. |
| **Prime Intellect** | Plateforme de compute distribué pour entraîner, évaluer et déployer des modèles ; clusters, orchestration, GPU multi-providers. | **Moyenne** | Plus orienté training/RL/cluster que API souveraine. Benchmark pour orchestration de clusters, moins pour conformité inference B2B. |
| **Gensyn** | Protocole de machine intelligence : entraînement, vérification, exécution reproductible, infrastructure décentralisée. | **Faible comme API directe, forte comme inspiration vérifiabilité** | Intéressant pour preuves de calcul, exécution reproductible et modèles économiques décentralisés. Pas un concurrent commercial immédiat d'une API Infera. |
| **Vast.ai** | Marketplace GPU très liquide, API-native, pricing temps réel, per-second billing, nombreux GPU et datacenters. | **Moyenne pour compute, faible comme API IA managée** | Concurrent de prix et liquidité, pas de souveraineté/trust. Infera ne doit pas jouer uniquement le prix. |

### 4.2. Enseignement central de la section B

Le marché DePIN est en train de se segmenter :

| Segment | Description | Acteurs typiques | Risque | Opportunité Infera |
|---|---|---|---|---|
| **API-first DePIN** | Produit consommable comme une API IA | AkashML, io Intelligence, Hyperbolic | Le plus proche d'Infera | Se différencier par souveraineté EU, HDS, SecNumCloud, observabilité énergie |
| **Compute-first marketplace** | Location GPU, clusters, jobs, Docker/K8s/Ray | Nosana, Vast.ai, Prime Intellect, Aethir | Prix et supply | Ajouter une couche API/trust au-dessus ; éventuellement agréger ces sources |
| **Protocol-first** | Vérifiabilité, settlement, exécution reproductible | Gensyn, Render compute clients | Peut devenir infrastructure de fond | Emprunter preuves et mécanismes, sans exposer la complexité au client |
| **Aggregator cloud** | Multi-providers, dashboard, API, pas forcément blockchain | Spheron, Hyperbolic, Hugging Face Providers | UX très simple | Infera doit égaler la simplicité tout en ajoutant politique de confiance |

**Position recommandée :** Infera ne doit pas se présenter comme “un nouveau DePIN GPU”, mais comme **la couche d'inférence souveraine qui rend des pools GPU distribués acceptables pour les entreprises européennes**.

---

## 5. Table comparative de positionnement

### 5.1. Matrice stratégique : Infera contre solutions existantes

Légende :  
**+++** avantage très fort ; **++** bon niveau ; **+** présent mais limité ; **0** faible / absent / non cœur ; **?** non vérifié publiquement.

| Acteur | Catégorie | API OpenAI-compatible | Supply distribuée | Souveraineté EU | Trust / attestation | HDS / SecNumCloud fit | Observabilité énergie par requête | Simplicité développeur | Positionnement vs Infera |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| **OpenAI / Anthropic / Google** | Frontier API centralisée | +++ | 0 | 0/+ | ++ sécurité plateforme, pas DePIN | 0/+ selon offres enterprise/cloud | +/0 | +++ | Gagnent sur qualité modèle et écosystème ; perdent sur souveraineté distribuée et contrôle fin |
| **Mistral AI** | Modèles/API/enterprise européen | ++ | 0 | ++ | ++ enterprise | +/++ selon déploiement | +/0 | ++ | Très fort sur marque européenne et modèles ; Infera doit se positionner comme infra/routing multi-modèles, pas modèle propriétaire |
| **Scaleway Generative APIs / Managed Inference** | Cloud IA européen centralisé | +++ | 0 | +++ | ++ cloud/IAM/VPC | +/++ selon périmètre | ++ observability/énergie côté cloud | +++ | Benchmark direct pour souveraineté simple ; Infera gagne sur distribution, multi-supply, résilience, régionalisation fine |
| **OVHcloud AI Endpoints** | Cloud IA européen centralisé | ++ | 0 | +++ | ++ certifications cloud | ++ potentiel selon offres | +/0 | +++ | Concurrent très crédible pour DSI ; Infera doit démontrer qu'un réseau distribué reste aussi simple et plus résilient |
| **OUTSCALE / Dassault Systèmes** | Cloud souverain qualifié | + selon couches IA | 0 | +++ | +++ SecNumCloud | +++ si périmètre qualifié | +/0 | +/++ | Très fort conformité ; Infera peut l'utiliser comme partenaire HDS/SecNumCloud plutôt que l'affronter frontalement |
| **AkashML** | DePIN API-first | +++ | +++ | +/0 global | +/++ roadmap | 0/+ | +/0 | ++ | Concurrent DePIN le plus proche ; Infera doit dominer sur EU compliance, policy routing, tiers de confiance |
| **io.net Intelligence** | DePIN API + confidential inference | +++ | +++ | +/0 global | +++ confidential inference | 0/+ | +/0 | ++ | Très fort sur attestation ; Infera doit intégrer des preuves similaires mais les packager pour DSI européennes |
| **Hyperbolic** | Open-access AI cloud / GPU marketplace | +++ | ++ | +/0 | ++ privacy/zero retention | 0/+ | +/0 | +++ | Excellent benchmark DX ; Infera doit éviter d'être seulement une API open-source low-cost |
| **Nosana** | DePIN compute/deployments | ++ via déploiements | +++ | 0/+ | +/0 | 0 | 0 | +/++ | Moins packagé pour entreprise ; Infera peut gagner sur productisation API et confiance |
| **Aethir** | Distributed enterprise GPU cloud | + selon offre | +++ | +/0 global | ++ SLA/SLO/bare metal | 0/+ | +/0 | ++ | Concurrent compute premium ; Infera gagne si API-first et conformité européenne réelle |
| **Render Network** | GPU network / compute clients | + via clients | +++ | 0 | +/0 | 0 | 0 | + | Plus substrat que API ; Infera peut s'en inspirer ou agréger indirectement |
| **Spheron** | Aggregated GPU cloud | ++ | ++ | +/0 | + selon providers | + selon providers | +/0 | ++ | Prouve la valeur de l'agrégation ; Infera doit ajouter trust tiers et souveraineté |
| **Prime Intellect** | Compute platform / clusters | + selon déploiement | ++ | +/0 | + | 0/+ | 0 | ++ | Fort training/clusters ; Infera doit rester inference API, pas plateforme training généraliste |
| **Gensyn** | Protocol/verifiability/training | 0/+ | +++ | 0 | +++ conceptuellement | 0 | 0 | + | Inspiration technique, pas concurrent API B2B immédiat |
| **Together AI / Fireworks AI** | API open-source managée | +++ | 0/+/non DePIN | 0/+ | ++ cloud/security | 0/+ | +/0 | +++ | Benchmark performance/API ; Infera doit gagner sur résidence, conformité, multi-pool et sobriété |
| **Hugging Face Inference Providers** | Router multi-providers | ++/+++ | ++ via providers | + selon provider | + selon provider | + selon provider | +/0 | +++ | Benchmark d'agrégation API ; Infera doit être le router de confiance spécialisé EU/DePIN |

### 5.2. Mapping perceptuel

| Axe | Position des concurrents | Position recommandée Infera |
|---|---|---|
| **Qualité modèle frontier** | OpenAI, Anthropic, Google, Mistral dominent | Ne pas concurrencer frontalement ; assumer open-weight et cas d'usage adaptés |
| **Simplicité API** | OpenAI, Mistral, Scaleway, OVHcloud, Together, Hyperbolic dominent | Atteindre le même niveau : `base_url`, clé API, SDK OpenAI, dashboard, logs, usage |
| **Prix brut GPU** | Vast.ai, Akash, Nosana, Hyperbolic très forts | Ne pas baser le moat sur le prix seul ; vendre confiance + routage |
| **Décentralisation réelle** | Akash, io.net, Nosana, Render, Gensyn | Être pragmatique : distribution comme moyen de résilience, pas dogme marketing |
| **Souveraineté européenne** | Scaleway, OVHcloud, OUTSCALE, Mistral forts | Combiner souveraineté + distribution + no-lock-in |
| **Conformité régulée** | OUTSCALE, OVHcloud, certains clouds certifiés | Construire HDS/SecNumCloud Grid comme offre séparée avec partenaires certifiés |
| **Attestation / confidential inference** | io.net très visible ; Akash roadmaps ; clouds confidentiels selon cas | Faire de l'attestation un pilier du Trusted Grid |
| **Observabilité énergie** | Peu différenciée dans les offres DePIN publiques | Opportunité forte : mesure énergétique par requête, qualité de mesure, reporting exportable |

---

## 6. Positionnement recommandé d'Infera

### 6.1. Phrase de positionnement

> **Infera fournit une API d'inférence compatible OpenAI qui exécute les modèles open-weight sur des pools GPU distribués, souverains et auditables, avec des politiques de routage par coût, latence, résidence, niveau de confiance, conformité et efficacité énergétique.**

### 6.2. Ce qu'Infera n'est pas

Infera ne doit pas être :

- une marketplace GPU générique ;
- un clone d'OpenAI ;
- une plateforme crypto exposée aux DSI ;
- un fournisseur de modèles frontier ;
- un simple wrapper vLLM ;
- une promesse HDS/SecNumCloud sans périmètre certifié réel ;
- un réseau de gamers prétendant traiter toutes les données sensibles dès le jour 1.

### 6.3. Ce qu'Infera doit devenir

Infera doit devenir :

- une **API d'inférence souveraine** ;
- un **policy router** multi-pools ;
- une **plateforme de trust scoring des nœuds GPU** ;
- une **couche d'observabilité économique, technique et énergétique** ;
- une **offre B2B self-care** avec contrats, DPA, clés API, quota, reporting et facturation ;
- une **fédération de supply** : gamers pour Public Grid, OS durci pour Trusted Grid, partenaires certifiés pour HDS/SecNumCloud.

---

## 7. Différenciation par tier

### 7.1. Public Grid

**Cible :** développeurs, tests, données publiques, batch bas risque, workloads internes non sensibles, RP, hackathons, open data.

**Promesse :**

- coût bas ;
- élasticité ;
- contribution de GPU particuliers ;
- paiement simple ;
- modèles quantifiés ;
- batch asynchrone ;
- pas de promesse forte de confidentialité.

**Risque :** ce tier peut nuire à la crédibilité enterprise s'il est mélangé au discours B2B sensible. Il doit être présenté comme opportuniste et non comme cœur de conformité.

### 7.2. Trusted Grid

**Cible :** PME, ETI, équipes produit, support interne, RAG documentaire, copilotes métiers, données internes non médicales/hautement critiques.

**Promesse :**

- OS immuable ou agent durci ;
- nœuds vérifiés ;
- mTLS ;
- clés générées localement ;
- scoring de nœuds ;
- attestation lorsque disponible ;
- résidence géographique ;
- absence de stockage prompts/réponses ;
- logs d'audit côté Router ;
- SLA différencié selon nœuds ;
- fallback multi-nœuds.

**C'est l'offre cœur.** Elle doit être vendue comme “acceptable enterprise” sans prétendre remplacer immédiatement HDS/SecNumCloud.

### 7.3. HDS / SecNumCloud Grid

**Cible :** santé, banque, secteur public, défense, opérateurs d'importance vitale, données hautement sensibles.

**Promesse :**

- supply fermée ou très curée ;
- datacenters et opérateurs certifiés ;
- contrats spécifiques ;
- infogérance et administration contrôlées ;
- auditabilité complète ;
- segmentation réseau stricte ;
- conservation maîtrisée des logs ;
- conformité documentaire ;
- périmètre de certification clair.

**Point critique :** HDS/SecNumCloud ne peut pas être un simple label marketing. Il faut décider si Infera est hébergeur certifié, sous-traitant d'un hébergeur certifié, éditeur SaaS sur infrastructure certifiée, ou opérateur d'une offre qualifiée avec périmètre propre.

---

## 8. Wedge go-to-market recommandé

### 8.1. Ne pas commencer par les cas d'usage les plus exigeants

Éviter au départ :

- chat grand public temps réel très haute qualité ;
- agents complexes multi-outils critiques ;
- workloads de santé directement patients ;
- défense classifiée ;
- benchmarks “GPT-5 replacement” ;
- long contexte extrême ;
- workloads nécessitant H100/H200 garantis 24/7 dès le jour 1.

### 8.2. Commencer par les cas où l'inférence distribuée est naturellement adaptée

Prioriser :

| Cas d'usage | Pourquoi c'est adapté | Tier recommandé |
|---|---|---|
| Classification documentaire | Tolère latence, modèles petits efficaces | Trusted / Public |
| Extraction structurée de PDF | Batch, valeur forte, confidentialité importante | Trusted |
| RAG interne PME/ETI | Modèles open-weight suffisants dans beaucoup de cas | Trusted |
| Résumé de tickets / emails / CRM | Volume élevé, latence modérée | Trusted |
| Enrichissement batch de bases de données | Très bon fit économique | Public / Trusted |
| Génération de brouillons métier | Exigence modèle modérée, besoin souveraineté | Trusted |
| Analyse de logs / support IT | Données internes, batch + streaming possible | Trusted |
| Pré-traitement avant modèle frontier | Réduction coût et données envoyées aux hyperscalers | Trusted |
| Santé back-office non critique | Besoin HDS progressif | HDS Grid à terme |

### 8.3. ICP initial

**ICP recommandé :**

- entreprises européennes de 200 à 5 000 employés ;
- secteurs : santé numérique hors données critiques initiales, legaltech, assurance, industrie, services B2B, collectivités, éditeurs SaaS B2B ;
- équipes qui utilisent déjà OpenAI-compatible APIs ;
- besoin de souveraineté/résidence mais budget insuffisant pour infra dédiée ;
- workloads récurrents de documents, extraction, classification, RAG, support.

### 8.4. Message commercial

Ne pas dire :

> “Nous remplaçons OpenAI avec des GPU de gamers.”

Dire :

> “Nous vous donnons une API OpenAI-compatible pour exécuter vos workloads open-weight sur une infrastructure distribuée européenne, avec choix du niveau de confiance, résidence contrôlée, auditabilité et reporting d'impact.”

---

## 9. Business model

### 9.1. Ne pas vendre une seule commodité

Une pure marge sur GPU loué est fragile. Les marketplaces déjà établies peuvent compresser les prix. Infera doit monétiser plusieurs couches :

| Couche de revenu | Description | Défendabilité |
|---|---|---|
| Usage API | Prix au token, requête, batch ou minute GPU | Standard mais commoditisable |
| Premium de tier | Public < Trusted < HDS/SecNumCloud | Défendable par conformité et garanties |
| Capacité réservée | Débit garanti, modèles dédiés, fenêtre 24/7 | Défendable B2B |
| Policy routing | Règles résidence, confiance, énergie, coût | Différenciant |
| Observabilité & reporting | Logs, SLA, coût, énergie, audit exports | Très différenciant si bien produit |
| Services managés | Intégration, RAG, migration, conformité, POC | Nécessaire au début |
| Marketplace supply | Commission côté provider | Utile mais pas suffisant |
| Certification / trust scoring | Badge nœud, audits, niveaux hardware | Potentiel moat supply |

### 9.2. Pricing conceptuel

| Offre | Modèle de prix | Logique |
|---|---|---|
| Public Grid | Prix bas au token / batch ; variable selon disponibilité | Acquisition développeur, preuve économique |
| Trusted Grid | Prix premium au token + SLA + logs + géorestriction | Offre cœur B2B |
| Dedicated Trusted Pool | Abonnement + usage, débit garanti | Clients récurrents |
| HDS Grid | Contrat enterprise, minimum mensuel, audit/compliance inclus | Marché régulé |
| Energy-aware batch | Remise si exécution différée sur zones/périodes favorables | Différenciation sobriété |
| BYOM / modèle dédié | Setup fee + hosting + inférence | Évite commoditisation modèle |

### 9.3. Économie côté provider

Pour attirer les providers GPU, Infera doit leur offrir :

- installation simple ;
- benchmark automatique ;
- estimation revenu ;
- paiement lisible ;
- fenêtres de disponibilité configurables ;
- pas d'exposition des données en clair ;
- scoring transparent ;
- pénalités compréhensibles ;
- compatibilité OS durci pour Trusted Grid ;
- différenciation de revenu selon fiabilité, localisation, GPU, bande passante, sécurité.

Pour éviter une supply de mauvaise qualité, il faut un système de :

- probation ;
- burn-in test ;
- benchmark régulier ;
- anti-fraude ;
- détection de throttling ;
- mesure de disponibilité réelle ;
- réputation ;
- slashing contractuel ou exclusion, même sans token.

---

## 10. Architecture produit recommandée

### 10.1. Control plane

Le Router Infera doit être plus qu'un reverse proxy. Il doit gérer :

- comptes clients ;
- clés API ;
- quotas ;
- modèles disponibles ;
- politiques de routage ;
- inventaire nœuds ;
- état temps réel ;
- certificats ;
- mTLS ;
- health checks ;
- scoring ;
- facturation ;
- logs ;
- métriques énergie ;
- audit trail ;
- fallback ;
- batch queue ;
- rate limiting.

### 10.2. Data plane

Le data plane doit séparer :

- requête client ;
- routage ;
- exécution modèle ;
- streaming de réponse ;
- métriques ;
- traces ;
- données sensibles.

Ne pas stocker prompts/réponses par défaut. Offrir des modes configurables :

| Mode | Stockage prompts/réponses | Usage |
|---|---:|---|
| Zero retention | Non | Défaut B2B |
| Debug temporaire | Opt-in, durée courte | POC/dev |
| Audit metadata only | Métadonnées sans contenu | Enterprise |
| Regulated retention | Selon contrat | HDS/banque/public |

### 10.3. Agent provider

L'agent doit supporter :

- génération locale de clé ;
- CSR vers Router ;
- mTLS ;
- pull model / long polling pour NAT traversal ;
- benchmark GPU ;
- mesure VRAM ;
- monitoring température/power ;
- runtime vLLM/SGLang/TGI ;
- sandboxing ;
- mise à jour signée ;
- attestation quand hardware compatible ;
- séparation modèle/données ;
- kill switch ;
- logs techniques sans contenu client.

### 10.4. Modèles

Ne pas lancer avec trop de modèles. L'offre initiale doit être maîtrisée :

| Type | Exemples de familles | Pourquoi |
|---|---|---|
| 7B-12B instruct | Llama, Mistral, Qwen, Gemma selon licences | Fit GPU consumer, faible coût |
| 14B-32B quantifiés | Qwen, DeepSeek distill, Mistral family | Bon compromis qualité/coût |
| Embeddings | multilingual/e5/bge selon licences | RAG |
| Rerankers | petits modèles spécialisés | Qualité RAG |
| Vision léger | OCR/vision selon GPU | Upsell |
| Batch specialized | classification/extraction | Très bon fit économique |

---

## 11. Moat potentiel

### 11.1. Moat faible : prix GPU

Le prix brut est copiable et compressible. Akash, Vast.ai, Hyperbolic, io.net et autres peuvent mener une guerre de prix. Infera ne doit pas fonder sa thèse uniquement sur “moins cher”.

### 11.2. Moat moyen : réseau de supply

Une bonne supply locale européenne est défendable si elle combine :

- densité géographique ;
- fiabilité ;
- diversité hardware ;
- scoring ;
- onboarding simple ;
- revenus provider prévisibles ;
- contrats avec clouders souverains.

### 11.3. Moat fort : trust policy + compliance

Le moat le plus fort est la capacité à traduire une infrastructure hétérogène en produit achetable par une DSI :

- quelles données peuvent aller où ;
- sur quel type de nœud ;
- avec quel niveau d'attestation ;
- selon quel contrat ;
- avec quels logs ;
- avec quelle preuve de non-rétention ;
- avec quel reporting énergétique ;
- avec quel fallback ;
- avec quelle responsabilité.

### 11.4. Moat fort : observabilité énergie

Une fonctionnalité de reporting énergétique par requête reste crédible et différenciante. Peu d'acteurs DePIN mettent cette couche en avant comme fonctionnalité achetable. Infera peut créer un différenciateur visible :

- kWh estimé par requête ;
- GPU utilisé ;
- durée d'inférence ;
- PUE estimé par pool ;
- niveau de confiance et incertitude ;
- mode batch energy-aware ;
- export CSV/API pour reporting ;
- comparaison “centralized cloud vs distributed/idle capacity” avec méthodologie documentée.

Attention : ne pas surpromettre l'impact écologique. Il faudra une méthodologie transparente, auditée, et distinguer énergie marginale, énergie totale, amortissement hardware, localisation et facteur d'émission.

---

## 12. Risques majeurs

| Risque | Gravité | Pourquoi | Mitigation |
|---|---:|---|---|
| Confidentialité sur GPU particuliers | Très élevée | Les entreprises n'accepteront pas des données sensibles sur machines non contrôlées | Séparer Public/Trusted/HDS ; OS immuable ; attestation ; zero retention ; chiffrement ; workload admissibility |
| Qualité de service hétérogène | Élevée | Latence, panne, réseau, throttling, disponibilité variable | Scoring, health checks, fallback, batch, SLA par tier, réservations |
| Promesse HDS/SecNumCloud prématurée | Très élevée | Risque juridique et réputation | Parler de roadmap ; construire avec partenaires certifiés ; périmètre clair |
| Guerre des prix GPU | Élevée | Marketplaces déjà liquides | Vendre trust/compliance, pas prix seul |
| Qualité modèle inférieure aux frontier | Élevée | Les clients comparent à GPT/Claude/Gemini | Choisir cas d'usage adaptés ; benchmark transparent ; hybrid routing optionnel |
| Complexité provider onboarding | Moyenne/élevée | Trop de friction réduit la supply | Agent simple ; dashboard ; revenu estimé ; auto-tests |
| Sécurité du Router central | Très élevée | Point de contrôle critique | Hardening, audits, red team, segmentation, secrets management |
| Réglementation crypto/Web3 | Moyenne | Peut faire peur aux clients | UX Web2 ; pas de wallet côté client ; token éventuel seulement back-end |
| Anti-fraude supply | Moyenne/élevée | Faux GPU, spoofing, mauvaise perf | Attestation, benchmarks, reputation, monitoring |
| Coût support B2B | Moyenne | Enterprise exige support humain | Self-care + support paid tiers + intégrateurs |

---

## 13. Roadmap recommandée

### Phase 0 - Preuve technique

Objectif : prouver que l'API fonctionne.

- API OpenAI-compatible minimale ;
- router ;
- 2 ou 3 modèles open-weight ;
- agent Docker ;
- quelques nœuds contrôlés ;
- logs d'usage ;
- facturation interne ;
- batch simple ;
- premiers benchmarks latence/coût.

### Phase 1 - Trusted Grid beta

Objectif : prouver le B2B.

- OS immuable ou agent durci ;
- mTLS ;
- scoring nœud ;
- dashboard client ;
- dashboard provider ;
- policy routing par région/tier ;
- zero retention ;
- contrats DPA ;
- métriques par requête ;
- batch ;
- POC avec 3-5 entreprises.

### Phase 2 - Carbon-aware inference

Objectif : différenciation.

- métriques énergie GPU ;
- facteur d'émission régional ;
- reporting exportable ;
- batch différé ;
- comparatifs coût/énergie ;
- documentation méthodologique.

### Phase 3 - Dedicated pools

Objectif : revenu récurrent.

- pools dédiés client ;
- capacité réservée ;
- modèles dédiés ;
- BYOM ;
- SLA ;
- support enterprise.

### Phase 4 - HDS Grid

Objectif : marché régulé.

- partenaires HDS ;
- architecture contractuelle ;
- audit sécurité ;
- périmètre de certification ;
- logs conformes ;
- segmentation ;
- procédures d'incident ;
- contrôle sous-traitants.

### Phase 5 - SecNumCloud / public sector

Objectif : secteur public et critique.

- partenaire qualifié ;
- qualification si stratégique ;
- conformité administration ;
- offre souveraine multi-site ;
- dossier ANSSI si nécessaire.

---

## 14. Recommandations finales

### 14.1. Décision de catégorie

Choisir officiellement :

> **API d'inférence souveraine distribuée**

et éviter :

> “marketplace GPU DePIN”.

Le terme DePIN peut être utile pour investisseurs et providers, mais il ne doit pas dominer le message client B2B.

### 14.2. Produit minimum vendable

Le MVP vendable n'est pas “des gamers branchés au réseau”. C'est :

- une API compatible OpenAI ;
- 3-4 modèles bien choisis ;
- un Router fiable ;
- Trusted Grid avec nœuds contrôlés ;
- zero retention ;
- géorestriction Europe/France ;
- logs et dashboard ;
- batch ;
- premiers indicateurs énergie ;
- contrats B2B simples.

### 14.3. Position contre les concurrents

- Contre **AkashML/io.net/Hyperbolic** : “plus souverain, plus conforme, plus policy-driven”.
- Contre **Scaleway/OVHcloud/Mistral** : “plus distribué, plus résilient, moins lock-in, plus transparent sur l'exécution”.
- Contre **Vast.ai/Nosana/Aethir** : “plus simple, API-first, enterprise-ready”.
- Contre **OpenAI/Anthropic/Google** : “pas frontier, mais contrôlable, souverain, adapté aux workloads open-weight”.
- Contre **Hugging Face/Together/Fireworks** : “moins généraliste, plus souverain et plus auditable”.

### 14.4. Claim central

Le claim recommandé :

> **“L'inférence IA souveraine, distribuée et auditable - compatible OpenAI.”**

Variantes :

- “Run open-weight AI where your compliance allows it.”
- “A policy router for sovereign inference.”
- “The European trust layer for distributed GPU inference.”
- “Inference API with residency, trust tiers and energy-aware routing.”

---

## 15. Annexes : sources principales

[^vllm]: vLLM, OpenAI-Compatible Server - https://docs.vllm.ai/en/latest/serving/openai_compatible_server/  
[^sglang]: SGLang, OpenAI APIs / OpenAI-Compatible APIs - https://docs.sglang.io/basic_usage/openai_api_completions.html  
[^tgi]: Hugging Face Text Generation Inference, Messages API - https://huggingface.co/docs/text-generation-inference/main/messages_api  
[^akashml]: AkashML - https://akashml.com/  
[^ionet-api]: io.net, IO Intelligence APIs - https://io.net/docs/guides/intelligence/io-intelligence-apis  
[^ionet-conf]: io.net, Confidential Inference Overview and Verification Guide - https://io.net/docs/guides/confidential-inference/overview and https://io.net/docs/guides/confidential-inference/verification-guide  
[^hyperbolic]: Hyperbolic Serverless Inference / Open-Access AI Cloud - https://docs.hyperbolic.xyz/docs/serverless-inference and https://www.hyperbolic.ai/  
[^nosana]: Nosana docs - https://docs.nosana.com/  
[^aethir]: Aethir docs, Aethir Introduction / Cloud Host Requirements - https://docs.aethir.com/aethir-introduction and https://docs.aethir.com/aethir-cloud/aethir-cloud-host/operational-requirements-for-cloud-hosts  
[^render]: Render Network Compute Clients - https://rendernetwork.com/participate-compute-clients/  
[^prime]: Prime Intellect - https://www.primeintellect.ai/  
[^gensyn]: Gensyn docs - https://docs.gensyn.ai/  
[^spheron]: Spheron overview - https://docs.spheron.network/overview  
[^vast]: Vast.ai - https://vast.ai/  
[^scaleway-genapi]: Scaleway Generative APIs - https://www.scaleway.com/en/generative-apis/  
[^scaleway-inference]: Scaleway Managed Inference - https://www.scaleway.com/en/inference/  
[^ovh-ai]: OVHcloud AI Endpoints - https://www.ovhcloud.com/en/public-cloud/ai-endpoints  
[^mistral-deploy]: Mistral deployment docs - https://docs.mistral.ai/models/deployment  
[^together]: Together AI OpenAI compatibility and serverless inference - https://docs.together.ai/docs/openai-api-compatibility and https://www.together.ai/serverless-inference  
[^eu-ai-act]: European Commission, AI Act enters into force - https://commission.europa.eu/news/ai-act-enters-force-2024-08-01_en  
[^eu-gpai]: European Commission / Digital Strategy, AI Act timeline and GPAI obligations - https://digital-strategy.ec.europa.eu/en/policies/regulatory-framework-ai and https://digital-strategy.ec.europa.eu/en/news/eu-rules-general-purpose-ai-models-start-apply-bringing-more-transparency-safety-and-accountability  
[^hds]: Agence du Numérique en Santé, HDS - https://esante.gouv.fr/ens/offre/hds  
[^anssi-secnum]: ANSSI, Cloud et qualification SecNumCloud - https://cyber.gouv.fr/enjeux-technologiques/cloud/faq-qualification-secnumcloud/  

---

## 16. Conclusion courte

Infera a une fenêtre de marché réelle, mais elle est étroite. Le produit ne doit pas être vendu comme une promesse crypto ou comme un cloud GPU low-cost. Il doit être vendu comme **la couche de confiance qui rend l'inférence distribuée utilisable par les entreprises européennes**.

Le bon positionnement est :

> **Plus simple qu'un DePIN brut. Plus distribué qu'un cloud souverain centralisé. Plus contrôlable qu'une API frontier.**

C'est là que l'espace concurrentiel est le moins saturé.
