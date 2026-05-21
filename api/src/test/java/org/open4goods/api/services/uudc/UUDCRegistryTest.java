package org.open4goods.api.services.uudc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.services.serialisation.service.SerialisationService;

/**
 * Unit tests for {@link UUDCRegistry}.
 * <p>
 * Loads {@code dimensions.yml} from the test classpath (resolved via the verticals
 * module on the classpath) and verifies round-trip conversions for each dimension.
 */
class UUDCRegistryTest
{
    private UUDCRegistry registry;

    @BeforeEach
    void setUp()
    {
        registry = new UUDCRegistry(new SerialisationService());
        registry.init();
    }

    // -------------------------------------------------------------------------
    // MASS
    // -------------------------------------------------------------------------

    @Test
    void massKgToKg()
    {
        double result = registry.convertToBase(1.0, "kg", "MASS");
        assertThat(result).isEqualTo(1.0);
    }

    @Test
    void massGramsToKg()
    {
        double result = registry.convertToBase(500.0, "g", "MASS");
        assertThat(result).isCloseTo(0.5, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void massPoundsToKg()
    {
        double result = registry.convertToBase(1.0, "lb", "MASS");
        assertThat(result).isCloseTo(0.45359237, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void massSynonymKilogramme()
    {
        UnitDefinition def = registry.resolveUnit("kilogrammes", "MASS");
        assertThat(def).isNotNull();
        assertThat(def.getSymbol()).isEqualTo("kg");
    }

    // -------------------------------------------------------------------------
    // LENGTH
    // -------------------------------------------------------------------------

    @Test
    void lengthCmToM()
    {
        double result = registry.convertToBase(100.0, "cm", "LENGTH");
        assertThat(result).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void lengthMmToM()
    {
        double result = registry.convertToBase(420.0, "mm", "LENGTH");
        assertThat(result).isCloseTo(0.42, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void lengthInchToM()
    {
        double result = registry.convertToBase(1.0, "in", "LENGTH");
        assertThat(result).isCloseTo(0.0254, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void lengthSynonymCentimetres()
    {
        UnitDefinition def = registry.resolveUnit("centimetres", "LENGTH");
        assertThat(def).isNotNull();
        assertThat(def.getSymbol()).isEqualTo("cm");
    }

    // -------------------------------------------------------------------------
    // POWER
    // -------------------------------------------------------------------------

    @Test
    void powerWattsToW()
    {
        double result = registry.convertToBase(1500.0, "W", "POWER");
        assertThat(result).isEqualTo(1500.0);
    }

    @Test
    void powerKilowattsToW()
    {
        double result = registry.convertToBase(1.5, "kW", "POWER");
        assertThat(result).isCloseTo(1500.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    // -------------------------------------------------------------------------
    // SOUND_LEVEL
    // -------------------------------------------------------------------------

    @Test
    void soundLevelDbToDb()
    {
        double result = registry.convertToBase(42.0, "dB", "SOUND_LEVEL");
        assertThat(result).isEqualTo(42.0);
    }

    @Test
    void soundLevelSynonymDba()
    {
        UnitDefinition def = registry.resolveUnit("dba", "SOUND_LEVEL");
        assertThat(def).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    void unknownSymbolReturnsNull()
    {
        UnitDefinition def = registry.resolveUnit("furlong", "LENGTH");
        assertThat(def).isNull();
    }

    @Test
    void unknownDimensionReturnsNull()
    {
        UnitDefinition def = registry.resolveUnit("kg", "FANTASY_DIMENSION");
        assertThat(def).isNull();
    }

    @Test
    void convertUnknownSymbolReturnsValueUnchanged()
    {
        double result = registry.convertToBase(42.0, "unknown_unit", "MASS");
        assertThat(result).isEqualTo(42.0);
    }

    @Test
    void baseUnitForMassIsKg()
    {
        assertThat(registry.getBaseUnit("MASS")).isEqualTo("kg");
    }

    @Test
    void baseUnitForLengthIsM()
    {
        assertThat(registry.getBaseUnit("LENGTH")).isEqualTo("m");
    }
}
