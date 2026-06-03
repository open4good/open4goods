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

    /** RFC-4180 quotes are preferred, but malformed partner feeds may need no quote handling. */
    private static final Character[] QUOTE_CANDIDATES = {'"', null};

    /** Some partner feeds use backslash-escaped quotes instead of doubled quotes. */
    private static final Character[] ESCAPE_CANDIDATES = {null, '\\'};

    /** Number of non-blank lines sampled, including the header. */
    private static final int DETECTION_SAMPLE_LINES = 2_000;

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
        Character bestQuote = '"';
        Character bestEscape = null;
        double bestScore = -1.0;

        for (char sep : SEPARATOR_CANDIDATES)
        {
            for (Character quote : QUOTE_CANDIDATES)
            {
                Character[] escapeCandidates = quote == null ? new Character[] {null} : ESCAPE_CANDIDATES;
                for (Character escape : escapeCandidates)
                {
                    double score = consistencyScore(sampleLines, sep, quote, escape);
                    if (score > bestScore)
                    {
                        bestScore = score;
                        bestSep = sep;
                        bestQuote = quote;
                        bestEscape = escape;
                    }
                }
            }
        }

        CsvSchema schema = buildSchema(sampleLines.getFirst(), bestSep, bestQuote);
        if (bestEscape != null)
        {
            schema = schema.withEscapeChar(bestEscape);
        }
        return schema;
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

    private CsvSchema buildSchema(String header, char separator, Character quote)
    {
        CsvSchema.Builder builder = CsvSchema.builder()
                .setColumnSeparator(separator);
        if (quote == null)
        {
            for (String column : splitUnquotedHeader(header, separator))
            {
                builder.addColumn(stripWrappingQuotes(column));
            }
            return builder
                    .disableQuoteChar()
                    .setSkipFirstDataRow(true)
                    .build();
        }
        return builder
                .setQuoteChar(quote)
                .setUseHeader(true)
                .build();
    }

    private List<String> splitUnquotedHeader(String header, char separator)
    {
        List<String> columns = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < header.length(); i++)
        {
            if (header.charAt(i) == separator)
            {
                columns.add(header.substring(start, i));
                start = i + 1;
            }
        }
        columns.add(header.substring(start));
        return columns;
    }

    private String stripWrappingQuotes(String value)
    {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"')
        {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private double consistencyScore(List<String> lines, char sep, Character quote, Character escape)
    {
        int headerColumns = countFieldsOutsideQuotes(lines.getFirst(), sep, quote, escape);
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
            int cols = countFieldsOutsideQuotes(lines.get(i), sep, quote, escape);
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

    private int countFieldsOutsideQuotes(String line, char sep, Character quote, Character escape)
    {
        int fields = 1;
        boolean inQuotes = false;
        boolean fieldStarted = false;
        boolean quoteClosed = false;
        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (c == sep && !inQuotes)
            {
                fields++;
                fieldStarted = false;
                quoteClosed = false;
                continue;
            }
            if (quoteClosed)
            {
                if (!Character.isWhitespace(c))
                {
                    return 0;
                }
                continue;
            }
            if (escape != null && c == escape && inQuotes && i + 1 < line.length())
            {
                i++;
                fieldStarted = true;
                continue;
            }
            if (quote != null && c == quote)
            {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == quote)
                {
                    i++;
                }
                else if (inQuotes)
                {
                    inQuotes = false;
                    quoteClosed = true;
                }
                else if (!fieldStarted)
                {
                    inQuotes = true;
                    fieldStarted = true;
                }
                else
                {
                    return 0;
                }
            }
            else if (inQuotes || !Character.isWhitespace(c))
            {
                fieldStarted = true;
            }
        }
        return inQuotes ? 0 : fields;
    }
}
