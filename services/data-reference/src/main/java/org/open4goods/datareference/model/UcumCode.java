package org.open4goods.datareference.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Unit of measure as a UCUM code.
 *
 * <p>UCUM is case-sensitive and its case carries meaning: {@code mm} is
 * millimetre and {@code Mm} is megametre. The code is therefore never
 * case-folded, and never replaced by a localizable label such as
 * {@code "centimetres"}, which cannot survive a change of display language and
 * cannot be converted.
 *
 * <p>Validation is by composition, not by pattern: a code is a sequence of
 * terms joined by {@code .} and {@code /}, and each term is a known unit atom,
 * optionally carrying a metric prefix and an integer exponent. A pattern alone
 * would accept {@code centimetres}, which is exactly the free text this type
 * exists to keep out of a unit slot.
 *
 * <p>{@link #ATOMS} is deliberately bounded to the units the O4G registry can
 * declare. Adding a unit is a reviewed change here, because a unit the registry
 * declares but no converter knows produces values that cannot be compared.
 *
 * @param value UCUM code such as {@code cm}, {@code kg} or {@code kW.h}
 */
public record UcumCode(@JsonValue String value) {

    /** Longest code accepted, guarding against free text in a unit slot. */
    public static final int MAX_LENGTH = 32;

    /**
     * Unit atoms the O4G registry may declare, in UCUM case-sensitive spelling.
     */
    public static final Set<String> ATOMS = Set.of(
            // SI base and coherent derived units
            "m", "g", "s", "A", "K", "mol", "cd", "rad", "sr",
            "Hz", "N", "Pa", "J", "W", "C", "V", "F", "Ohm", "S", "Wb", "T", "H",
            "lm", "lx", "Bq", "Gy", "Sv", "kat", "Cel",
            // customary units UCUM defines and product data uses
            "L", "l", "min", "h", "d", "wk", "mo", "a", "t", "bar", "eV", "u",
            "[in_i]", "[ft_i]", "[lb_av]", "[oz_av]", "[gal_us]",
            // information units and dimensionless ratios
            "By", "bit", "%", "1");

    /** Metric prefixes, longest first so that {@code da} is tried before {@code d}. */
    private static final List<String> PREFIXES = List.of(
            "da", "Y", "Z", "E", "P", "T", "G", "M", "k", "h",
            "d", "c", "m", "u", "n", "p", "f", "a", "z", "y");

    /** One term: an atom, optionally prefixed, optionally raised to a power. */
    private static final Pattern TERM = Pattern.compile("(?<atom>\\[[A-Za-z0-9_]+\\]|[A-Za-z%]+|1)(?<exponent>[+-]?\\d+)?");

    /**
     * Validates the code without altering its case.
     */
    public UcumCode {
        Objects.requireNonNull(value, "UCUM code must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("UCUM code must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("UCUM code must not exceed " + MAX_LENGTH + " characters");
        }
        for (String term : value.split("[./]", -1)) {
            requireTerm(term, value);
        }
    }

    /**
     * Rebuilds a UCUM code from its serialized form.
     *
     * @param value serialized code
     * @return validated code
     */
    @JsonCreator
    public static UcumCode fromJson(String value) {
        return new UcumCode(value);
    }

    /**
     * Validates one term of a composed code.
     *
     * @param term term to validate
     * @param code full code, for error reporting
     */
    private static void requireTerm(String term, String code) {
        if (term.isEmpty()) {
            throw new IllegalArgumentException("Not a UCUM code, empty term: " + code);
        }
        Matcher matcher = TERM.matcher(term);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a UCUM code: " + code);
        }
        String atom = matcher.group("atom");
        if (ATOMS.contains(atom)) {
            return;
        }
        for (String prefix : PREFIXES) {
            if (atom.length() > prefix.length() && atom.startsWith(prefix)
                    && ATOMS.contains(atom.substring(prefix.length()))) {
                return;
            }
        }
        throw new IllegalArgumentException("Unknown UCUM unit atom '" + atom + "' in code: " + code);
    }

    @Override
    public String toString() {
        return value;
    }
}
