package org.open4goods.crawler.services.fetching;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;

import tools.jackson.databind.MappingIterator;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
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
        assertThat(schema.getEscapeChar()).isEqualTo(-1);
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
    void detectsBackslashEscapedQuotesInSemicolonFeed() throws Exception
    {
        CsvSchema schema = detect("""
                product_name;price;description
                Store;19,90;"Rideau avec \\\"motif\\\" et finition"
                Blind;29,90;"Tissu occultant; largeur standard"
                """);

        assertThat((char) schema.getColumnSeparator()).isEqualTo(';');
        assertThat((char) schema.getQuoteChar()).isEqualTo('"');
        assertThat((char) schema.getEscapeChar()).isEqualTo('\\');
    }

    @Test
    void detectsQuotedSemicolonFeedWithMultilineDescription() throws Exception
    {
        Path file = write("""
                id;title;description;price;gtin
                13911734;Rouleau Gerflor;"Découvrez le sol vinyle 1734 Madras Storm de Gerflor.
                Ce revêtement en pose collée, disponible en rouleau de 4m, résiste à l'humidité.";14.50;3475710430077
                6086S071;Plinthe Gerflor;"Fabriquée en Europe, la plinthe S071 contribue à réduire l'empreinte carbone.";6.54;3475710421433
                """);

        CsvSchema schema = detector.detectSchema(file.toFile(), StandardCharsets.UTF_8);

        assertThat((char) schema.getColumnSeparator()).isEqualTo(';');
        assertThat((char) schema.getQuoteChar()).isEqualTo('"');
        try (MappingIterator<Map<String, String>> rows = CsvMapper.builder()
                .enable(CsvReadFeature.IGNORE_TRAILING_UNMAPPABLE)
                .enable(CsvReadFeature.INSERT_NULLS_FOR_MISSING_COLUMNS)
                .build()
                .readerFor(Map.class)
                .with(schema)
                .readValues(new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8)))
        {
            Map<String, String> first = rows.next();
            assertThat(first.get("description")).contains("Ce revêtement en pose collée");
            assertThat(first.get("price")).isEqualTo("14.50");
        }
    }

    @Test
    void fallsBackToUnquotedParsingForMalformedQuotedFeed() throws Exception
    {
        Path file = write("\"title\";\"price\";\"description\"\n"
                + "\"Rideau\";\"45.90\";\"Motifs \"\"\"\"circulaires\"\"\"\",lineaires\"\n"
                + "\"Store\";\"55.34\";\"Tissu premium,\"\"\"\"\"\"\"Fabrique en France\"\"\"\"\"\","
                + "\"\"\"\"\"\"\"gage de qualite\"\"\"\"\"\"\"\";;;;\"\n");

        CsvSchema schema = detector.detectSchema(file.toFile(), StandardCharsets.UTF_8);

        assertThat((char) schema.getColumnSeparator()).isEqualTo(';');
        assertThat(schema.getQuoteChar()).isEqualTo(-1);
        try (MappingIterator<Map<String, String>> rows = CsvMapper.builder()
                .enable(CsvReadFeature.IGNORE_TRAILING_UNMAPPABLE)
                .enable(CsvReadFeature.INSERT_NULLS_FOR_MISSING_COLUMNS)
                .build()
                .readerFor(Map.class)
                .with(schema)
                .readValues(new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8)))
        {
            Map<String, String> first = rows.next();
            assertThat(first.get("title")).isEqualTo("\"Rideau\"");
            assertThat(first.get("price")).isEqualTo("\"45.90\"");
        }
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

    @Test
    void comparableCsvHeaderNormalizesCommonFeedHeaderVariants()
    {
        assertThat(CsvIndexationWorker.comparableCsvHeader("\ufeff\"Sale Price\""))
                .isEqualTo(CsvIndexationWorker.comparableCsvHeader("sale_price"));
        assertThat(CsvIndexationWorker.comparableCsvHeader("Prix TTC"))
                .isEqualTo(CsvIndexationWorker.comparableCsvHeader("prix_ttc"));
        assertThat(CsvIndexationWorker.comparableCsvHeader("Référence produit"))
                .isEqualTo(CsvIndexationWorker.comparableCsvHeader("reference_produit"));
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
