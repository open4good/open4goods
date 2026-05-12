package org.open4goods.services.eprelservice.service;

import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.open4goods.model.eprel.EprelProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reads EPREL catalogues distributed as ZIP files containing JSON documents.
 */
@Component
public class JsonZipEprelCatalogueParser implements EprelCatalogueParser
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonZipEprelCatalogueParser.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final tools.jackson.databind.ObjectReader eprelProductReader = objectMapper
            .readerFor(EprelProduct.class)
            .without(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    /**
     * Creates the parser.
     *
     * @param objectMapper configured Jackson mapper
     */
    public JsonZipEprelCatalogueParser()
    {
    }

    @Override
    public void parse(Path zipFile, Consumer<EprelProduct> consumer) throws IOException
    {
        Objects.requireNonNull(zipFile, "zipFile");
        Objects.requireNonNull(consumer, "consumer");

        try (InputStream fileStream = Files.newInputStream(zipFile); ZipInputStream zipInputStream = new ZipInputStream(fileStream))
        {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null)
            {
                if (entry.isDirectory())
                {
                    continue;
                }
                Path tempJson = Files.createTempFile("eprel-entry-", ".json");
                try
                {
                    try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tempJson)))
                    {
                        zipInputStream.transferTo(outputStream);
                    }
                    processJsonFile(tempJson, consumer);
                }
                finally
                {
                    Files.deleteIfExists(tempJson);
                    zipInputStream.closeEntry();
                }
            }
        }
    }

    private void processJsonFile(Path jsonFile, Consumer<EprelProduct> consumer) throws IOException
    {
        TokenStreamFactory factory = objectMapper.tokenStreamFactory();
        try (JsonParser parser = factory.createParser(jsonFile.toFile()))
        {
            if (parser.nextToken() == JsonToken.START_ARRAY)
            {
                while (parser.nextToken() != JsonToken.END_ARRAY)
                {
                    EprelProduct product = eprelProductReader.readValue(parser);
                    consumer.accept(product);
                }
            }
            else
            {
                LOGGER.warn("Unexpected JSON structure in EPREL catalogue: {}", jsonFile);
            }
        }
    }
}
