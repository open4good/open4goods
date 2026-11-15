package org.open4goods.model.eprel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.model.util.ProductModelCandidateHelper;

/**
 * Brand table that maps commercial names, regional regulatory numbers and part numbers to the model identifier
 * registered in EPREL.
 *
 * <p>Some manufacturers register a regulatory number that merchants never print: Apple registers {@code A3094}
 * for the "iPhone 15 Plus", while offers carry the commercial name or a part number such as {@code MU163ZD/A}.
 * Public EPREL records have no GTIN, so the EPREL search cannot reach these products without this table.
 *
 * <p>Tables are loaded from {@code classpath*:eprel-model-aliases/{vertical-id}/*.yml}. Resolution is
 * conservative, because a wrong EPREL match gives a product another model's classes:
 * <ul>
 *   <li>the product brand must equal the table brand;</li>
 *   <li>each label (offer title, model, part number) casts at most one vote: an exact regulatory number, a known
 *       part-number base, or the longest commercial name found as a whole phrase;</li>
 *   <li>a name preceded by an accessory marker ("pour", "for", "compatible"...) does not vote;</li>
 *   <li>the winner needs a strict majority of the votes, otherwise nothing is resolved.</li>
 * </ul>
 */
public class EprelModelAliasTable
{
    /** Apple-style part number: five-character base, region suffix, then {@code /A} (e.g. {@code MU163ZD/A}). */
    private static final Pattern PART_NUMBER = Pattern.compile("^([A-Z0-9]{5})[A-Z]{1,2}/A$");

    /** Splits glued tokens such as {@code iphone15plus} or {@code 16e} into words. */
    private static final Pattern LETTER_DIGIT_BOUNDARY = Pattern.compile("(?<=[a-z])(?=[0-9])|(?<=[0-9])(?=[a-z])");

    /** Words that, just before a commercial name, mean the offer is an accessory for that model. */
    private static final Set<String> ACCESSORY_MARKERS = Set.of("pour", "for", "compatible", "para", "fur", "per",
            "avec", "with");

    private String brand;

    private String source;

    private String checkedOn;

    private List<EprelModelAlias> models = new ArrayList<>();

    /**
     * Resolves the EPREL model identifier named by the product labels.
     *
     * @param productBrand brand of the catalogue product
     * @param labels       offer titles, model, alternate models and part numbers of the product
     * @return the EPREL model identifier, or empty when the brand differs, nothing matches or the votes are split
     */
    public Optional<String> resolveEprelModel(String productBrand, Collection<String> labels)
    {
        String normalizedBrand = ProductModelCandidateHelper.normalizePhrase(productBrand);
        if (normalizedBrand == null || labels == null
                || !normalizedBrand.equals(ProductModelCandidateHelper.normalizePhrase(brand)))
        {
            return Optional.empty();
        }
        Map<String, Integer> votes = new LinkedHashMap<>();
        for (String label : labels)
        {
            vote(label).ifPresent(model -> votes.merge(model, 1, Integer::sum));
        }
        int total = votes.values().stream().mapToInt(Integer::intValue).sum();
        return votes.entrySet().stream()
                .filter(entry -> entry.getValue() * 2 > total)
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * Returns the EPREL model named by one label.
     *
     * @param label offer title, model or part number
     * @return EPREL model identifier, or empty when the label names no known model
     */
    Optional<String> vote(String label)
    {
        if (label == null || label.isBlank())
        {
            return Optional.empty();
        }
        String compact = ProductModelCandidateHelper.compactModel(label);
        Matcher partNumber = PART_NUMBER.matcher(label.trim().toUpperCase(Locale.ROOT));
        String mpnBase = partNumber.matches() ? partNumber.group(1) : null;
        for (EprelModelAlias alias : models)
        {
            if (sameCode(compact, alias.eprelModel())
                    || alias.otherModels().stream().anyMatch(other -> sameCode(compact, other))
                    || (mpnBase != null && alias.mpnBases().stream().anyMatch(mpnBase::equalsIgnoreCase)))
            {
                return Optional.ofNullable(alias.eprelModel());
            }
        }

        String phrase = words(label);
        if (phrase == null)
        {
            return Optional.empty();
        }
        EprelModelAlias best = null;
        int bestLength = 0;
        for (EprelModelAlias alias : models)
        {
            for (String name : alias.names())
            {
                String normalizedName = words(name);
                if (normalizedName != null && normalizedName.length() > bestLength
                        && namesModel(phrase, normalizedName))
                {
                    best = alias;
                    bestLength = normalizedName.length();
                }
            }
        }
        return best == null ? Optional.empty() : Optional.ofNullable(best.eprelModel());
    }

    private static boolean sameCode(String compactLabel, String code)
    {
        return compactLabel != null && compactLabel.equals(ProductModelCandidateHelper.compactModel(code));
    }

    /**
     * Checks that the phrase contains the name as whole words, at least once without an accessory marker before.
     */
    private static boolean namesModel(String phrase, String name)
    {
        String paddedPhrase = " " + phrase + " ";
        String paddedName = " " + name + " ";
        int from = 0;
        int index;
        while ((index = paddedPhrase.indexOf(paddedName, from)) >= 0)
        {
            String before = paddedPhrase.substring(0, index).trim();
            String previousWord = before.isEmpty() ? "" : before.substring(before.lastIndexOf(' ') + 1);
            if (!ACCESSORY_MARKERS.contains(previousWord))
            {
                return true;
            }
            from = index + 1;
        }
        return false;
    }

    /**
     * Lower-case ASCII words, with letters and digits split ({@code iPhone15Plus} gives {@code iphone 15 plus}).
     */
    static String words(String value)
    {
        String normalized = ProductModelCandidateHelper.normalizePhrase(value);
        if (normalized == null)
        {
            return null;
        }
        return LETTER_DIGIT_BOUNDARY.matcher(normalized).replaceAll(" ").replaceAll(" +", " ");
    }

    public String getBrand()
    {
        return brand;
    }

    public void setBrand(String brand)
    {
        this.brand = brand;
    }

    /**
     * @return where the table was read (URL of the manufacturer page, catalogue measure)
     */
    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    /**
     * @return date of the last check against the source (ISO date)
     */
    public String getCheckedOn()
    {
        return checkedOn;
    }

    public void setCheckedOn(String checkedOn)
    {
        this.checkedOn = checkedOn;
    }

    public List<EprelModelAlias> getModels()
    {
        return models;
    }

    public void setModels(List<EprelModelAlias> models)
    {
        this.models = models == null ? new ArrayList<>() : models;
    }
}
