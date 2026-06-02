package org.open4goods.crawler.services.fetching;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Detects CSV dialects from a small file sample.
 *
 * <p>The detector rejects candidates that only produce one column, strips a UTF byte-order mark
 * from the header, ignores blank sample lines, and scores candidates by header/data column-count
 * consistency. Explicit YAML settings are applied by {@link CsvIndexationWorker} after detection
 * and remain authoritative.</p>
 */
public class CsvDialectDetector
{
    /**
     * Candidate column separators in priority order. Comma is last so it wins ties for the
     * common default format without beating a more consistent explicit delimiter.
     */
    private static final char[] SEPARATOR_CANDIDATES = {'\t', '|', ';', ','};

    /** Only {@code "} is a RFC-4180 quote character. */
    private static final char[] QUOTE_CANDIDATES = {'"'};

    /** Number of non-blank lines sampled, including the header. */
    private static final int DETECTION_SAMPLE_LINES = 501;

    /**
     * Detects a CSV schema for the supplied file.
     *
     * @param file file to inspect
     * @param charset charset used to decode the sample
     * @return schema using the detected separator and quote character
     * @throws IOException when the file cannot be read
     */
    public CsvSchema detectSchema(File file, Charset charset) throws IOException
    {
        List<String> sampleLines = readSample(file, charset);
        if (sampleLines.isEmpty())
        {
            return CsvSchema.builder().setUseHeader(true).build();
        }

        char bestSep = ',';
        char bestQuote = '"';
        double bestScore = -1.0;

        for (char sep : SEPARATOR_CANDIDATES)
        {
            for (char quote : QUOTE_CANDIDATES)
            {
                double score = consistencyScore(sampleLines, sep, quote);
                if (score > bestScore)
                {
                    bestScore = score;
                    bestSep = sep;
                    bestQuote = quote;
                }
            }
        }

        return CsvSchema.builder()
                .setColumnSeparator(bestSep)
                .setQuoteChar(bestQuote)
                .setUseHeader(true)
                .build();
    }

    private List<String> readSample(File file, Charset charset) throws IOException
    {
        List<String> sampleLines = new ArrayList<>(DETECTION_SAMPLE_LINES);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset)))
        {
            String line;
            while ((line = br.readLine()) != null && sampleLines.size() < DETECTION_SAMPLE_LINES)
            {
                String normalized = stripBom(line);
                if (!normalized.isBlank())
                {
                    sampleLines.add(normalized);
                }
            }
        }
        return sampleLines;
    }

    private String stripBom(String line)
    {
        if (!line.isEmpty() && line.charAt(0) == '\ufeff')
        {
            return line.substring(1);
        }
        return line;
    }

    private double consistencyScore(List<String> lines, char sep, char quote)
    {
        int headerColumns = countFieldsOutsideQuotes(lines.getFirst(), sep, quote);
        if (headerColumns <= 1)
        {
            return 0.0;
        }
        if (lines.size() == 1)
        {
            return Math.log1p(headerColumns) * 0.5;
        }

        Map<Integer, Integer> colCountFreq = new HashMap<>();
        int matchingHeader = 0;
        for (int i = 1; i < lines.size(); i++)
        {
            int cols = countFieldsOutsideQuotes(lines.get(i), sep, quote);
            colCountFreq.merge(cols, 1, Integer::sum);
            if (cols == headerColumns)
            {
                matchingHeader++;
            }
        }

        int dataLines = lines.size() - 1;
        int modalCount = colCountFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        if (modalCount <= 1)
        {
            return 0.0;
        }

        int modalFreq = colCountFreq.getOrDefault(modalCount, 0);
        double modalConsistency = (double) modalFreq / dataLines;
        double headerConsistency = (double) matchingHeader / dataLines;
        double headerPenalty = modalCount == headerColumns ? 1.0 : 0.45;

        return (modalConsistency + headerConsistency) * 0.5 * Math.log1p(modalCount) * headerPenalty;
    }

    private int countFieldsOutsideQuotes(String line, char sep, char quote)
    {
        int fields = 1;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (c == quote)
            {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == quote)
                {
                    i++;
                }
                else
                {
                    inQuotes = !inQuotes;
                }
            }
            else if (c == sep && !inQuotes)
            {
                fields++;
            }
        }
        return fields;
    }
}
