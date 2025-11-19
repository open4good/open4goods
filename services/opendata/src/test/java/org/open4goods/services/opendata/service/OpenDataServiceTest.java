package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttributes;
import org.open4goods.model.attribute.ReferentielKey;
import org.junit.jupiter.api.extension.ExtendWith;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.GtinInfo;
import org.open4goods.model.product.Product;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.productrepository.services.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OpenDataServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OpenDataConfig openDataConfig;

    @TempDir
    Path tempDir;

    private OpenDataService service;

    private File tmpIsbn;
    private File tmpGtin;
    private File finalIsbn;
    private File finalGtin;

    @BeforeEach
    void setUp() {
        tmpIsbn = tempDir.resolve("isbn-tmp.zip").toFile();
        tmpGtin = tempDir.resolve("gtin-tmp.zip").toFile();
        finalIsbn = tempDir.resolve("isbn.zip").toFile();
        finalGtin = tempDir.resolve("gtin.zip").toFile();

        lenient().when(openDataConfig.tmpIsbnZipFile()).thenReturn(tmpIsbn);
        lenient().when(openDataConfig.tmpGtinZipFile()).thenReturn(tmpGtin);
        lenient().when(openDataConfig.isbnZipFile()).thenReturn(finalIsbn);
        lenient().when(openDataConfig.gtinZipFile()).thenReturn(finalGtin);
        lenient().when(openDataConfig.getGenerationEnabled()).thenReturn(Boolean.TRUE);
        lenient().when(openDataConfig.isGenerationEnabled()).thenReturn(true);
        lenient().when(openDataConfig.getDownloadSpeedKb()).thenReturn(256);
        lenient().when(openDataConfig.getConcurrentDownloads()).thenReturn(2);

        service = new OpenDataService(productRepository, openDataConfig);
    }

    @Test
    void limitedRateStreamShouldEnforceConcurrencyLimits() throws Exception {
        File dataset = tempDir.resolve("dataset.csv").toFile();
        try (FileOutputStream outputStream = new FileOutputStream(dataset)) {
            outputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }

        when(openDataConfig.getConcurrentDownloads()).thenReturn(1);

        InputStream firstDownload = service.limitedRateStream(dataset.getAbsolutePath());
        assertThatThrownBy(() -> service.limitedRateStream(dataset.getAbsolutePath()))
                .isInstanceOf(TechnicalException.class);
        firstDownload.close();

        // Once the stream is closed the counter must be decremented allowing a new download.
        try (InputStream second = service.limitedRateStream(dataset.getAbsolutePath())) {
            assertThat(second).isNotNull();
        }
    }

    @Test
    void processDataFilesShouldExportAllBarcodesInSinglePass() throws Exception {
        when(productRepository.exportAll(anyCollection(), any(String[].class))).thenReturn(Stream.empty());

        service.processDataFiles();

        ArgumentCaptor<Collection<BarcodeType>> typesCaptor = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String[]> fieldsCaptor = ArgumentCaptor.forClass(String[].class);
        verify(productRepository).exportAll(typesCaptor.capture(), fieldsCaptor.capture());

        assertThat(typesCaptor.getValue()).containsExactlyInAnyOrder(
                BarcodeType.ISBN_13,
                BarcodeType.GTIN_8,
                BarcodeType.GTIN_12,
                BarcodeType.GTIN_13,
                BarcodeType.GTIN_14);
        assertThat(fieldsCaptor.getValue()).contains(
                "datasourceCategories",
                "attributes",
                "offerNames");
        assertThat(tmpIsbn).exists();
        assertThat(tmpGtin).exists();
    }

    @Test
    void processDataFilesShouldCloseRepositoryStream() throws Exception {
        @SuppressWarnings("unchecked")
        Stream<Product> stream = mock(Stream.class);
        doAnswer(invocation -> null).when(stream).forEach(Mockito.any());
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(stream).close();

        lenient().when(productRepository.exportAll(anyCollection(), any(String[].class))).thenReturn(stream);

        service.processDataFiles();

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        verify(stream).close();
    }

    @Test
    void processDataFilesShouldWriteCsvRows() throws Exception {
        Product isbnProduct = buildProduct("9781234567890", BarcodeType.ISBN_13);
        Product gtinProduct = buildProduct("1234567890123", BarcodeType.GTIN_13);

        when(productRepository.exportAll(anyCollection(), any(String[].class))).thenReturn(Stream.of(isbnProduct, gtinProduct));

        service.processDataFiles();

        List<String> isbnLines = readZipLines(tmpIsbn, "open4goods-isbn-dataset.csv");
        List<String> gtinLines = readZipLines(tmpGtin, "open4goods-gtin-dataset.csv");

        assertThat(isbnLines).hasSize(2);
        assertThat(isbnLines.get(1)).contains("Test Editor", "https://nudger.fr/9781234567890");
        assertThat(gtinLines).hasSize(2);
        assertThat(gtinLines.get(1)).contains("TestBrand", "https://nudger.fr/1234567890123");
        assertThat(gtinLines.get(1)).contains("electronics").contains("gaming");

        verify(productRepository).exportAll(anyCollection(), any(String[].class));
    }

    @Test
    void generateOpendataShouldNotMoveFilesWhenProcessingFails() throws Exception {
        FileUtils.writeStringToFile(finalIsbn, "existing", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(finalGtin, "existing", StandardCharsets.UTF_8);

        lenient().when(productRepository.exportAll(anyCollection(), any(String[].class)))
                .thenThrow(new RuntimeException("boom"));

        service.generateOpendata();

        assertThat(FileUtils.readFileToString(finalIsbn, StandardCharsets.UTF_8)).isEqualTo("existing");
        assertThat(FileUtils.readFileToString(finalGtin, StandardCharsets.UTF_8)).isEqualTo("existing");
        assertThat(tmpIsbn).doesNotExist();
    }

    @Test
    void humanReadableByteCountShouldFormatValues() {
        assertThat(OpenDataService.humanReadableByteCountBin(999)).isEqualTo("999 B");
        assertThat(OpenDataService.humanReadableByteCountBin(1_500).replace("," , ".")) .isEqualTo("1.5 kB");
    }

    @Test
    void csvMappersShouldHandleMissingData() throws Exception {
        Product product = mock(Product.class);
        when(product.getAttributes()).thenReturn(null);

        String[] gtinEntry = invokeMapper("toGtinEntry", product);
        String[] isbnEntry = invokeMapper("toIsbnEntry", product);

        assertThat(gtinEntry).doesNotContainNull();
        assertThat(isbnEntry).doesNotContainNull();
    }

    private String[] invokeMapper(String methodName, Product product) throws Exception {
        Method method = OpenDataService.class.getDeclaredMethod(methodName, Product.class);
        method.setAccessible(true);
        return (String[]) method.invoke(service, product);
    }

    private Product buildProduct(String gtin, BarcodeType barcodeType) {
        Product product = new Product();
        product.setId(Long.parseLong(gtin));
        product.setOffersCount(3);
        product.setLastChange(Instant.parse("2024-01-01T00:00:00Z").toEpochMilli());
        product.setOfferNames(new HashSet<>(Set.of("short name", "a longer offer name")));
        product.setDatasourceCategories(new HashSet<>(Set.of("electronics", "gaming")));

        ProductAttributes attributes = new ProductAttributes();
        attributes.addReferentielAttribute(ReferentielKey.BRAND, "TestBrand");
        attributes.addReferentielAttribute(ReferentielKey.MODEL, "ModelX");
        addIndexedAttribute(attributes, "EDITEUR", "Test Editor");
        addIndexedAttribute(attributes, "FORMAT", "Broché");
        addIndexedAttribute(attributes, "NB DE PAGES", "320");
        addIndexedAttribute(attributes, "CLASSIFICATION DECITRE 1", "Class1");
        addIndexedAttribute(attributes, "CLASSIFICATION DECITRE 2", "Class2");
        addIndexedAttribute(attributes, "CLASSIFICATION DECITRE 3", "Class3");
        addIndexedAttribute(attributes, "SOUSCATEGORIE", "SousCat1");
        addIndexedAttribute(attributes, "SOUSCATEGORIE2", "SousCat2");
        product.setAttributes(attributes);

        AggregatedPrice aggregatedPrice = new AggregatedPrice();
        aggregatedPrice.setPrice(42.42d);
        aggregatedPrice.setCompensation(0.5d);
        aggregatedPrice.setCurrency(Currency.EUR);

        AggregatedPrices aggregatedPrices = new AggregatedPrices();
        aggregatedPrices.setMinPrice(aggregatedPrice);
        product.setPrice(aggregatedPrices);

        GtinInfo gtinInfo = new GtinInfo();
        gtinInfo.setCountry("FR");
        gtinInfo.setUpcType(barcodeType);
        product.setGtinInfos(gtinInfo);

        return product;
    }

    private void addIndexedAttribute(ProductAttributes attributes, String key, String value) {
        attributes.getIndexed().put(key, new IndexedAttribute(key, value));
    }

    private List<String> readZipLines(File file, String entryName) throws Exception {
        try (ZipInputStream inputStream = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        List<String> lines = new ArrayList<>();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            lines.add(line);
                        }
                        return lines;
                    }
                }
            }
        }
        throw new IllegalStateException("Unable to locate entry " + entryName + " in " + file);
    }
}

