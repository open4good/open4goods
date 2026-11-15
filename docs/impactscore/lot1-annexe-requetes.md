# Annexe : requêtes de motifs de présence (lot 1a)

Le calcul de C se fait par comptage des motifs de présence des critères, sans lire de document. Pour chaque
combinaison de présence (1) / absence (0) des critères K1..Kn, un agrégat `filter` de l'outil `aggregate` :

```json
{"p101": {"filter": {"bool": {
  "filter":   [{"exists": {"field": "attributes.indexed.K1.value"}}, {"exists": {"field": "attributes.indexed.K3.value"}}],
  "must_not": [{"exists": {"field": "attributes.indexed.K2.value"}}]}}}}
```

avec la requête de la population A :

```json
{"bool":{"filter":[{"term":{"vertical":"<verticale>"}},{"term":{"excluded":true}},{"term":{"excludedCauses":"missing_eprel"}},{"range":{"offersCount":{"gt":0}}}],
 "must_not":[{"term":{"excludedCauses":"missing_brand"}},{"term":{"excludedCauses":"missing_model"}},{"prefix":{"excludedCauses":"missing_attr_"}}]}}
```

(population B : `excluded=false` et `offersCount > 0`). Limite du connecteur : 20 agrégats par appel, donc 4 critères au
plus par appel (16 motifs). Générateur :

```python
import json, itertools, sys
crit = sys.argv[1].split(',')
ex = lambda k: {"exists": {"field": "attributes.indexed.%s.value" % k}}
aggs = {}
for bits in itertools.product([1, 0], repeat=len(crit)):
    f = {"bool": {}}
    must = [ex(c) for c, b in zip(crit, bits) if b]
    mn = [ex(c) for c, b in zip(crit, bits) if not b]
    if must: f["bool"]["filter"] = must
    if mn: f["bool"]["must_not"] = mn
    aggs["p" + "".join(map(str, bits))] = {"filter": f}
print(json.dumps(aggs))
```

Calcul de C : `C = Σ (poids v2 des critères à 1) / Σ poids` (somme des poids v2 = 1, sans `DATA_QUALITY`). Les poids sont
ceux de `verticals/src/main/resources/verticals/impactscores/<verticale>.yml`, bloc `v2.criteriasPonderation`.

Présence marginale par critère : un agrégat `filter` `exists` par critère sous `terms vertical`.
