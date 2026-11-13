#!/usr/bin/env python3
"""Report of the ImpactScore V1/V2 simulation (GOU-1887, design docs/impactscore/v2-design.md section 6).

Reads the CSV files written by ImpactScoreSimulationTest (module api) and writes:
- the Markdown tables of the report (one section per vertical) to --markdown;
- the CSV files of the changes to --csv-dir: <vertical>-ecarts.csv (largest rank changes, explained per
  criterion), <vertical>-non-notes.csv (rated in V1, not rated in V2), <vertical>-entrants.csv (products the
  lifted EPREL filter brings in) and fidelite.csv.

Standard library only. Usage:
  python3 scripts/impactscore/simulation_report.py --input <simulation output> \
      --manifest <export>/MANIFEST.md --markdown <file.md> --csv-dir docs/impactscore/simulation
"""

import argparse
import csv
import math
import re
import statistics
from datetime import datetime, timezone
from pathlib import Path

VERTICALS = ["tv", "oven", "smartphones", "dishwasher", "washing-machine", "refrigerator", "air-conditioner"]
NAMES = {
    "tv": "TV",
    "oven": "Fours",
    "smartphones": "Smartphones",
    "dishwasher": "Lave-vaisselle",
    "washing-machine": "Lave-linge",
    "refrigerator": "Réfrigérateurs",
    "air-conditioner": "Climatiseurs",
}
# Criteria of the TV functional unit change (design 2.8)
TV_UNIT = {"POWER_DENSITY_SDR", "POWER_CONSUMPTION_SDR", "POWER_CONSUMPTION_HDR", "DIAGONALE_POUCES"}
# Criteria replaced at gate 2 or by the derived "repairability or durability" criterion (GOU-2041)
REPLACED = {
    "REPAIRABILITY_OR_DURABILITY_INDEX": "réparabilité ou durabilité (GOU-2041)",
    "DURABILITY_INDEX": "réparabilité ou durabilité (GOU-2041)",
    "EU_REPAIRABILITY_CLASS": "classe de réparabilité UE (porte n° 2)",
    "ENERGY_CONSUMPTION_100_CYCLES": "kWh/100 cycles (porte n° 2)",
    "POWER_CONSUMPTION_TYPICAL": "kWh/100 cycles (porte n° 2)",
}
REPLACED_REPAIRABILITY = {
    "smartphones": "classe de réparabilité UE (porte n° 2)",
    "tv": "réparabilité ou durabilité (GOU-2041)",
    "washing-machine": "réparabilité ou durabilité (GOU-2041)",
    "air-conditioner": "réparabilité retirée (porte n° 2)",
}
# Validity of an offer for the sitemap (ProductRepository.VALID_UNTIL_DURATION)
RECENT_OFFER_MS = 2 * 24 * 3600 * 1000
GUARD_SHARE = 0.10
TOP = 20
CSV_ROWS = 200
LABELS = ["excellent", "good", "fair", "poor"]


def num(value):
    return None if value in (None, "") else float(value)


def fmt(value, digits=2):
    if value is None:
        return "-"
    return f"{value:.{digits}f}".replace(".", ",")


def cnum(value, digits=2):
    """A number for a CSV file: dot decimal separator, empty when missing."""
    return "" if value is None else round(value, digits)


def pct(part, whole):
    return "-" if not whole else f"{100.0 * part / whole:.1f} %".replace(".", ",")


def quantile(values, q):
    """Linear interpolation between closest ranks (numpy default)."""
    if not values:
        return None
    ordered = sorted(values)
    position = (len(ordered) - 1) * q
    low = math.floor(position)
    high = math.ceil(position)
    return ordered[low] + (ordered[high] - ordered[low]) * (position - low)


def ranks(values):
    order = sorted(range(len(values)), key=lambda i: values[i])
    result = [0.0] * len(values)
    i = 0
    while i < len(order):
        j = i
        while j + 1 < len(order) and values[order[j + 1]] == values[order[i]]:
            j += 1
        for k in range(i, j + 1):
            result[order[k]] = (i + j) / 2.0 + 1
        i = j + 1
    return result


