package org.open4goods.services.opendata;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.open4goods.model.product.Product;

import com.opencsv.CSVWriter;

public class DatasetWriter implements AutoCloseable {
    private final DatasetDefinition definition;
    private final CSVWriter writer;
    private final ZipOutputStream zipOutputStream;
    private final AtomicLong count = new AtomicLong();

    public DatasetWriter(DatasetDefinition definition) throws IOException {
        this.definition = definition;
        FileOutputStream fos = new FileOutputStream(definition.zipFile());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fos);
        this.zipOutputStream = new ZipOutputStream(bufferedOutputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8));
        this.writer = new CSVWriter(bufferedWriter);
        ZipEntry entry = new ZipEntry(definition.filename());
        zipOutputStream.putNextEntry(entry);
        writer.writeNext(definition.header());
    }

    public void write(Product product) {
        writer.writeNext(definition.rowMapper().apply(product));
        count.incrementAndGet();
    }

    public long count() {
        return count.get();
    }

    public DatasetDefinition definition() {
        return definition;
    }

    @Override
    public void close() throws IOException {
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
    }
}