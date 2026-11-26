package org.open4goods.services.opendata;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.open4goods.model.product.Product;

import com.opencsv.CSVWriter;

/**
 * Writes one dataset as a single UTF-8 CSV entry of a zip, and computes while writing what its manifest needs: the
 * SHA-256 of the zip bytes, the row count and the non-empty cells per column.
 */
public class DatasetWriter implements AutoCloseable {
    private final DatasetDefinition definition;
    private final CSVWriter writer;
    private final ZipOutputStream zipOutputStream;
    private final MessageDigest digest;
    private final AtomicLong count = new AtomicLong();
    private final long[] filledCells;
    private final List<String[]> sample = new ArrayList<>();
    private String sha256;
    private boolean closed;

    public DatasetWriter(DatasetDefinition definition) throws IOException {
        this.definition = definition;
        this.filledCells = new long[definition.header().length];
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is not available", e);
        }
        FileOutputStream fos = new FileOutputStream(definition.zipFile());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new DigestOutputStream(fos, digest));
        this.zipOutputStream = new ZipOutputStream(bufferedOutputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8));
        this.writer = new CSVWriter(bufferedWriter);
        ZipEntry entry = new ZipEntry(definition.filename());
        zipOutputStream.putNextEntry(entry);
        writer.writeNext(definition.header());
    }

    public void write(Product product) {
        String[] row = definition.rowMapper().apply(product);
        for (int i = 0; i < filledCells.length && i < row.length; i++) {
            if (row[i] != null && !row[i].isEmpty()) {
                filledCells[i]++;
            }
        }
        if (sample.size() < DatasetDefinition.SAMPLE_ROWS) {
            sample.add(row.clone());
        }
        writer.writeNext(row);
        count.incrementAndGet();
    }

    /**
     * @return the header then the first {@link DatasetDefinition#SAMPLE_ROWS} rows, as written in the CSV
     */
    public List<String[]> sampleRows() {
        return List.copyOf(sample);
    }

    public long count() {
        return count.get();
    }

    /**
     * @param column index of the column in the header
     * @return data rows with a non-empty value in this column
     */
    public long filledCells(int column) {
        return filledCells[column];
    }

    /**
     * @return lowercase hexadecimal SHA-256 of the zip, available once the writer is closed
     */
    public String sha256() {
        if (sha256 == null) {
            throw new IllegalStateException("The SHA-256 of " + definition.filename() + " is known once the writer is closed");
        }
        return sha256;
    }

    public DatasetDefinition definition() {
        return definition;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException first = null;
        try {
            writer.flush();
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            first = e;
        }
        try {
            writer.close();
        } catch (IOException closeException) {
            if (first != null) {
                closeException.addSuppressed(first);
            }
            throw closeException;
        }
        if (first != null) {
            throw first;
        }
        sha256 = HexFormat.of().formatHex(digest.digest());
    }
}