def spearman(xs, ys):
    if len(xs) < 3:
        return None
    rx, ry = ranks(xs), ranks(ys)
    mx, my = statistics.fmean(rx), statistics.fmean(ry)
    cov = sum((a - mx) * (b - my) for a, b in zip(rx, ry))
    var = math.sqrt(sum((a - mx) ** 2 for a in rx) * sum((b - my) ** 2 for b in ry))
    return cov / var if var else None


def better_than(ranking, count):
    """Share of the rated cohort the product beats (lot 0 formula)."""
    ranking, count = num(ranking), num(count)
    if ranking is None or count is None or count < 2:
        return None
    return ranking / (count - 1)


def load(input_dir, vertical):
    with open(input_dir / f"{vertical}-products.csv", newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    with open(input_dir / f"{vertical}-weights.csv", newline="", encoding="utf-8") as handle:
        weights = list(csv.DictReader(handle))
    return rows, weights


def label(row):
    name = (row["name"] or "").strip()
    brand = (row["brand"] or "").strip()
    model = (row["model"] or "").strip()
    text = name or f"{brand} {model}".strip()
    return text[:70].replace("|", "/")


def contributions(row, weights, vertical=None):
    """Per-criterion contribution to the V1 and V2 ImpactScores (0-20), and the cause of each change."""
    c1, c2, causes = {}, {}, {}
    real_v2 = sum(float(w["v2_weight"]) for w in weights if w["v2_weight"] and num(row.get("v2:" + w["criterion"])) is not None)
    for w in weights:
        key = w["criterion"]
        w1, w2 = num(w["v1_weight"]), num(w["v2_weight"])
        r1, r2 = num(row.get("v1:" + key)), num(row.get("v2:" + key))
        virtual = row.get("v1virtual:" + key) == "true"
        c1[key] = 4 * w1 * r1 if w1 and r1 is not None else 0.0
        c2[key] = 4 * w2 * r2 / real_v2 if w2 and r2 is not None and real_v2 else 0.0
        if key == "DATA_QUALITY":
            causes[key] = "DATA_QUALITY retiré"
        elif key in TV_UNIT:
            causes[key] = "unité TV"
        elif key == "REPAIRABILITY_INDEX" and vertical in REPLACED_REPAIRABILITY and (not w1 or not w2):
            causes[key] = REPLACED_REPAIRABILITY[vertical]
        elif key in REPLACED and (not w1 or not w2):
            causes[key] = REPLACED[key]
        elif not w1:
            causes[key] = "critère ajouté en V2"
        elif not w2:
            causes[key] = "critère retiré en V2"
        elif virtual and r2 is None:
            causes[key] = "redistribution (moyenne imputée retirée)"
        elif r1 is not None and r2 is not None and abs(r2 - r1) > 0.05:
            causes[key] = "bornes robustes" if w["v2_scoring_override"] == "true" else "échelle V2"
        else:
            causes[key] = "redistribution"
    # A V1 criterion no product of the cohort holds gets the configured fallback: share the rest over them
    missing = [w["criterion"] for w in weights if num(w["v1_weight"]) and num(row.get("v1:" + w["criterion"])) is None]
    if missing and row["v1_value"]:
        rest = float(row["v1_value"]) - sum(c1.values())
        total = sum(num(w["v1_weight"]) for w in weights if w["criterion"] in missing)
        for w in weights:
            if w["criterion"] in missing:
                c1[w["criterion"]] = rest * num(w["v1_weight"]) / total
    return c1, c2, causes


def explain(row, weights, cohort_means, vertical):
    """The three criteria that move the product most against its cohort, in points out of 20."""
    c1, c2, causes = contributions(row, weights, vertical)
    effects = []
    for key in c1:
        effect = (c2[key] - c1[key]) - cohort_means.get(key, 0.0)
        effects.append((abs(effect), effect, key))
    effects.sort(reverse=True)
    return "; ".join(f"{key} [{causes[key]}] {effect:+.2f}" for _, effect, key in effects[:3] if abs(effect) >= 0.05)


def guard_threshold(coverages, n_rated):
    """Largest threshold that leaves at most 10 % of the V1 rated products without a V2 score."""
    allowed = math.floor(GUARD_SHARE * n_rated)
    ordered = sorted(coverages)
    if allowed >= len(ordered):
        return None
    return math.floor(ordered[allowed] * 100) / 100


def write_csv(path, header, rows):
    with open(path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle, lineterminator="\n")
        writer.writerow(header)
        writer.writerows(rows)


def section(vertical, rows, weights, reference_ms, csv_dir, out):
    v1 = [r for r in rows if r["v1_value"]]
    v2 = [r for r in rows if r["v2_status"]]
    v2_rated = [r for r in v2 if r["v2_status"] == "RATED"]
    both = [r for r in v1 if r["v2_status"] == "RATED"]
    entrants = [r for r in v2 if not r["v1_value"]]
    leaving_cohort = [r for r in v1 if not r["v2_status"]]
    lost = [r for r in v1 if r["v2_status"] == "NOT_RATED"]
    reasons = {}
    for r in v2:
        if r["v2_status"] == "NOT_RATED":
            reasons[r["v2_reason"]] = reasons.get(r["v2_reason"], 0) + 1
    threshold = num(weights[0]["v2_threshold"]) if weights else None
    eprel = weights[0]["v2_eprel_required"] if weights else "true"

    out.append(f"## {NAMES[vertical]} (`{vertical}`)\n")
    out.append(f"Seuil V2 {fmt(threshold)}, filtre EPREL {'gardé' if eprel == 'true' else 'levé'}.\n")
    out.append("### Effectifs\n")
    out.append("| Notés V1 | Cohorte V2 | Notés V2 | Non noté : couverture faible | Non noté : aucun critère réel | Entrants (filtre EPREL levé) | dont notés | Notés V1 passés en Non noté |")
    out.append("|---:|---:|---:|---:|---:|---:|---:|---:|")
    entrants_rated = sum(1 for r in entrants if r["v2_status"] == "RATED")
    out.append(f"| {len(v1)} | {len(v2)} | {len(v2_rated)} | {reasons.get('LOW_COVERAGE', 0)} | {reasons.get('NO_REAL_CRITERIA', 0)} "
               f"| {len(entrants)} | {entrants_rated} | {len(lost)} ({pct(len(lost), len(v1))}) |\n")
    if leaving_cohort:
        out.append(f"{len(leaving_cohort)} notés V1 sortent de la cohorte V2 (exclusion recalculée).\n")
    guard = len(lost) > GUARD_SHARE * len(v1)
    if guard:
        coverages = [num(r["v2_coverage"]) or 0.0 for r in v1 if r["v2_status"]]
        avoid = guard_threshold(coverages, len(v1))
        out.append(f"**Garde-fou dépassé** : {pct(len(lost), len(v1))} des notés V1 passent en Non noté (plus de 10 %). "
                   f"Seuil qui le ramènerait sous 10 % : {fmt(avoid)} (au lieu de {fmt(threshold)}).\n")

    out.append("### Distribution\n")
    s1 = [float(r["v1_value"]) for r in v1]
    s2 = [float(r["v2_value"]) for r in v2_rated]
    out.append("| Version | p5 | p50 | p95 | p95 - p5 |")
    out.append("|---|---:|---:|---:|---:|")
    for name, values in (("V1", s1), ("V2", s2)):
        p5, p50, p95 = quantile(values, 0.05), quantile(values, 0.5), quantile(values, 0.95)
        spread = None if p5 is None else p95 - p5
        out.append(f"| {name} | {fmt(p5)} | {fmt(p50)} | {fmt(p95)} | {fmt(spread)} |")
    levels = {}
    for r in v2_rated:
        levels[r["v2_level"] or "aucun"] = levels.get(r["v2_level"] or "aucun", 0) + 1
    out.append("")
    out.append("Libellés V2 : " + ", ".join(f"{key} {levels[key]} ({pct(levels[key], len(v2_rated))})"
                                          for key in LABELS + ["aucun"] if key in levels) + ".\n")

    out.append("### Classements\n")
    rho = spearman([float(r["v1_value"]) for r in both], [float(r["v2_value"]) for r in both])
    rho_stored = spearman([float(r["stored_value"]) for r in both if r["stored_value"]],
                          [float(r["v2_value"]) for r in both if r["stored_value"]])
    out.append(f"Spearman V1/V2 sur les {len(both)} produits notés dans les deux versions : **{fmt(rho, 3)}** "
               f"(ECOSCORE stocké contre V2 : {fmt(rho_stored, 3)}).\n")
    top1 = sorted(v1, key=lambda r: (-float(r["v1_value"]), r["id"]))[:TOP]
    top2 = sorted(v2_rated, key=lambda r: (-float(r["v2_value"]), r["id"]))[:TOP]
    common = {r["id"] for r in top1} & {r["id"] for r in top2}
    by_id = {r["id"]: r for r in rows}
    out.append(f"Top {TOP} : {len(common)} produits communs. Place en V2 des produits du top {TOP} V1 :")
    places = []
    for r in top1:
        if r["v2_status"] == "RATED":
            places.append(f"{r['v2_position']}")
        else:
            places.append("Non noté")
    out.append(", ".join(places) + ".\n")
    out.append(f"| # | Top {TOP} V1 | V1 | Top {TOP} V2 | V2 | Place V1 |")
    out.append("|---:|---|---:|---|---:|---:|")
    for i in range(max(len(top1), len(top2))):
        a = top1[i] if i < len(top1) else None
        b = top2[i] if i < len(top2) else None
        place = "-" if b is None or not by_id[b["id"]]["v1_position"] else by_id[b["id"]]["v1_position"]
        out.append(f"| {i + 1} | {label(a) if a else '-'} | {fmt(num(a['v1_value'])) if a else '-'} "
                   f"| {label(b) if b else '-'} | {fmt(num(b['v2_value'])) if b else '-'} | {place} |")
    out.append("")

    # Largest rank changes, explained against the cohort mean of each criterion effect
    deltas = {}
    sums = {}
    for r in both:
        c1, c2, _ = contributions(r, weights)
        for key in c1:
            sums[key] = sums.get(key, 0.0) + (c2[key] - c1[key])
    cohort_means = {key: value / len(both) for key, value in sums.items()} if both else {}
    changes = []
    for r in both:
        b1 = better_than(r["v1_ranking"], r["v1_count"])
        b2 = better_than(r["v2_ranking"], r["v2_count"])
        if b1 is None or b2 is None:
            continue
        deltas[r["id"]] = (b2 - b1) * 100
        changes.append((abs(deltas[r["id"]]), r["id"], r, b1, b2))
    changes.sort(key=lambda item: (-item[0], item[1]))
    moved = [abs(item[2]) for item in [(0, 0, deltas[k]) for k in deltas]]
    if moved:
        out.append(f"Écart de rang (points de centile, |V2 - V1|) : médiane {fmt(statistics.median(moved), 1)}, "
                   f"p90 {fmt(quantile(moved, 0.9), 1)}, plus de 20 points pour {sum(1 for m in moved if m > 20)} produits.\n")
    out.append(f"Les {TOP} plus gros écarts de rang (centile : part de la cohorte notée que le produit dépasse ; "
               "effets en points sur 20, rapportés à l'effet moyen de la cohorte) :\n")
    out.append("| GTIN | Produit | V1 | Centile V1 | V2 | Centile V2 | C | Critères qui expliquent l'écart |")
    out.append("|---|---|---:|---:|---:|---:|---:|---|")
    csv_rows = []
    for _, _, r, b1, b2 in changes[:CSV_ROWS]:
        why = explain(r, weights, cohort_means, vertical)
        csv_rows.append([r["id"], r["brand"], r["model"], label(r), cnum(num(r["v1_value"])), round(b1 * 100, 1),
                         cnum(num(r["v2_value"])), round(b2 * 100, 1), round((b2 - b1) * 100, 1),
                         cnum(num(r["v2_coverage"])), why])
    for line in csv_rows[:TOP]:
        out.append(f"| {line[0]} | {line[3]} | {fmt(line[4])} | {fmt(line[5], 1)} | {fmt(line[6])} | {fmt(line[7], 1)} "
                   f"| {fmt(line[9])} | {line[10].replace('.', ',')} |")
    out.append("")
    write_csv(csv_dir / f"{vertical}-ecarts.csv",
              ["gtin", "marque", "modele", "produit", "v1", "centile_v1", "v2", "centile_v2", "ecart_centile",
               "couverture_c", "criteres"], csv_rows)

    out.append("### Listes et sitemap\n")
    recent = {r["id"] for r in rows if int(r["offers_count"] or 0) > 0 and int(r["last_change"] or 0) > reference_ms - RECENT_OFFER_MS}
    v1_score = {r["id"] for r in rows if r["stored_value"] and r["stored_version"] in ("", "1")}
    sitemap_v1 = {r["id"] for r in rows if r["stored_excluded"] == "false" and (r["id"] in recent or r["id"] in v1_score)}
    sitemap_v2 = {r["id"] for r in rows if r["v2_excluded"] == "false" and (r["id"] in recent or r["v2_status"] == "RATED")}
    inherited = {r["id"] for r in rows if r["stored_excluded"] == "false" and r["id"] not in recent and r["id"] in v1_score}
    leaving = sitemap_v1 - sitemap_v2
    out.append("| Pages catégories : fiches ajoutées | dont notées V2 | Notés V1 qui perdent leur note (restent listés) | Sitemap : fiches ajoutées | Sitemap : fiches qui sortent | dont sans offre récente, ECOSCORE hérité | dont non notées V2, offre ancienne |")
    out.append("|---:|---:|---:|---:|---:|---:|---:|")
    out.append(f"| {len(entrants)} | {entrants_rated} | {len(lost)} | {len(sitemap_v2 - sitemap_v1)} | {len(leaving)} "
               f"| {len(leaving & inherited)} | {len(leaving - inherited)} |\n")
    if lost:
        write_csv(csv_dir / f"{vertical}-non-notes.csv",
                  ["gtin", "marque", "modele", "produit", "v1", "centile_v1", "motif", "couverture_c", "criteres_reels"],
                  [[r["id"], r["brand"], r["model"], label(r), cnum(num(r["v1_value"])),
                    round((better_than(r["v1_ranking"], r["v1_count"]) or 0) * 100, 1), r["v2_reason"],
                    cnum(num(r["v2_coverage"])), f"{r['v2_real']}/{r['v2_total']}"]
                   for r in sorted(lost, key=lambda r: num(r["v2_coverage"]) or 0)])
    if entrants:
        write_csv(csv_dir / f"{vertical}-entrants.csv",
                  ["gtin", "marque", "modele", "produit", "statut_v2", "motif", "couverture_c", "v2", "centile_v2",
                   "libelle", "causes_v1"],
                  [[r["id"], r["brand"], r["model"], label(r), r["v2_status"], r["v2_reason"], cnum(num(r["v2_coverage"])),
                    cnum(num(r["v2_value"])), round((better_than(r["v2_ranking"], r["v2_count"]) or 0) * 100, 1)
                    if r["v2_status"] == "RATED" else "", r["v2_level"], r["stored_causes"]]
                   for r in sorted(entrants, key=lambda r: (r["v2_status"] != "RATED", -(num(r["v2_value"]) or 0)))])
    return {
        "vertical": vertical, "v1": len(v1), "v2": len(v2_rated), "lost": len(lost), "entrants": len(entrants),
        "entrants_rated": entrants_rated, "rho": rho, "inherited": len(leaving & inherited), "guard": guard,
        "s1": (quantile(s1, 0.05), quantile(s1, 0.5), quantile(s1, 0.95)),
        "s2": (quantile(s2, 0.05), quantile(s2, 0.5), quantile(s2, 0.95)), "levels": levels,
        "common": len(common), "rows": rows,
    }


def audit(results, out):
    out.append("## Cas de l'audit\n")
    tv = results.get("tv")
    if tv:
        out.append("### TV 27 pouces, classes E (SDR) et G (HDR)\n")
        out.append("| GTIN | Produit | ECOSCORE stocké | V1 | Centile V1 | V2 | Centile V2 | W/dm² | Statut V2 |")
        out.append("|---|---|---:|---:|---:|---:|---:|---:|---|")
        for r in tv["rows"]:
            diagonal = num(r.get("raw:DIAGONALE_POUCES"))
            if diagonal is None or round(diagonal) != 27 or r.get("raw:CLASSE_ENERGY_SDR") != "E" \
                    or r.get("raw:CLASSE_ENERGY_HDR") != "G" or not r["v1_value"]:
                continue
            sdr = num(r.get("raw:POWER_CONSUMPTION_SDR"))
            density = None if sdr is None else sdr / (diagonal ** 2 * 144 / 337 * 0.064516)
            b1 = better_than(r["v1_ranking"], r["v1_count"])
            b2 = better_than(r["v2_ranking"], r["v2_count"])
            out.append(f"| {r['id']} | {label(r)} | {fmt(num(r['stored_value']))} | {fmt(num(r['v1_value']))} "
                       f"| {fmt(None if b1 is None else b1 * 100, 1)} | {fmt(num(r['v2_value']))} "
                       f"| {fmt(None if b2 is None else b2 * 100, 1)} | {fmt(density)} | {r['v2_status']} |")
        out.append("")
        sized = [r for r in tv["rows"] if r["v1_value"] and r["v2_status"] == "RATED" and num(r.get("raw:DIAGONALE_POUCES"))]
        diagonals = [float(r["raw:DIAGONALE_POUCES"]) for r in sized]
        tops = []
        for version in ("v1", "v2"):
            top = sorted(sized, key=lambda r: -float(r[version + "_value"]))[:len(sized) // 20]
            tops.append(statistics.median(float(r["raw:DIAGONALE_POUCES"]) for r in top))
        out.append(f"Effet taille sur les {len(sized)} TV notées dans les deux versions : Spearman note/diagonale "
                   f"{fmt(spearman([float(r['v1_value']) for r in sized], diagonals))} en V1, "
                   f"{fmt(spearman([float(r['v2_value']) for r in sized], diagonals))} en V2 ; diagonale médiane du top 5 % "
                   f"{fmt(tops[0], 0)} pouces en V1, {fmt(tops[1], 0)} en V2 (cohorte : {fmt(statistics.median(diagonals), 0)}).\n")
    phones = results.get("smartphones")
    if phones:
        out.append("### Smartphones à réparabilité imputée\n")
        imputed = [r for r in phones["rows"] if r["v1_value"] and r.get("v1virtual:REPAIRABILITY_INDEX") == "true"]
        real = [r for r in phones["rows"] if r["v1_value"] and r.get("v1virtual:REPAIRABILITY_INDEX") == "false"]
        rated = [r for r in imputed if r["v2_status"] == "RATED"]
        eu = sum(1 for r in imputed if num(r.get("v2:EU_REPAIRABILITY_CLASS")) is not None)
        out.append(f"{len(imputed)} des {phones['v1']} notés V1 ont un indice de réparabilité imputé (moyenne, poids 0,50), "
                   f"{len(real)} un indice réel. En V2, {eu} des {len(imputed)} ont une classe de réparabilité UE réelle ; "
                   f"{len(rated)} sont notés.\n")
        for name, group in (("imputé", imputed), ("réel", real)):
            both = [r for r in group if r["v2_status"] == "RATED"]
            if both:
                b1 = [better_than(r["v1_ranking"], r["v1_count"]) * 100 for r in both]
                b2 = [better_than(r["v2_ranking"], r["v2_count"]) * 100 for r in both]
                out.append(f"- Indice {name} : V1 médiane {fmt(statistics.median(float(r['v1_value']) for r in both))}, "
                           f"V2 médiane {fmt(statistics.median(float(r['v2_value']) for r in both))} ; "
                           f"centile médian V1 {fmt(statistics.median(b1), 1)}, V2 {fmt(statistics.median(b2), 1)}.")
        out.append("")
    ac = results.get("air-conditioner")
    if ac:
        out.append("### Climatiseurs\n")
        rows = [r for r in ac["rows"] if r["v1_value"]]
        changed = sum(1 for r in rows if r["stored_value"] and abs(float(r["stored_value"]) - float(r["v1_value"])) > 0.05)
        out.append(f"{len(rows)} notés V1, {ac['v2']} notés V2, {ac['lost']} passent en Non noté. "
                   f"{changed} ECOSCORE V1 changent déjà avec l'échelle A+++ à D (GOU-2137, V1 aussi). "
                   "Aucun libellé de position (moins de 100 notés).\n")
        rated = [r for r in rows if r["v2_status"] == "RATED"]
        out.append(f"Spearman note V2/couverture C : {fmt(spearman([float(r['v2_value']) for r in rated], [float(r['v2_coverage']) for r in rated]))} "
                   f"(C médiane {fmt(statistics.median(float(r['v2_coverage']) for r in rated))}).\n")
        out.append("| GTIN | Produit | Classe | ECOSCORE stocké | V1 (main) | V2 | Place V2 | C | Statut V2 |")
        out.append("|---|---|---|---:|---:|---:|---:|---:|---|")
        for r in sorted(rows, key=lambda r: -(num(r["v2_value"]) or -1))[:15]:
            out.append(f"| {r['id']} | {label(r)} | {r.get('raw:CLASSE_ENERGY') or '-'} | {fmt(num(r['stored_value']))} "
                       f"| {fmt(num(r['v1_value']))} | {fmt(num(r['v2_value']))} | {r['v2_position'] or '-'} "
                       f"| {fmt(num(r['v2_coverage']))} | {r['v2_status']} |")
        out.append("")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--markdown", required=True, type=Path)
    parser.add_argument("--csv-dir", required=True, type=Path)
    args = parser.parse_args()

    start = re.search(r"Début : (\S+)", args.manifest.read_text(encoding="utf-8")).group(1)
    reference = datetime.strptime(start, "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=timezone.utc)
    reference_ms = int(reference.timestamp() * 1000)
    args.csv_dir.mkdir(parents=True, exist_ok=True)

    out = []
    with open(args.input / "fidelity.csv", newline="", encoding="utf-8") as handle:
        fidelity = list(csv.DictReader(handle))
    out.append("## Contrôle de fidélité\n")
    out.append("| Verticale | Notés par le dernier batch | Reproduits à 0,05 | Part | Écart max | Cohorte de l'export : comparés | Reproduits | Part |")
    out.append("|---|---:|---:|---:|---:|---:|---:|---:|")
    for f in fidelity:
        out.append(f"| {f['vertical']} | {f['last_batch']} | {f['reproduced']} | {pct(int(f['reproduced']), int(f['last_batch']))} "
                   f"| {fmt(float(f['max_gap']), 4)} | {f['cohort_compared']} | {f['cohort_reproduced']} "
                   f"| {pct(int(f['cohort_reproduced']), int(f['cohort_compared']))} |")
    out.append("")
    write_csv(args.csv_dir / "fidelite.csv", list(fidelity[0].keys()), [list(f.values()) for f in fidelity])

    results = {}
    for vertical in VERTICALS:
        if not (args.input / f"{vertical}-products.csv").exists():
            continue
        rows, weights = load(args.input, vertical)
        results[vertical] = section(vertical, rows, weights, reference_ms, args.csv_dir, out)
    audit(results, out)

    out.append("## Synthèse\n")
    out.append("| Verticale | Notés V1 | Notés V2 | Notés V1 en Non noté | Entrants notés | Spearman | Top 20 communs | p50 V1 | p50 V2 | Sortent du sitemap (ECOSCORE hérité) |")
    out.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    for r in results.values():
        out.append(f"| {NAMES[r['vertical']]} | {r['v1']} | {r['v2']} | {r['lost']} ({pct(r['lost'], r['v1'])}) "
                   f"| {r['entrants_rated']} | {fmt(r['rho'], 3)} | {r['common']} | {fmt(r['s1'][1])} | {fmt(r['s2'][1])} "
                   f"| {r['inherited']} |")
    out.append("")
    args.markdown.write_text("\n".join(out), encoding="utf-8")


if __name__ == "__main__":
    main()
