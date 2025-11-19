package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.Product;
import org.junit.jupiter.api.extension.ExtendWith;
import org.open4goods.model.exceptions.TechnicalException;
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
    void processDataFilesShouldExportIsbnAndGtinConcurrently() throws Exception {
        lenient().when(productRepository.exportAll(Mockito.<BarcodeType[]>any())).thenAnswer(invocation -> {
            try {
                TimeUnit.MILLISECONDS.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Stream.empty();
        });

        long start = System.nanoTime();
        service.processDataFiles();
        long elapsedMillis = java.time.Duration.ofNanos(System.nanoTime() - start).toMillis();

        org.mockito.Mockito.verify(productRepository).exportAll(BarcodeType.ISBN_13);
        org.mockito.Mockito.verify(productRepository).exportAll(BarcodeType.GTIN_8, BarcodeType.GTIN_12, BarcodeType.GTIN_13, BarcodeType.GTIN_14);
        assertThat(elapsedMillis).isLessThan(700);
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

        lenient().when(productRepository.exportAll(Mockito.<BarcodeType[]>any())).thenAnswer(invocation -> {
            BarcodeType[] types = extractBarcodeTypes(invocation.getArguments());
            if (isIsbnTypes(types)) {
                return stream;
            }
            return Stream.empty();
        });

        service.processDataFiles();

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        verify(stream).close();
    }

    @Test
    void generateOpendataShouldNotMoveFilesWhenProcessingFails() throws Exception {
        FileUtils.writeStringToFile(finalIsbn, "existing", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(finalGtin, "existing", StandardCharsets.UTF_8);

        lenient().when(productRepository.exportAll(Mockito.<BarcodeType[]>any())).thenAnswer(invocation -> {
            BarcodeType[] types = extractBarcodeTypes(invocation.getArguments());
            if (isIsbnTypes(types)) {
                throw new RuntimeException("boom");
            }
            return Stream.empty();
        });

        service.generateOpendata();

        assertThat(FileUtils.readFileToString(finalIsbn, StandardCharsets.UTF_8)).isEqualTo("existing");
        assertThat(FileUtils.readFileToString(finalGtin, StandardCharsets.UTF_8)).isEqualTo("existing");
        assertThat(tmpIsbn).doesNotExist();
    }

    @Test
    void humanReadableByteCountShouldFormatValues() {
        assertThat(OpenDataService.humanReadableByteCountBin(999)).isEqualTo("999 B");
        assertThat(OpenDataService.humanReadableByteCountBin(1_500)).isEqualTo("1.5 kB");
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

    private boolean isIsbnTypes(BarcodeType[] types) {
        return types.length == 1 && types[0] == BarcodeType.ISBN_13;
    }

    private String[] invokeMapper(String methodName, Product product) throws Exception {
        Method method = OpenDataService.class.getDeclaredMethod(methodName, Product.class);
        method.setAccessible(true);
        return (String[]) method.invoke(service, product);
    }

    private BarcodeType[] extractBarcodeTypes(Object[] args) {
        if (args.length == 1 && args[0] instanceof BarcodeType[]) {
            return (BarcodeType[]) args[0];
        }
        BarcodeType[] result = new BarcodeType[args.length];
        for (int i = 0; i < args.length; i++) {
            result[i] = (BarcodeType) args[i];
        }
        return result;
    }
}

