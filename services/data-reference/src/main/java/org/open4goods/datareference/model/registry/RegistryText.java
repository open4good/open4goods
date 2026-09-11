package org.open4goods.datareference.model.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.model.LanguageTag;

/** Internal validation helpers shared by authored registry definitions. */
final class RegistryText {

    private RegistryText() {
    }

    static Map<String, String> requireTranslations(Map<String, String> labels, String subject) {
        Objects.requireNonNull(labels, "labels must not be null");
        Map<String, String> copy = new LinkedHashMap<>();
        labels.forEach((language, label) -> {
            String canonicalLanguage = new LanguageTag(language).value();
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException(subject + " has a blank " + canonicalLanguage + " label");
            }
            if (copy.put(canonicalLanguage, label.trim()) != null) {
                throw new IllegalArgumentException(subject + " declares a language twice: " + canonicalLanguage);
            }
        });
        if (!copy.containsKey("en") || !copy.containsKey("fr")) {
            throw new IllegalArgumentException(subject + " must declare English and French labels");
        }
        return Collections.unmodifiableMap(copy);
    }

    static void requireUniqueMappings(List<ExternalMapping> mappings, String subject) {
        for (int left = 0; left < mappings.size(); left++) {
            for (int right = left + 1; right < mappings.size(); right++) {
                ExternalMapping first = mappings.get(left);
                ExternalMapping second = mappings.get(right);
                if (first.hasCoordinate(second) && first.overlaps(second)
                        && first.status() == ExternalMappingStatus.REVIEWED
                        && second.status() == ExternalMappingStatus.REVIEWED) {
                    throw new IllegalArgumentException(subject + " has overlapping reviewed mappings for "
                            + first.system() + ":" + first.externalId());
                }
            }
        }
    }

    static String requirePolicy(String value, String subject) {
        if (value == null || !value.matches("o4g:resolution-policy:[a-z0-9]+(?:-[a-z0-9]+)*@[1-9][0-9]*")) {
            throw new IllegalArgumentException(subject + " must declare a versioned O4G resolution policy");
        }
        return value;
    }
}
