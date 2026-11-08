package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Tests for {@link CsvDialectDetector}.
 */
class CsvDialectDetectorTest
{
    @TempDir
    Path tempDir;

    private CsvSchema detect(String content) throws IOException
    {
        File file = tempDir.resolve("feed.csv").toFile();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return new CsvDialectDetector().detectSchema(file, StandardCharsets.UTF_8);
    }

    @Test
    void semicolonFeedWithStrayTabsInValuesKeepsSemicolon() throws IOException
    {
        StringBuilder csv = new StringBuilder("id;nom;description;prix;url\n");
        for (int i = 0; i < 20; i++)
        {
            csv.append(i).append(";Produit ").append(i).append(";Poussette\tlegere\tpliable;")
                    .append(10 + i).append(".90;https://example.org/p/").append(i).append('\n');
        }
        assertThat(detect(csv.toString()).getColumnSeparator()).isEqualTo(';');
    }

    @Test
    void semicolonFeedWithTabInHeaderAndValuesKeepsSemicolon() throws IOException
    {
        StringBuilder csv = new StringBuilder("id;nom;description\t;prix;url\n");
        for (int i = 0; i < 20; i++)
        {
            csv.append(i).append(";Produit ").append(i).append(";Poussette\tlegere;")
                    .append(10 + i).append(".90;https://example.org/p/").append(i).append('\n');
        }
        assertThat(detect(csv.toString()).getColumnSeparator()).isEqualTo(';');
    }

    @Test
    void semicolonFeedWithUnbalancedQuoteAndTabsKeepsSemicolon() throws IOException
    {
        StringBuilder csv = new StringBuilder("id;nom;description;prix;url\n");
        for (int i = 0; i < 20; i++)
        {
            csv.append(i).append(";Produit 5\" ").append(i).append(";Poussette\tlegere;")
                    .append(10 + i).append(".90;https://example.org/p/").append(i).append('\n');
        }
        assertThat(detect(csv.toString()).getColumnSeparator()).isEqualTo(';');
    }

    @Test
    void semicolonFeedWithEntitySemicolonsAndTrailingTabsKeepsSemicolon() throws IOException
    {
        StringBuilder csv = new StringBuilder("id;nom;description;prix;url;marque;categorie;ean;stock;frais\t\n");
        for (int i = 0; i < 30; i++)
        {
            String description = i % 3 == 0 ? "Poussette" : "Poussette&nbsp;legere&amp;pliable&eacute;";
            csv.append(i).append(";Produit ").append(i).append(';').append(description).append(';')
                    .append(10 + i).append(".90;https://example.org/p/").append(i)
                    .append(";Marque;Bebe;123456789012").append(i % 10).append(";1;0\t\n");
        }
        assertThat(detect(csv.toString()).getColumnSeparator()).isEqualTo(';');
    }

    @Test
    void semicolonHeaderWithUnusableDataRowsDoesNotFallBackToTab() throws IOException
    {
        // Data rows with a stray quote in a single-column shape score 0 for every dialect: the header decides.
        StringBuilder csv = new StringBuilder("id;nom;description;prix;url\n");
        for (int i = 0; i < 5; i++)
        {
            csv.append("\"seul champ ").append(i).append(" mal ferme\n");
        }
        assertThat(detect(csv.toString()).getColumnSeparator()).isEqualTo(';');
    }
}
