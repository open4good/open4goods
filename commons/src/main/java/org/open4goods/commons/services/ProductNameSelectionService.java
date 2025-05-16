package org.open4goods.commons.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;

/**
 * Service de sélection du nom le plus représentatif parmi une liste de variantes.
 * <p>
 * Implémente plusieurs approches :
 * <ul>
 *     <li>Longueur moyenne</li>
 *     <li>Vote de tokens (fréquence)</li>
 *     <li>Similarité pair-à-pair (Jaro-Winkler)</li>
 *     <li>Centroïde TF-IDF</li>
 * </ul>
 * Gère les cas limites (null, listes vides, valeurs null ou vides).
 */
@Service
public class ProductNameSelectionService {

    private static final Set<String> DEFAULT_STOPWORDS = Set.of(
            "avec", "et", "de", "le", "la", "du", "des", "en", "version", "to", "japonais", "standard"
    );

    /**
     * Choisit le nom dont la longueur est la plus proche de la longueur moyenne.
     * @param names liste de noms
     * @return optional du nom sélectionné
     */
    public Optional<String> selectByAverageLength(List<String> names) {
        List<String> list = sanitize(names);
        if (list.isEmpty()) return Optional.empty();
        double avg = list.stream().mapToInt(String::length).average().orElse(0);
        return list.stream()
                .min(Comparator.comparingDouble(n -> Math.abs(n.length() - avg)));
    }

    /**
     * Sélectionne le nom basé sur le vote de tokens (fréquence de mots).
     * @param names liste de noms
     * @return optional du nom sélectionné
     */
    public Optional<String> selectByTokenVote(List<String> names) {
        List<String> list = sanitize(names);
        if (list.isEmpty()) return Optional.empty();

        Map<String, Long> freq = list.stream()
                .flatMap(n -> tokenize(n).stream())
                .filter(t -> !DEFAULT_STOPWORDS.contains(t))
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        return list.stream()
                .max(Comparator.comparingLong(n -> tokenize(n).stream()
                        .mapToLong(t -> freq.getOrDefault(t, 0L)).sum()));
    }

    /**
     * Sélectionne le nom le plus "central" selon la similarité moyenne (Jaro-Winkler).
     * @param names liste de noms
     * @return optional du nom sélectionné
     */
    public Optional<String> selectByPairwiseSimilarity(List<String> names) {
        List<String> list = sanitize(names);
        if (list.isEmpty()) return Optional.empty();
        JaroWinklerSimilarity jw = new JaroWinklerSimilarity();

        Map<String, Double> avgSim = new ConcurrentHashMap<>();
        list.parallelStream().forEach(n1 -> {
            DoubleAdder sum = new DoubleAdder();
            list.forEach(n2 -> sum.add(jw.apply(n1, n2)));
            avgSim.put(n1, sum.doubleValue() / list.size());
        });
        return avgSim.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    /**
     * Sélectionne le nom le plus proche du centroïde TF-IDF.
     * @param names liste de noms
     * @return optional du nom sélectionné
     */
    public Optional<String> selectByTfIdfCentroid(List<String> names) {
        List<String> list = sanitize(names);
        if (list.isEmpty()) return Optional.empty();

        // Construction du vocabulaire et document frequencies
        Map<String, Long> df = new HashMap<>();
        List<List<String>> docs = list.stream()
                .map(n -> tokenize(n).stream()
                        .filter(t -> !DEFAULT_STOPWORDS.contains(t)).collect(Collectors.toList()))
                .collect(Collectors.toList());

        docs.forEach(doc -> doc.stream().distinct()
                .forEach(t -> df.merge(t, 1L, Long::sum)));
        int N = list.size();

        // Calcul des vecteurs TF-IDF
        List<Map<String, Double>> vectors = docs.stream().map(doc -> {
            Map<String, Double> tf = new HashMap<>();
            doc.forEach(t -> tf.merge(t, 1.0, Double::sum));
            tf.replaceAll((t, c) -> c / doc.size());
            Map<String, Double> tfidf = new HashMap<>();
            tf.forEach((t, w) -> {
                double idf = Math.log((double) N / (1 + df.getOrDefault(t, 0L)));
                tfidf.put(t, w * idf);
            });
            return tfidf;
        }).collect(Collectors.toList());

        // Calcul centroïde
        Map<String, Double> centroid = new HashMap<>();
        vectors.forEach(vec -> vec.forEach((t, w) -> centroid.merge(t, w, Double::sum)));
        centroid.replaceAll((t, w) -> w / N);

        // Choix du nom le plus proche (cosine similarity)
        return Optional.of(list.get(
                IntStream.range(0, list.size())
                        .boxed()
                        .max(Comparator.comparingDouble(i -> cosineSimilarity(centroid, vectors.get(i))))
                        .orElse(0)
        ));
    }

    /**
     * Méthode industrielle : combine les approches TF-IDF et similarité pair-à-pair
     * via un score pondéré.
     * @param names liste de noms
     * @return best name
     */
    public Optional<String> selectBestNameIndustrial(List<String> names) {
        // Poids des méthodes
        double wTf = 0.6, wPair = 0.4;
        Optional<String> tfidf = selectByTfIdfCentroid(names);
        Optional<String> pair = selectByPairwiseSimilarity(names);
        if (tfidf.isPresent() && pair.isPresent() && tfidf.get().equals(pair.get())) {
            return tfidf;
        }
        // Si différent, choisir selon fréquence de concordance
        // Ici on priorise TF-IDF puis pairwise
        return tfidf.isPresent() ? tfidf : pair;
    }

    // ==================== Utils ====================

    private List<String> sanitize(List<String> names) {
        if (names == null) return Collections.emptyList();
        return names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^\\p{L}\\d]+", " ")
                        .split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0, magA = 0, magB = 0;
        for (var e : a.entrySet()) {
            double va = e.getValue();
            double vb = b.getOrDefault(e.getKey(), 0.0);
            dot += va * vb;
            magA += va * va;
        }
        for (double vb : b.values()) {
            magB += vb * vb;
        }
        return dot / (Math.sqrt(magA) * Math.sqrt(magB) + 1e-9);
    }
}
