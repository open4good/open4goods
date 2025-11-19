package org.open4goods.services.opendata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasetWriterManager implements AutoCloseable {
    private final List<DatasetWriter> writers = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetWriterManager.class);

    public DatasetWriterManager(List<DatasetDefinition> datasetDefinitions) throws IOException {
        try {
            for (DatasetDefinition definition : datasetDefinitions) {
                writers.add(new DatasetWriter(definition));
            }
        } catch (IOException e) {
            closeQuietly();
            throw e;
        }
    }

    public List<DatasetWriter> writers() {
        return writers;
    }

    @Override
    public void close() throws IOException {
        IOException first = null;
        for (DatasetWriter writer : writers) {
            try {
                writer.close();
            } catch (IOException e) {
                if (first == null) {
                    first = e;
                } else {
                    first.addSuppressed(e);
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }

    private void closeQuietly() {
        for (DatasetWriter writer : writers) {
            try {
                writer.close();
            } catch (IOException e) {
                LOGGER.warn("Unable to close dataset {}", writer.definition().filename(), e);
            }
        }
    }
}