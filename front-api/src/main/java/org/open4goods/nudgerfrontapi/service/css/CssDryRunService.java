package org.open4goods.nudgerfrontapi.service.css;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.open4goods.nudgerfrontapi.service.MerchantDomainsService;
import org.open4goods.nudgerfrontapi.service.MerchantDomainsService.MerchantDomain;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.protobuf.util.JsonFormat;
import com.google.shopping.css.v1.CssProductInput;

/**
 * CSS dry run (GOU-2080): evaluates every product of every enabled vertical and writes, in
 * {@code front.css.output-directory}, one NDJSON file per vertical (one {@link CssProductInput} per line, protobuf
 * {@code JsonFormat}) and the report ({@code report.json}, {@code report.md}).
 * <p>
 * Nothing is sent to Google: no CSS client is created.
 * </p>
 */
@Service
@ConditionalOnProperty(prefix = "front.css", name = "enabled", havingValue = "true")
public class CssDryRunService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CssDryRunService.class);

    private final ProductRepository productRepository;
    private final VerticalsConfigService verticalsConfigService;
    private final MerchantDomainsService merchantDomainsService;
    private final CssEligibilityService eligibilityService;
    private final CssProductInputMapper mapper;
    private final CssProperties properties;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<CssDryRunReport> last = new AtomicReference<>();

    /**
     * @param productRepository      product index
     * @param verticalsConfigService vertical configurations
     * @param merchantDomainsService merchant domain rules
     * @param eligibilityService     eligibility rules
     * @param mapper                 product to CssProductInput mapping
     * @param properties             CSS settings
     */
    public CssDryRunService(ProductRepository productRepository, VerticalsConfigService verticalsConfigService,
            MerchantDomainsService merchantDomainsService, CssEligibilityService eligibilityService,
            CssProductInputMapper mapper, CssProperties properties) {
        this(productRepository, verticalsConfigService, merchantDomainsService, eligibilityService, mapper,
                properties, Clock.systemUTC());
    }

    CssDryRunService(ProductRepository productRepository, VerticalsConfigService verticalsConfigService,
            MerchantDomainsService merchantDomainsService, CssEligibilityService eligibilityService,
            CssProductInputMapper mapper, CssProperties properties, Clock clock) {
        this.productRepository = productRepository;
        this.verticalsConfigService = verticalsConfigService;
        this.merchantDomainsService = merchantDomainsService;
        this.eligibilityService = eligibilityService;
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * @return true while a run is in progress
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * @return the report of the last completed run, or null when none since startup
     */
    public CssDryRunReport lastReport() {
        return last.get();
    }

    /**
     * Starts a run in a dedicated thread.
     *
     * @return false when a run is already in progress
     */
    public boolean start() {
        if (!running.compareAndSet(false, true)) {
            return false;
        }
        Thread thread = new Thread(() -> {
            try {
                run();
            } catch (Exception e) {
                LOGGER.error("CSS dry run failed", e);
            } finally {
                running.set(false);
            }
        }, "css-dry-run");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    /**
     * Runs synchronously: files are written, then the report is kept as the last one.
     *
     * @return the report
     * @throws IOException when the output directory cannot be written
     */
    public CssDryRunReport run() throws IOException {
        Path directory = Path.of(properties.getOutputDirectory());
        Files.createDirectories(directory);
        Function<String, MerchantDomain> resolver = merchantDomainsService.domainResolver();
        Map<String, CssDryRunReport.VerticalReport> verticals = new TreeMap<>();
        for (VerticalConfig vertical : verticalsConfigService.getConfigsWithoutDefault(true)) {
            verticals.put(vertical.getId(), runVertical(vertical, resolver, directory));
        }
        CssDryRunReport report = new CssDryRunReport(clock.instant().toString(), verticals);
        ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        json.writeValue(directory.resolve("report.json").toFile(), report);
        Files.writeString(directory.resolve("report.md"), report.toMarkdown(), StandardCharsets.UTF_8);
        last.set(report);
        LOGGER.info("CSS dry run: {} eligible / {} examined, files in {}", report.eligible(), report.examined(), directory);
        return report;
    }

    private CssDryRunReport.VerticalReport runVertical(VerticalConfig vertical,
            Function<String, MerchantDomain> resolver, Path directory) throws IOException {
        long examined = 0;
        long eligible = 0;
        Map<CssExclusionReason, Long> exclusions = new EnumMap<>(CssExclusionReason.class);
        JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
        try (BufferedWriter out = Files.newBufferedWriter(directory.resolve(vertical.getId() + ".ndjson"),
                StandardCharsets.UTF_8); Stream<Product> products = productRepository.exportAll(vertical.getId())) {
            for (var it = products.iterator(); it.hasNext();) {
                Product product = it.next();
                examined++;
                CssEligibility result = eligibilityService.evaluate(product, vertical, resolver);
                if (result.eligible()) {
                    out.write(printer.print(mapper.map(product, vertical)));
                    out.newLine();
                    eligible++;
                } else {
                    result.reasons().forEach(r -> exclusions.merge(r, 1L, Long::sum));
                }
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return new CssDryRunReport.VerticalReport(examined, eligible, exclusions);
    }
}
