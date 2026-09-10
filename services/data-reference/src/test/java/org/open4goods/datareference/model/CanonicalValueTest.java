package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.value.BooleanValue;
import org.open4goods.datareference.model.value.CanonicalValue;
import org.open4goods.datareference.model.value.CanonicalValueType;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.model.value.DateValue;
import org.open4goods.datareference.model.value.DecimalValue;
import org.open4goods.datareference.model.value.IntegerValue;
import org.open4goods.datareference.model.value.LocalizedTextValue;
import org.open4goods.datareference.model.value.QuantityValue;
import org.open4goods.datareference.model.value.UriValue;

class CanonicalValueTest {

    @Test
    void exposesTheClosedSetOfStructuredValueTypes() {
        List<CanonicalValue> values = List.of(
                new LocalizedTextValue("Largeur", new LanguageTag("fr")),
                new BooleanValue(true),
                new IntegerValue(new BigInteger("9223372036854775808")),
                new DecimalValue(new BigDecimal("0.1000000000000000001")),
                new QuantityValue(new BigDecimal("0.845"), "length", "m"),
                new CodeValue("energy-class", "A"),
                new DateValue(LocalDate.parse("2026-09-09")),
                new UriValue(URI.create("https://example.test/evidence")));

        assertThat(values).extracting(CanonicalValue::type).containsExactly(
                CanonicalValueType.LOCALIZED_TEXT,
                CanonicalValueType.BOOLEAN,
                CanonicalValueType.INTEGER,
                CanonicalValueType.DECIMAL,
                CanonicalValueType.QUANTITY,
                CanonicalValueType.CODE,
                CanonicalValueType.DATE,
                CanonicalValueType.URI);
        assertThat(((QuantityValue) values.get(4)).value()).isEqualByComparingTo("0.845");
        assertThat(((QuantityValue) values.get(4)).dimension()).isEqualTo("LENGTH");
    }
}
