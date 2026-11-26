package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ProductAttributes;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.model.product.GtinInfo;
import org.open4goods.model.product.Product;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.OpenDataGenerationStatus.State;
import org.open4goods.services.opendata.model.OpenDataManifest;
import org.open4goods.services.opendata.model.OpenDataPublicationStatus;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.springframework.boot.health.contributor.Status;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class OpenDataServiceTest {

    private static final String GTIN_CSV = "open4goods-gtin-dataset.csv";
    private static final String ISBN_CSV = "open4goods-isbn-dataset.csv";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VerticalsConfigService verticalsConfigService;

    @TempDir
    Path tempDir;

    private OpenDataConfig openDataConfig;
    private SimpleMeterRegistry meterRegistry;
    private OpenDataService service;
    private final OpenDataManifestStore manifestStore = new OpenDataManifestStore();

    @BeforeEach
    void setUp() {
        openDataConfig = new OpenDataConfig();
        openDataConfig.setRootFolder(tempDir.toString());
        openDataConfig.setGenerationEnabled(Boolean.TRUE);
        openDataConfig.setPagePause(Duration.ZERO);
        openDataConfig.setExportPageSize(2);
        openDataConfig.setPublicBaseUrl("https://data.nudger.fr/");

        lenient().when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(1L);
        lenient().when(verticalsConfigService.getConfigsWithoutDefault(true)).thenReturn(List.of());

        meterRegistry = new SimpleMeterRegistry();
        service = new OpenDataService(productRepository, openDataConfig, meterRegistry, verticalsConfigService);
    }

    @Test
    void generationShouldPublishZipsThenManifests() throws Exception {
        exportReturns(buildProduct("9781234567890", BarcodeType.ISBN_13), buildProduct("1234567890123", BarcodeType.GTIN_13));

        assertThat(service.generateOpendata()).isTrue();

        List<String> gtinLines = readZipLines(openDataConfig.gtinZipFile(), GTIN_CSV);
        List<String> isbnLines = readZipLines(openDataConfig.isbnZipFile(), ISBN_CSV);
        // Row conversion (GOU-2616): no url nor categories, UTC date, a cell quoted only when it needs to be.
        assertThat(gtinLines).containsExactly(
                "gtin,brand,model,name,last_updated,gs1_country,gtinType,offers_count,min_price,currency,sources,vertical",
                "1234567890123,TestBrand,ModelX,short name,2024-01-01,FR,GTIN_13,3,42.42,EUR,eprel|rakuten.com,");
        assertThat(isbnLines).containsExactly(
                "isbn,title,last_updated,offers_count,min_price,currency,editeur,format,nb_page,souscategorie,"
                        + "souscategorie2,sources",
                "9781234567890,short name,2024-01-01,3,42.42,EUR,Test Editor,Broché,320,SousCat1,SousCat2,eprel|rakuten.com");

        DatasetManifest gtin = manifestStore.readDataset(openDataConfig.gtinManifestFile()).orElseThrow();
        assertThat(gtin.schemaVersion()).isEqualTo(OpenDataManifest.SCHEMA_VERSION).isEqualTo(1);
        assertThat(gtin.datasetVersion()).isEqualTo(2);
        assertThat(gtin.id()).isEqualTo("gtin");
        assertThat(gtin.file()).isEqualTo("gtin-open-data.zip");
        assertThat(gtin.url()).isEqualTo("https://data.nudger.fr/gtin-open-data.zip");
        assertThat(gtin.format()).isEqualTo("text/csv+zip");
        assertThat(gtin.license()).isEqualTo("ODbL-1.0");
        assertThat(gtin.rows()).isEqualTo(1);
        assertThat(gtin.indexCount()).isEqualTo(1);
        assertThat(gtin.bytes()).isEqualTo(openDataConfig.gtinZipFile().length());
        assertThat(gtin.sha256()).isEqualTo(sha256(openDataConfig.gtinZipFile()));
        assertThat(Instant.parse(gtin.generatedAt())).isNotNull();
        assertThat(gtin.columns()).extracting(column -> column.name()).containsExactly(OpenDataService.GTIN_HEADER);
        assertThat(gtin.columns()).filteredOn(column -> !column.name().equals("vertical"))
                .allSatisfy(column -> assertThat(column.fillRate()).isEqualTo(1.0));
        assertThat(gtin.sample()).isEqualTo("gtin-open-data-sample.csv");
        List<String> sample = java.nio.file.Files.readAllLines(
                openDataConfig.gtinManifestFile().toPath().resolveSibling("gtin-open-data-sample.csv"));
        assertThat(sample).isEqualTo(gtinLines);

        DatasetManifest isbn = manifestStore.readDataset(openDataConfig.isbnManifestFile()).orElseThrow();
        assertThat(isbn.datasetVersion()).isEqualTo(1);
        assertThat(isbn.sha256()).isEqualTo(sha256(openDataConfig.isbnZipFile()));

        OpenDataManifest all = manifestStore.readAll(openDataConfig.manifestFile()).orElseThrow();
        assertThat(all.license()).isEqualTo("ODbL-1.0");
        assertThat(all.datasets()).containsExactly(gtin, isbn);

        assertThat(openDataConfig.tmpDirectory()).doesNotExist();
        assertThat(service.status().state()).isEqualTo(State.SUCCEEDED);
        assertThat(service.status().rows()).containsEntry("gtin", 1L).containsEntry("isbn", 1L);
        assertThat(service.health().getStatus()).isEqualTo(Status.UP);
        assertThat(meterRegistry.get("opendata.generation.runs").tag("outcome", "success").counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("opendata.dataset.rows").tag("dataset", "gtin").gauge().value()).isEqualTo(1);
        assertThat(meterRegistry.get("opendata.generation.failed").gauge().value()).isZero();
    }

    @Test
    void gtinDatasetShouldEndWithTheVerticalColumn() throws Exception {
        assertThat(OpenDataService.GTIN_HEADER).hasSize(12).endsWith("sources", "vertical");
        Product inVertical = buildProduct("1234567890123", BarcodeType.GTIN_13);
        inVertical.setVertical("tv");
        exportReturns(inVertical, buildProduct("1234567890128", BarcodeType.GTIN_13));
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);

        assertThat(service.generateOpendata()).isTrue();

        assertThat(readZipLines(openDataConfig.gtinZipFile(), GTIN_CSV)).containsExactly(
                "gtin,brand,model,name,last_updated,gs1_country,gtinType,offers_count,min_price,currency,sources,vertical",
                "1234567890123,TestBrand,ModelX,short name,2024-01-01,FR,GTIN_13,3,42.42,EUR,eprel|rakuten.com,tv",
                "1234567890128,TestBrand,ModelX,short name,2024-01-01,FR,GTIN_13,3,42.42,EUR,eprel|rakuten.com,");
        DatasetManifest gtin = manifestStore.readDataset(openDataConfig.gtinManifestFile()).orElseThrow();
        assertThat(gtin.columns()).filteredOn(column -> column.name().equals("vertical"))
                .singleElement().satisfies(column -> assertThat(column.fillRate()).isEqualTo(0.5));
    }

    @Test
    void fillRateShouldCountEmptyCells() throws Exception {
        Product withoutPrice = buildProduct("1234567890128", BarcodeType.GTIN_13);
        withoutPrice.setPrice(new AggregatedPrices());
        exportReturns(buildProduct("1234567890123", BarcodeType.GTIN_13), withoutPrice);
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);

        assertThat(service.generateOpendata()).isTrue();

        DatasetManifest gtin = manifestStore.readDataset(openDataConfig.gtinManifestFile()).orElseThrow();
        assertThat(gtin.rows()).isEqualTo(2);
        assertThat(gtin.columns()).filteredOn(column -> column.name().equals("min_price"))
                .singleElement().satisfies(column -> assertThat(column.fillRate()).isEqualTo(0.5));
        assertThat(gtin.columns()).filteredOn(column -> column.name().equals("gtin"))
                .singleElement().satisfies(column -> assertThat(column.fillRate()).isEqualTo(1.0));
    }

    @Test
    void noFileShouldContainDecitre() throws Exception {
        // GOU-2262: a product carrying the purged Decitre keys must not leak them into any produced file.
        Product book = buildProduct("9781234567890", BarcodeType.ISBN_13);
        Product gtinProduct = buildProduct("1234567890123", BarcodeType.GTIN_13);
        for (Product product : List.of(book, gtinProduct)) {
            for (String key : List.of("CLASSIFICATION DECITRE 1", "CLASSIFICATION DECITRE 2", "CLASSIFICATION DECITRE 3",
                    "ATTRIBUTS", "LIVRES ORIGINE", "PROPRIETAIRE")) {
                addIndexedAttribute(product.getAttributes(), key, "Decitre " + key);
                ProductAttribute attribute = new ProductAttribute();
                attribute.setName(key);
                attribute.setValue("decitre.fr");
                product.getAttributes().getAll().put(key, attribute);
            }
        }
        exportReturns(book, gtinProduct);

        assertThat(service.generateOpendata()).isTrue();

        List<String> produced = new ArrayList<>();
        produced.addAll(readZipLines(openDataConfig.gtinZipFile(), GTIN_CSV));
        produced.addAll(readZipLines(openDataConfig.isbnZipFile(), ISBN_CSV));
        for (File manifest : List.of(openDataConfig.manifestFile(), openDataConfig.gtinManifestFile(),
                openDataConfig.isbnManifestFile())) {
            produced.add(Files.readString(manifest.toPath()));
        }
        assertThat(produced).hasSize(7)
                .allSatisfy(content -> assertThat(content.toLowerCase(Locale.ROOT)).doesNotContain("decitre"));
        assertThat(OpenDataService.EXPORT_SOURCE_FIELDS)
                .noneMatch(field -> field.toLowerCase(Locale.ROOT).contains("decitre"))
                .noneMatch(field -> field.contains("ATTRIBUTS") || field.contains("LIVRES ORIGINE") || field.contains("PROPRIETAIRE"));
    }

    @Test
    void exportShouldReadAMinimalProjectionWithBoundedPages() throws Exception {
        openDataConfig.setExportPageSize(1500);
        exportReturns();
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);

        service.generateOpendata();

        ArgumentCaptor<String[]> fields = ArgumentCaptor.forClass(String[].class);
        verify(productRepository).exportAll(fields.capture(), Mockito.eq(1500));
        assertThat(fields.getValue())
                .contains("id", "offerNames", "attributes.referentielAttributes", "price.minPrice.price",
                        "attributes.indexed.EDITEUR", "attributes.all.NB DE PAGES")
                .doesNotContain("attributes", "price", "offers");
    }

    @Test
    void mainPassShouldReadNoImpactFieldWithTheMainPage() throws Exception {
        // GOU-2556: the criteria sources weigh tens of KB per product; the scroll of the whole catalogue never reads them.
        openDataConfig.setExportPageSize(2000);
        openDataConfig.setImpactExportPageSize(500);
        when(verticalsConfigService.getConfigsWithoutDefault(true))
                .thenReturn(List.of(verticalConfig("oven", "CLASSE_ENERGY", "WARRANTY")));
        when(productRepository.countItemsByVertical("oven")).thenReturn(0L);
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        exportReturns();

        assertThat(service.generateOpendata()).isTrue();

        ArgumentCaptor<String[]> fields = ArgumentCaptor.forClass(String[].class);
        verify(productRepository).exportAll(fields.capture(), Mockito.eq(2000));
        assertThat(fields.getValue()).containsExactly(OpenDataService.EXPORT_SOURCE_FIELDS)
                .noneMatch(field -> field.startsWith("attributes.indexed.") && field.contains(".source"))
                .noneMatch(field -> field.contains("WARRANTY") || field.contains("CLASSE_ENERGY"));
    }

    @Test
    void impactPassShouldReadOnlyTheImpactVerticalsWithTheImpactPage() throws Exception {
        openDataConfig.setExportPageSize(2000);
        openDataConfig.setImpactExportPageSize(500);
        when(verticalsConfigService.getConfigsWithoutDefault(true))
                .thenReturn(List.of(verticalConfig("oven", "WARRANTY"), verticalConfig("tv", "REPAIRABILITY_INDEX")));
        when(productRepository.countItemsByVertical("oven")).thenReturn(1L);
        when(productRepository.countItemsByVertical("tv")).thenReturn(1L);
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        Product oven = impactProduct("1234567890123", "oven", "WARRANTY", "2", "eprel");
        Product tv = impactProduct("1234567890135", "tv", "REPAIRABILITY_INDEX", "7.1", "icecat.biz");
        Product book = buildProduct("9781234567890", BarcodeType.ISBN_13);
        exportReturns(oven, tv, book);

        assertThat(service.generateOpendata()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> verticals = ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<String[]> fields = ArgumentCaptor.forClass(String[].class);
        verify(productRepository).exportAllInVerticals(verticals.capture(), fields.capture(), Mockito.eq(500));
        assertThat(verticals.getValue()).containsExactly("oven", "tv");
        assertThat(fields.getValue())
                .contains("id", "vertical", "names.url", "offerNames", "attributes.referentielAttributes",
                        "attributes.indexed.WARRANTY.value", "attributes.indexed.REPAIRABILITY_INDEX.source.cleanedValue")
                .doesNotContain("attributes.indexed.WARRANTY", "attributes", "offers", "price.minPrice.price",
                        "attributes.indexed.EDITEUR");
        assertThat(readZipLines(openDataConfig.impactZipFile("oven"), "impact-oven.csv")).hasSize(2);
        assertThat(readZipLines(openDataConfig.impactZipFile("tv"), "impact-tv.csv")).hasSize(2);
        assertThat(readZipLines(openDataConfig.isbnZipFile(), ISBN_CSV)).hasSize(2);
    }

    @Test
    void withoutImpactDatasetThereShouldBeNoImpactPass() throws Exception {
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        exportReturns(buildProduct("1234567890123", BarcodeType.GTIN_13));

        assertThat(service.generateOpendata()).isTrue();

        verify(productRepository, never()).exportAllInVerticals(anyCollection(), any(String[].class), anyInt());
    }

    @Test
    void elasticsearchFailureShouldKeepPublishedFilesAndBeVisible() throws Exception {
        exportReturns(buildProduct("1234567890123", BarcodeType.GTIN_13), buildProduct("9781234567890", BarcodeType.ISBN_13));
        assertThat(service.generateOpendata()).isTrue();
        Snapshot published = snapshot();

        AtomicBoolean closed = new AtomicBoolean();
        Stream<Product> failing = Stream.concat(Stream.of(buildProduct("1234567890128", BarcodeType.GTIN_13)),
                Stream.<Product>generate(() -> {
                    throw new IllegalStateException("entity content is too long [104859155]");
                })).onClose(() -> closed.set(true));
        when(productRepository.exportAll(any(String[].class), anyInt())).thenReturn(failing);

        assertThat(service.generateOpendata()).isFalse();

        assertThat(snapshot()).isEqualTo(published);
        assertThat(closed).isTrue();
        assertThat(openDataConfig.tmpDirectory()).doesNotExist();
        assertFailureVisible("entity content is too long");
        assertThat(service.status().lastSuccessAt()).isNotNull();
    }

    @Test
    void lowDiskSpaceShouldRefuseThePassBeforeReading() throws Exception {
        Files.createDirectories(openDataConfig.openDataDirectory());
        Files.writeString(openDataConfig.gtinZipFile().toPath(), "x".repeat(100));
        Files.writeString(openDataConfig.isbnZipFile().toPath(), "existing");
        Snapshot published = snapshot();

        OpenDataService fullDisk = new OpenDataService(productRepository, openDataConfig, meterRegistry, verticalsConfigService) {
            @Override
            long usableSpace(Path directory) {
                return 161;
            }
        };

        assertThat(fullDisk.generateOpendata()).isFalse();

        verify(productRepository, never()).exportAll(any(String[].class), anyInt());
        assertThat(snapshot()).isEqualTo(published);
        assertThat(openDataConfig.tmpDirectory()).doesNotExist();
        assertThat(fullDisk.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(fullDisk.status().error()).contains("Not enough free disk space", "162 required");
    }

    @Test
    void rowGuardShouldRefuseADatasetThatLostRowsComparedWithTheManifest() throws Exception {
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        exportReturns(IntStream.range(0, 10).mapToObj(i -> buildProduct("123456789012" + i, BarcodeType.GTIN_13))
                .toArray(Product[]::new));
        assertThat(service.generateOpendata()).isTrue();
        Snapshot published = snapshot();

        exportReturns(IntStream.range(0, 8).mapToObj(i -> buildProduct("123456789012" + i, BarcodeType.GTIN_13))
                .toArray(Product[]::new));

        assertThat(service.generateOpendata()).isFalse();

        assertThat(snapshot()).isEqualTo(published);
        assertThat(manifestStore.readDataset(openDataConfig.gtinManifestFile()).orElseThrow().rows()).isEqualTo(10);
        assertFailureVisible("the published manifest has 10");
    }

    @Test
    void rowGuardShouldRefuseAnExportThatStoppedEarly() throws Exception {
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(100L);
        exportReturns(buildProduct("1234567890123", BarcodeType.GTIN_13));

        assertThat(service.generateOpendata()).isFalse();

        assertThat(openDataConfig.gtinZipFile()).doesNotExist();
        assertThat(openDataConfig.manifestFile()).doesNotExist();
        assertFailureVisible("the index counted 100 products");
    }

    @Test
    void disabledGenerationShouldNotReadTheIndex() {
        openDataConfig.setGenerationEnabled(Boolean.FALSE);
        OpenDataService disabled = new OpenDataService(productRepository, openDataConfig, meterRegistry, verticalsConfigService);

        assertThat(disabled.generateOpendata()).isFalse();
        assertThat(disabled.launchGeneration()).isFalse();

        verify(productRepository, never()).exportAll(any(String[].class), anyInt());
        assertThat(disabled.status().state()).isEqualTo(State.IDLE);
    }

    @Test
    void publicationShouldReceiveTheLocalFilesOfASuccessfulPass() throws Exception {
        OpenDataPublisher publisher = Mockito.mock(OpenDataPublisher.class);
        when(publisher.isEnabled()).thenReturn(true);
        OpenDataService publishing = new OpenDataService(productRepository, openDataConfig, meterRegistry, publisher, verticalsConfigService);
        exportReturns(buildProduct("9781234567890", BarcodeType.ISBN_13), buildProduct("1234567890123", BarcodeType.GTIN_13));

        assertThat(publishing.generateOpendata()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OpenDataPublisher.Dataset>> datasets = ArgumentCaptor.forClass(List.class);
        verify(publisher).publish(datasets.capture(), Mockito.eq(List.of()),
                Mockito.eq(openDataConfig.manifestFile().toPath()));
        assertThat(datasets.getValue()).extracting(OpenDataPublisher.Dataset::zipFile)
                .containsExactly(openDataConfig.gtinZipFile().toPath(), openDataConfig.isbnZipFile().toPath());
        assertThat(datasets.getValue()).extracting(OpenDataPublisher.Dataset::manifestFile)
                .containsExactly(openDataConfig.gtinManifestFile().toPath(), openDataConfig.isbnManifestFile().toPath());
        assertThat(datasets.getValue().get(0).manifest())
                .isEqualTo(manifestStore.readDataset(openDataConfig.gtinManifestFile()).orElseThrow());
    }

    @Test
    void passHooksShouldWrapASuccessfulPassAndAFailedOne() {
        java.util.List<String> events = new java.util.ArrayList<>();
        service.setPassHooks(() -> events.add("start"), () -> events.add("end"));
        exportReturns(buildProduct("9781234567890", BarcodeType.ISBN_13), buildProduct("1234567890123", BarcodeType.GTIN_13));
        assertThat(service.generateOpendata()).isTrue();
        assertThat(events).containsExactly("start", "end");

        events.clear();
        when(productRepository.exportAll(any(String[].class), anyInt())).thenThrow(new IllegalStateException("ES down"));
        assertThat(service.generateOpendata()).isFalse();
        assertThat(events).containsExactly("start", "end");
    }

    @Test
    void failedPassShouldNotPublish() {
        OpenDataPublisher publisher = Mockito.mock(OpenDataPublisher.class);
        lenient().when(publisher.isEnabled()).thenReturn(true);
        lenient().when(publisher.status()).thenReturn(OpenDataPublicationStatus.idle());
        OpenDataService publishing = new OpenDataService(productRepository, openDataConfig, meterRegistry, publisher, verticalsConfigService);
        when(productRepository.exportAll(any(String[].class), anyInt())).thenThrow(new IllegalStateException("ES down"));

        assertThat(publishing.generateOpendata()).isFalse();

        verify(publisher, never()).publish(any(), any(), any());
    }

    @Test
    void failedPublicationShouldTurnHealthDownAndKeepTheGenerationSucceeded() {
        openDataConfig.getPublication().setEnabled(true);
        OpenDataPublisher publisher = new OpenDataPublisher(openDataConfig, meterRegistry, publication -> {
            throw new IllegalStateException("R2 unreachable");
        });
        OpenDataService publishing = new OpenDataService(productRepository, openDataConfig, meterRegistry, publisher, verticalsConfigService);
        exportReturns(buildProduct("9781234567890", BarcodeType.ISBN_13), buildProduct("1234567890123", BarcodeType.GTIN_13));

        assertThat(publishing.generateOpendata()).isTrue();

        assertThat(publishing.status().state()).isEqualTo(State.SUCCEEDED);
        assertThat(publisher.status().state()).isEqualTo(State.FAILED);
        assertThat(publishing.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(publishing.health().getDetails()).containsEntry("publication", "FAILED")
                .containsEntry("publicationError", "IllegalStateException: R2 unreachable");
        assertThat(meterRegistry.get("opendata.publication.runs").tag("outcome", "failure").counter().count()).isEqualTo(1);
    }

    @Test
    void healthShouldBeUnknownWhenNoFileWasPublished() {
        assertThat(service.health().getStatus()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void humanReadableByteCountShouldFormatValues() {
        assertThat(OpenDataService.humanReadableByteCountBin(999)).isEqualTo("999 B");
        assertThat(OpenDataService.humanReadableByteCountBin(1_500).replace("," , ".")) .isEqualTo("1.5 kB");
    }

    @Test
    void corruptedOfferNamesShouldBeSkippedInExports() throws Exception {
        Product product = buildProduct("1234567890123", BarcodeType.GTIN_13);
        product.setOfferNames(new HashSet<>(Set.of("P\u00BF\u00BAcheur", "bad \uFFFD", "Chapeau de p\u00EAcheur")));
        assertThat(invokeMapper("toGtinEntry", product)[3]).isEqualTo("Chapeau de p\u00EAcheur");
        assertThat(invokeMapper("toIsbnEntry", product)[1]).isEqualTo("Chapeau de p\u00EAcheur");

        product.setOfferNames(new HashSet<>(Set.of("V\u00BF\u00BFlo")));
        assertThat(invokeMapper("toGtinEntry", product)[3]).isEmpty();
    }

    @Test
    void csvMappersShouldHandleMissingData() throws Exception {
        Product product = Mockito.mock(Product.class);
        when(product.getAttributes()).thenReturn(null);

        String[] gtinEntry = invokeMapper("toGtinEntry", product);
        String[] isbnEntry = invokeMapper("toIsbnEntry", product);

        assertThat(gtinEntry).doesNotContainNull();
        assertThat(isbnEntry).doesNotContainNull();
        // Absence is not zero: an unknown last change is no 1970-01-01.
        assertThat(gtinEntry[4]).isEmpty();
        assertThat(isbnEntry[2]).isEmpty();
    }

    @Test
    void cellsShouldBeQuotedOnlyWhenNeededAndReadBackUnchanged() throws Exception {
        Product product = buildProduct("1234567890123", BarcodeType.GTIN_13);
        product.setOfferNames(new HashSet<>(Set.of("Lampe, \"LED\" 7W")));
        exportReturns(buildProduct("9781234567890", BarcodeType.ISBN_13), product);

        assertThat(service.generateOpendata()).isTrue();

        List<String> lines = readZipLines(openDataConfig.gtinZipFile(), GTIN_CSV);
        assertThat(lines.get(1)).startsWith("1234567890123,TestBrand,ModelX,\"Lampe, \"\"LED\"\" 7W\",2024-01-01,");
        try (com.opencsv.CSVReader reader = new com.opencsv.CSVReader(new java.io.StringReader(lines.get(1)))) {
            assertThat(reader.readNext()[3]).isEqualTo("Lampe, \"LED\" 7W");
        }
    }

    @Test
    void exportDateShouldBeTheUtcDay() {
        assertThat(OpenDataService.exportDate(Instant.parse("2026-11-22T23:59:59Z").toEpochMilli())).isEqualTo("2026-11-22");
        assertThat(OpenDataService.exportDate(0)).isEmpty();
    }

    @Test
    void impactDatasetsShouldPublishRawCriteriaWithoutAnyScore() throws Exception {
        VerticalConfig oven = verticalConfig("oven", "CLASSE_ENERGY", "WARRANTY", "DATA_QUALITY");
        VerticalConfig tv = verticalConfig("tv", "REPAIRABILITY_INDEX", "DATA_QUALITY");
        when(verticalsConfigService.getConfigsWithoutDefault(true)).thenReturn(List.of(oven, tv));
        when(productRepository.countItemsByVertical("oven")).thenReturn(2L);
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        when(productRepository.countItemsByVertical("tv")).thenReturn(1L);

        Product withWarranty = impactProduct("1234567890123", "oven", "WARRANTY", "2", "eprel");
        Product withoutValue = impactProduct("1234567890128", "oven", null, null, null);
        Product television = impactProduct("1234567890135", "tv", "REPAIRABILITY_INDEX", "7.1", "icecat.biz");
        exportReturns(withWarranty, withoutValue, television);

        assertThat(service.generateOpendata()).isTrue();

        List<String> oven_lines = readZipLines(openDataConfig.impactZipFile("oven"), "impact-oven.csv");
        assertThat(oven_lines).hasSize(3);
        assertThat(oven_lines.get(0)).isEqualTo("gtin,brand,model,name,vertical,nudger_url,eprel_registration_number,"
                + "offers_count,last_updated,classe_energy,classe_energy_source,warranty_year,warranty_source");
        assertThat(oven_lines.get(1)).isEqualTo("1234567890123,TestBrand,ModelX,short name,oven,"
                + "https://nudger.fr/slug-1234567890123,,3,2024-01-01T00:00:00Z,,,2,eprel");
        // Absence is not zero: no value, no source, no default.
        assertThat(oven_lines.get(2)).endsWith(",,,,");

        DatasetManifest ovenManifest = manifestStore.readDataset(openDataConfig.impactManifestFile("oven")).orElseThrow();
        assertThat(ovenManifest.id()).isEqualTo("impact-oven");
        assertThat(ovenManifest.file()).isEqualTo("impact-oven.zip");
        assertThat(ovenManifest.rows()).isEqualTo(2).isEqualTo(ovenManifest.indexCount());
        assertThat(ovenManifest.sha256()).isEqualTo(sha256(openDataConfig.impactZipFile("oven")));
        assertThat(ovenManifest.columns()).filteredOn(column -> column.name().equals("warranty_year"))
                .singleElement().satisfies(column -> {
                    assertThat(column.fillRate()).isEqualTo(0.5);
                    assertThat(column.unit()).isEqualTo("Year(s)");
                    assertThat(column.description()).isEqualTo("Durée de garantie");
                });

        OpenDataManifest all = manifestStore.readAll(openDataConfig.manifestFile()).orElseThrow();
        assertThat(all.datasets()).extracting(DatasetManifest::id)
                .containsExactly("gtin", "isbn", "impact-oven", "impact-tv", "impact-all");
        assertThat(all.datasets().get(3).rows()).isEqualTo(1);

        // Global zip (GOU-2240): the CSV of every impact dataset, rows and sha256 in its own manifest entry.
        DatasetManifest global = all.datasets().get(4);
        assertThat(global.file()).isEqualTo("impact-all.zip");
        assertThat(global.rows()).isEqualTo(3);
        assertThat(global.sha256()).isEqualTo(sha256(openDataConfig.impactZipFile("all")));
        assertThat(manifestStore.readDataset(openDataConfig.impactManifestFile("all"))).isPresent();
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(openDataConfig.impactZipFile("all"))) {
            assertThat(java.util.Collections.list(zip.entries())).extracting(java.util.zip.ZipEntry::getName)
                    .containsExactly("impact-oven.csv", "impact-tv.csv");
        }

        // No aggregated score, no sub-score, no percentile, no rank, no DATA_QUALITY in any impact file.
        for (String vertical : List.of("oven", "tv")) {
            String header = readZipLines(openDataConfig.impactZipFile(vertical), "impact-" + vertical + ".csv").get(0)
                    .toLowerCase(Locale.ROOT);
            assertThat(header).doesNotContain("data_quality", "score", "ecoscore", "rank", "percentile", "ranking");
        }
    }

    @Test
    void impactDatasetsShouldNotContainDecitre() throws Exception {
        // GOU-2262: the impact files, their manifests and the pass projection never carry a Decitre key.
        when(verticalsConfigService.getConfigsWithoutDefault(true))
                .thenReturn(List.of(verticalConfig("oven", "CLASSE_ENERGY", "WARRANTY")));
        when(productRepository.countItemsByVertical("oven")).thenReturn(1L);
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        Product oven = impactProduct("1234567890123", "oven", "WARRANTY", "2", "eprel");
        for (String key : List.of("CLASSIFICATION DECITRE 1", "CLASSIFICATION DECITRE 2", "CLASSIFICATION DECITRE 3",
                "ATTRIBUTS", "LIVRES ORIGINE", "PROPRIETAIRE")) {
            addIndexedAttribute(oven.getAttributes(), key, "Decitre " + key);
            ProductAttribute attribute = new ProductAttribute();
            attribute.setName(key);
            attribute.setValue("decitre.fr");
            oven.getAttributes().getAll().put(key, attribute);
        }
        exportReturns(oven);

        assertThat(service.generateOpendata()).isTrue();

        List<String> produced = new ArrayList<>(readZipLines(openDataConfig.impactZipFile("oven"), "impact-oven.csv"));
        produced.add(Files.readString(openDataConfig.impactManifestFile("oven").toPath()));
        produced.add(Files.readString(openDataConfig.manifestFile().toPath()));
        produced.add(Files.readString(openDataConfig.impactManifestFile("all").toPath()));
        assertThat(produced).allSatisfy(content -> assertThat(content.toLowerCase(Locale.ROOT)).doesNotContain("decitre"));
        ArgumentCaptor<String[]> fields = ArgumentCaptor.forClass(String[].class);
        verify(productRepository).exportAllInVerticals(anyCollection(), fields.capture(), anyInt());
        assertThat(fields.getValue()).contains("attributes.indexed.WARRANTY.value", "attributes.indexed.WARRANTY.source.dataSourcename")
                .doesNotContain("attributes.indexed.WARRANTY")
                .noneMatch(field -> field.toLowerCase(Locale.ROOT).contains("decitre"))
                .noneMatch(field -> field.contains("ATTRIBUTS") || field.contains("LIVRES ORIGINE") || field.contains("PROPRIETAIRE"));
    }

    @Test
    void impactColumnNamesShouldCarryTheUnitAndStayUnique() {
        assertThat(ImpactDataset.slug("Year(s)")).isEqualTo("year");
        assertThat(ImpactDataset.slug("kWh/cycle")).isEqualTo("kwh_cycle");
        assertThat(ImpactDataset.slug("%")).isEqualTo("percent");
        assertThat(ImpactDataset.slug("°")).isEmpty();
    }

    @Test
    void impactDatasetBelowTheRowThresholdShouldNotBlockGtinIsbnAndOtherImpactDatasets() throws Exception {
        VerticalConfig oven = verticalConfig("oven", "WARRANTY");
        VerticalConfig tv = verticalConfig("tv", "WARRANTY");
        when(verticalsConfigService.getConfigsWithoutDefault(true)).thenReturn(List.of(oven, tv));
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        when(productRepository.countItemsByVertical("oven")).thenReturn(10L, 5L);
        when(productRepository.countItemsByVertical("tv")).thenReturn(1L, 7L);
        List<Product> first = new ArrayList<>(IntStream.range(0, 10)
                .mapToObj(i -> impactProduct("123456789012" + i, "oven", "WARRANTY", "2", "eprel")).toList());
        first.add(impactProduct("2234567890120", "tv", "WARRANTY", "3", "eprel"));
        exportReturns(first.toArray(Product[]::new));
        assertThat(service.generateOpendata()).isTrue();
        DatasetManifest ovenBefore = manifestStore.readDataset(openDataConfig.impactManifestFile("oven")).orElseThrow();
        String ovenZipSha = sha256(openDataConfig.impactZipFile("oven"));

        List<Product> second = new ArrayList<>(IntStream.range(0, 5)
                .mapToObj(i -> impactProduct("123456789012" + i, "oven", "WARRANTY", "2", "eprel")).toList());
        IntStream.range(0, 7).forEach(i -> second.add(impactProduct("223456789012" + i, "tv", "WARRANTY", "3", "eprel")));
        exportReturns(second.toArray(Product[]::new));

        assertThat(service.generateOpendata()).isTrue();

        // The refused impact dataset keeps its published version and stays listed, the others are refreshed.
        assertThat(sha256(openDataConfig.impactZipFile("oven"))).isEqualTo(ovenZipSha);
        assertThat(manifestStore.readDataset(openDataConfig.impactManifestFile("oven")).orElseThrow()).isEqualTo(ovenBefore);
        assertThat(manifestStore.readDataset(openDataConfig.impactManifestFile("tv")).orElseThrow().rows()).isEqualTo(7);
        // 11 GTIN rows in the first pass, 12 in the second: the GTIN file is really refreshed.
        assertThat(manifestStore.readDataset(openDataConfig.gtinManifestFile()).orElseThrow().rows()).isEqualTo(12);
        OpenDataManifest all = manifestStore.readAll(openDataConfig.manifestFile()).orElseThrow();
        assertThat(all.datasets()).extracting(DatasetManifest::id)
                .containsExactlyInAnyOrder("gtin", "isbn", "impact-tv", "impact-all", "impact-oven");
        assertThat(all.datasets()).filteredOn(d -> d.id().equals("impact-oven")).containsExactly(ovenBefore);
        assertThat(service.status().state()).isEqualTo(State.FAILED);
        assertThat(service.status().error()).contains("impact-oven").contains("Datasets not published");
        assertThat(service.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(meterRegistry.get("opendata.generation.runs").tag("outcome", "partial").counter().count()).isEqualTo(1);

        // GOU-2314: impact-all keeps the CSV still published for the refused dataset, and counts its carried rows.
        assertThat(impactAllEntries()).containsExactly("impact-oven.csv", "impact-tv.csv");
        assertThat(readZipLines(openDataConfig.impactZipFile("all"), "impact-oven.csv"))
                .hasSize(11).isEqualTo(readZipLines(openDataConfig.impactZipFile("oven"), "impact-oven.csv"));
        assertThat(readZipLines(openDataConfig.impactZipFile("all"), "impact-tv.csv")).hasSize(8);
        DatasetManifest global = manifestStore.readDataset(openDataConfig.impactManifestFile("all")).orElseThrow();
        assertThat(global.rows()).isEqualTo(17);
        assertThat(global.indexCount()).isEqualTo(17);
        assertThat(global.sha256()).isEqualTo(sha256(openDataConfig.impactZipFile("all")));
        assertThat(all.datasets()).filteredOn(d -> d.id().equals("impact-all")).containsExactly(global);
    }

    @Test
    void severalRefusedImpactDatasetsShouldAllStayInImpactAll() throws Exception {
        when(verticalsConfigService.getConfigsWithoutDefault(true)).thenReturn(List.of(verticalConfig("oven", "WARRANTY"),
                verticalConfig("tv", "WARRANTY"), verticalConfig("washer", "WARRANTY")));
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        when(productRepository.countItemsByVertical("oven")).thenReturn(10L, 5L);
        when(productRepository.countItemsByVertical("tv")).thenReturn(10L, 5L);
        when(productRepository.countItemsByVertical("washer")).thenReturn(1L, 2L);
        exportReturns(pass(10, 10, 1, 0));
        assertThat(service.generateOpendata()).isTrue();
        String tvZipSha = sha256(openDataConfig.impactZipFile("tv"));

        exportReturns(pass(5, 5, 2, 10));
        assertThat(service.generateOpendata()).isTrue();

        assertThat(service.status().error()).contains("impact-oven").contains("impact-tv").doesNotContain("impact-washer");
        assertThat(sha256(openDataConfig.impactZipFile("tv"))).isEqualTo(tvZipSha);
        assertThat(impactAllEntries()).containsExactly("impact-oven.csv", "impact-tv.csv", "impact-washer.csv");
        assertThat(readZipLines(openDataConfig.impactZipFile("all"), "impact-oven.csv")).hasSize(11);
        assertThat(readZipLines(openDataConfig.impactZipFile("all"), "impact-tv.csv")).hasSize(11);
        assertThat(readZipLines(openDataConfig.impactZipFile("all"), "impact-washer.csv")).hasSize(3);
        OpenDataManifest all = manifestStore.readAll(openDataConfig.manifestFile()).orElseThrow();
        assertThat(all.datasets()).extracting(DatasetManifest::id).containsExactlyInAnyOrder("gtin", "isbn",
                "impact-oven", "impact-tv", "impact-washer", "impact-all");
        assertThat(all.datasets()).filteredOn(d -> d.id().equals("impact-all")).singleElement()
                .satisfies(global -> assertThat(global.rows()).isEqualTo(22));
        assertThat(meterRegistry.get("opendata.generation.runs").tag("outcome", "partial").counter().count()).isEqualTo(1);
    }

    @Test
    void everyImpactDatasetRefusedShouldKeepImpactAllListed() throws Exception {
        when(verticalsConfigService.getConfigsWithoutDefault(true))
                .thenReturn(List.of(verticalConfig("oven", "WARRANTY"), verticalConfig("tv", "WARRANTY")));
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        when(productRepository.countItemsByVertical("oven")).thenReturn(10L, 5L);
        when(productRepository.countItemsByVertical("tv")).thenReturn(10L, 5L);
        exportReturns(pass(10, 10, 0, 0));
        assertThat(service.generateOpendata()).isTrue();
        DatasetManifest globalBefore = manifestStore.readDataset(openDataConfig.impactManifestFile("all")).orElseThrow();
        String globalZipSha = sha256(openDataConfig.impactZipFile("all"));

        exportReturns(pass(5, 5, 0, 10));
        assertThat(service.generateOpendata()).isTrue();

        assertThat(sha256(openDataConfig.impactZipFile("all"))).isEqualTo(globalZipSha);
        assertThat(manifestStore.readDataset(openDataConfig.impactManifestFile("all")).orElseThrow()).isEqualTo(globalBefore);
        OpenDataManifest all = manifestStore.readAll(openDataConfig.manifestFile()).orElseThrow();
        assertThat(all.datasets()).extracting(DatasetManifest::id)
                .containsExactlyInAnyOrder("gtin", "isbn", "impact-oven", "impact-tv", "impact-all");
        assertThat(all.datasets()).filteredOn(d -> d.id().equals("impact-all")).containsExactly(globalBefore);
        assertThat(service.status().error()).contains("impact-oven").contains("impact-tv");
    }

    @Test
    void refusedDatasetAfterAFailedPublicationShouldMatchWhatR2Serves() throws Exception {
        // Pass N-2 published on R2, pass N-1 refreshes oven locally but its publication fails, pass N refuses oven.
        openDataConfig.getPublication().setEnabled(true);
        InMemoryOpenDataStorage storage = new InMemoryOpenDataStorage();
        OpenDataPublisher publisher = new OpenDataPublisher(openDataConfig, meterRegistry, publication -> storage);
        OpenDataService publishing = new OpenDataService(productRepository, openDataConfig, meterRegistry, publisher,
                verticalsConfigService);
        when(verticalsConfigService.getConfigsWithoutDefault(true))
                .thenReturn(List.of(verticalConfig("oven", "WARRANTY"), verticalConfig("tv", "WARRANTY")));
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        when(productRepository.countItemsByVertical("oven")).thenReturn(10L, 11L, 5L);
        when(productRepository.countItemsByVertical("tv")).thenReturn(1L);

        exportReturns(pass(10, 1, 0, 0));
        assertThat(publishing.generateOpendata()).isTrue();
        assertThat(publisher.status().state()).isEqualTo(State.SUCCEEDED);

        storage.failUploadOf = ".staging/impact-tv.zip";
        exportReturns(pass(11, 1, 0, 0));
        assertThat(publishing.generateOpendata()).isTrue();
        assertThat(publisher.status().state()).isEqualTo(State.FAILED);
        DatasetManifest ovenLocal = manifestStore.readDataset(openDataConfig.impactManifestFile("oven")).orElseThrow();
        assertThat(ovenLocal.rows()).isEqualTo(11);
        assertThat(sha256(storage.objects.get("impact-oven.zip").content())).isNotEqualTo(ovenLocal.sha256());

        storage.failUploadOf = null;
        exportReturns(pass(5, 1, 0, 7));
        assertThat(publishing.generateOpendata()).isTrue();

        assertThat(publishing.status().error()).contains("impact-oven");
        assertThat(publisher.status().state()).isEqualTo(State.SUCCEEDED);
        Path onlineFile = tempDir.resolve("online-manifest.json");
        Files.write(onlineFile, storage.objects.get("manifest.json").content());
        OpenDataManifest online = manifestStore.readAll(onlineFile.toFile()).orElseThrow();
        for (DatasetManifest dataset : online.datasets()) {
            assertThat(sha256(storage.objects.get(dataset.file()).content())).as(dataset.id()).isEqualTo(dataset.sha256());
        }
        assertThat(online.datasets()).filteredOn(d -> d.id().equals("impact-oven")).containsExactly(ovenLocal);
    }

    @Test
    void failedGenerationShouldNotHideAFailedPublication() {
        openDataConfig.getPublication().setEnabled(true);
        OpenDataPublisher publisher = new OpenDataPublisher(openDataConfig, meterRegistry, publication -> {
            throw new IllegalStateException("R2 unreachable");
        });
        OpenDataService publishing = new OpenDataService(productRepository, openDataConfig, meterRegistry, publisher,
                verticalsConfigService);
        exportReturns(buildProduct("9781234567890", BarcodeType.ISBN_13), buildProduct("1234567890123", BarcodeType.GTIN_13));
        assertThat(publishing.generateOpendata()).isTrue();

        when(productRepository.exportAll(any(String[].class), anyInt())).thenThrow(new IllegalStateException("ES down"));
        assertThat(publishing.generateOpendata()).isFalse();

        assertThat(publishing.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(publishing.health().getDetails())
                .containsEntry("error", "IllegalStateException: ES down")
                .containsEntry("publicationError", "IllegalStateException: R2 unreachable");
    }

    @Test
    void carriedDatasetsShouldBeHandedToThePublisher() throws Exception {
        OpenDataPublisher publisher = Mockito.mock(OpenDataPublisher.class);
        when(publisher.isEnabled()).thenReturn(true);
        lenient().when(publisher.status()).thenReturn(OpenDataPublicationStatus.idle());
        OpenDataService publishing = new OpenDataService(productRepository, openDataConfig, meterRegistry, publisher,
                verticalsConfigService);
        when(verticalsConfigService.getConfigsWithoutDefault(true))
                .thenReturn(List.of(verticalConfig("oven", "WARRANTY"), verticalConfig("tv", "WARRANTY")));
        when(productRepository.countItemsByBarcodeType(any(BarcodeType[].class))).thenReturn(0L);
        when(productRepository.countItemsByVertical("oven")).thenReturn(10L, 5L);
        when(productRepository.countItemsByVertical("tv")).thenReturn(1L);
        exportReturns(pass(10, 1, 0, 0));
        assertThat(publishing.generateOpendata()).isTrue();
        DatasetManifest ovenBefore = manifestStore.readDataset(openDataConfig.impactManifestFile("oven")).orElseThrow();

        exportReturns(pass(5, 1, 0, 5));
        assertThat(publishing.generateOpendata()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OpenDataPublisher.Dataset>> carried = ArgumentCaptor.forClass(List.class);
        verify(publisher, Mockito.times(2)).publish(any(), carried.capture(), any());
        assertThat(carried.getAllValues().get(0)).isEmpty();
        assertThat(carried.getValue()).singleElement().satisfies(dataset -> {
            assertThat(dataset.manifest()).isEqualTo(ovenBefore);
            assertThat(dataset.zipFile()).isEqualTo(openDataConfig.impactZipFile("oven").toPath());
            assertThat(dataset.manifestFile()).isEqualTo(openDataConfig.impactManifestFile("oven").toPath());
        });
    }

    /** Products of a pass: {@code oven}, {@code tv} and {@code washer} impact products, plus GTIN-only fillers. */
    private Product[] pass(int oven, int tv, int washer, int fillers) {
        List<Product> products = new ArrayList<>();
        IntStream.range(0, oven).forEach(i -> products.add(impactProduct(gtin(2, i), "oven", "WARRANTY", "2", "eprel")));
        IntStream.range(0, tv).forEach(i -> products.add(impactProduct(gtin(3, i), "tv", "WARRANTY", "3", "eprel")));
        IntStream.range(0, washer).forEach(i -> products.add(impactProduct(gtin(4, i), "washer", "WARRANTY", "1", "eprel")));
        IntStream.range(0, fillers).forEach(i -> products.add(buildProduct(gtin(5, i), BarcodeType.GTIN_13)));
        return products.toArray(Product[]::new);
    }

    private static String gtin(int prefix, int index) {
        return String.format(Locale.ROOT, "%d%012d", prefix, index);
    }

    private List<String> impactAllEntries() throws Exception {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(openDataConfig.impactZipFile("all"))) {
            return java.util.Collections.list(zip.entries()).stream().map(ZipEntry::getName).toList();
        }
    }

    @Test
    void impactNameShouldSkipCorruptedOfferNames() {
        Product product = impactProduct("1234567890123", "oven", "WARRANTY", "2", "eprel");
        product.setOfferNames(new HashSet<>(Set.of("P\u00BF\u00BAcheur", "bad \uFFFD", "Chapeau de p\u00EAcheur")));
        ImpactDataset impact = new ImpactDataset(verticalConfig("oven", "WARRANTY"), "https://nudger.fr");
        assertThat(impact.toRow(product)[3]).isEqualTo("Chapeau de p\u00EAcheur");

        product.setOfferNames(new HashSet<>(Set.of("V\u00BF\u00BFlo")));
        assertThat(impact.toRow(product)[3]).isEmpty();
    }

    @Test
    void impactSourceShouldBeEmptyWhenNoSourceCarriesThePublishedValue() {
        Product product = impactProduct("1234567890123", "oven", "WARRANTY", "2", "eprel");
        SourcedAttribute other = new SourcedAttribute();
        other.setDataSourcename("icecat.biz");
        other.setValue("5");
        product.getAttributes().getIndexed().get("WARRANTY").getSource().add(other);
        ImpactDataset impact = new ImpactDataset(verticalConfig("oven", "WARRANTY"), "https://nudger.fr");
        // Only the source with the published value is listed, never all the contributors.
        assertThat(impact.toRow(product)[10]).isEqualTo("eprel");

        product.getAttributes().getIndexed().get("WARRANTY").setValue("9");
        String[] row = impact.toRow(product);
        assertThat(row[9]).isEqualTo("9");
        assertThat(row[10]).isEmpty();
    }

    private VerticalConfig verticalConfig(String id, String... criteria) {
        VerticalConfig config = new VerticalConfig();
        config.setId(id);
        config.setAvailableImpactScoreCriterias(new ArrayList<>(List.of(criteria)));
        List<AttributeConfig> attributes = new ArrayList<>();
        AttributeConfig warranty = new AttributeConfig();
        warranty.setKey("WARRANTY");
        Localisable<String, String> suffix = new Localisable<>();
        suffix.put("default", "Year(s)");
        warranty.setSuffix(suffix);
        Localisable<String, String> name = new Localisable<>();
        name.put("fr", "Durée de garantie");
        warranty.setName(name);
        attributes.add(warranty);
        config.setAttributesConfig(new AttributesConfig(attributes));
        return config;
    }

    private Product impactProduct(String gtin, String vertical, String key, String value, String datasource) {
        Product product = buildProduct(gtin, BarcodeType.GTIN_13);
        product.setVertical(vertical);
        Localisable<String, String> url = new Localisable<>();
        url.put("fr", "slug-" + gtin);
        product.getNames().setUrl(url);
        if (key != null) {
            IndexedAttribute attribute = new IndexedAttribute(key, value);
            SourcedAttribute source = new SourcedAttribute();
            source.setDataSourcename(datasource);
            source.setValue(value);
            attribute.getSource().add(source);
            product.getAttributes().getIndexed().put(key, attribute);
        }
        return product;
    }

    private void assertFailureVisible(String error) {
        assertThat(service.status().state()).isEqualTo(State.FAILED);
        assertThat(service.status().error()).contains(error);
        assertThat(service.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(service.health().getDetails()).containsEntry("state", "FAILED");
        assertThat(meterRegistry.get("opendata.generation.runs").tag("outcome", "failure").counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("opendata.generation.failed").gauge().value()).isEqualTo(1);
    }

    /**
     * Main pass: every product. Impact pass: the products of the requested verticals, as the {@code terms} query does.
     */
    private void exportReturns(Product... products) {
        when(productRepository.exportAll(any(String[].class), anyInt())).thenReturn(Stream.of(products));
        lenient().when(productRepository.exportAllInVerticals(anyCollection(), any(String[].class), anyInt()))
                .thenAnswer(invocation -> {
                    Collection<String> verticals = invocation.getArgument(0);
                    return Stream.of(products).filter(product -> verticals.contains(product.getVertical()));
                });
    }

    private Snapshot snapshot() throws Exception {
        return new Snapshot(read(openDataConfig.gtinZipFile()), read(openDataConfig.isbnZipFile()),
                read(openDataConfig.gtinManifestFile()), read(openDataConfig.isbnManifestFile()),
                read(openDataConfig.manifestFile()));
    }

    private static String read(File file) throws Exception {
        return file.exists() ? HexFormat.of().formatHex(Files.readAllBytes(file.toPath())) : null;
    }

    private record Snapshot(String gtinZip, String isbnZip, String gtinManifest, String isbnManifest, String manifest) {
    }

    private static String sha256(File file) throws Exception {
        return sha256(Files.readAllBytes(file.toPath()));
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
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
        product.setSources(new java.util.TreeSet<>(Set.of("rakuten.com", "eprel")));

        ProductAttributes attributes = new ProductAttributes();
        attributes.addReferentielAttribute(ReferentielKey.BRAND, "TestBrand");
        attributes.addReferentielAttribute(ReferentielKey.MODEL, "ModelX");
        addIndexedAttribute(attributes, "EDITEUR", "Test Editor");
        addIndexedAttribute(attributes, "FORMAT", "Broché");
        addIndexedAttribute(attributes, "NB DE PAGES", "320");
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
