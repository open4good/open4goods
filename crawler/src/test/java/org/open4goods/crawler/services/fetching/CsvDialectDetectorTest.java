package org.open4goods.crawler.services.fetching;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;

import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Tests for {@link CsvDialectDetector}.
 */
class CsvDialectDetectorTest
{
    @TempDir
    private Path tempDir;

    private final CsvDialectDetector detector = new CsvDialectDetector();

    @Test
    void detectsCommaSeparatedFeed() throws Exception
    {
        CsvSchema schema = detect("""
                product_name,price,description
                Drill,42.50,"Compact, wired drill"
                Saw,19.90,"Small saw"
                """);

        assertThat((char) schema.getColumnSeparator()).isEqualTo(',');
    }

    @Test
    void detectsPipeSeparatedFeedWithCommasInDescriptions() throws Exception
    {
        CsvSchema schema = detect("""
                product_name|price|description
                Oil filter|8.20|Compatible with A, B, and C vehicles
                Brake pads|24.90|Front, ceramic, pair
                """);

        assertThat((char) schema.getColumnSeparator()).isEqualTo('|');
    }

    @Test
    void detectsSemicolonSeparatedFeed() throws Exception
    {
        CsvSchema schema = detect("""
                product_name;price;description
                Perceuse;42,50;Outil compact
                Scie;19,90;Petite scie
                """);

        assertThat((char) schema.getColumnSeparator()).isEqualTo(';');
    }

    @Test
    void detectsTabSeparatedFeed() throws Exception
    {
        CsvSchema schema = detect("product_name\tprice\tdescription\n"
                + "Drill\t42.50\tCompact tool\n"
                + "Saw\t19.90\tSmall saw\n");

        assertThat((char) schema.getColumnSeparator()).isEqualTo('\t');
    }

    @Test
    void ignoresBlankLinesAndStripsBom() throws Exception
    {
        CsvSchema schema = detect("\ufeffproduct_name|price|description\n"
                + "\n"
                + "Drill|42.50|Compact, reliable tool\n"
                + "\n"
                + "Saw|19.90|Small, precise saw\n");

        assertThat((char) schema.getColumnSeparator()).isEqualTo('|');
    }

    @Test
    void rejectsOneColumnCandidatesWhenOtherCandidateIsConsistent() throws Exception
    {
        CsvSchema schema = detect("""
                product_name|price|description
                Single comma, in text|42.50|Compact tool
                Another comma, in text|19.90|Small saw
                """);

        assertThat((char) schema.getColumnSeparator()).isEqualTo('|');
    }

    @Test
    void yamlOverridesTakePrecedenceAfterDetection() throws Exception
    {
        Path file = write("""
                product_name,price,description
                Drill,42.50,Compact tool
                """);
        CsvDataSourceProperties config = new CsvDataSourceProperties();
        config.setCsvSeparator('|');
        config.setCsvQuoteChar('\'');
        config.setCsvEscapeChar('\\');

        CsvSchema schema = CsvIndexationWorker.applyCsvSchemaOverrides(
                detector.detectSchema(file.toFile(), StandardCharsets.UTF_8),
                config);

        assertThat((char) schema.getColumnSeparator()).isEqualTo('|');
        assertThat((char) schema.getQuoteChar()).isEqualTo('\'');
        assertThat((char) schema.getEscapeChar()).isEqualTo('\\');
    }

    private CsvSchema detect(String content) throws Exception
    {
        return detector.detectSchema(write(content).toFile(), StandardCharsets.UTF_8);
    }

    private Path write(String content) throws Exception
    {
        Path file = tempDir.resolve("feed.csv");
        java.nio.file.Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
