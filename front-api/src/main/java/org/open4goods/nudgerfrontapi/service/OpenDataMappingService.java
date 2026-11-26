package org.open4goods.nudgerfrontapi.service;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataColumnDto;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataDatasetDto;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataOverviewDto;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestService;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestSnapshot;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataSampleService;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.ColumnManifest;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.service.OpenDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service assembling OpenData DTOs for the frontend REST API.
 * <p>
 * Rows, date, size and sha256 come from the published {@code manifest.json} ({@link OpenDataManifestService},
 * GOU-2290), never from the live index: the index counts differ from the published files (ISBN: 10.1 M in the index,
 * 8.7 M in the file on 15/11/2026). Without a readable manifest those fields are null; the endpoints never fail.
 * </p>
 */
@Service
public class OpenDataMappingService {

    private static final String[] SIZE_UNITS = {"B", "kB", "MB", "GB", "TB", "PB", "EB"};

    /** Zone of the displayed dates, independent of the host's default zone. */
    private static final String IMPACT_PREFIX = "impact-";
    private static final String IMPACT_ALL = "impact-all";

    static final ZoneId DISPLAY_ZONE = ZoneId.of("Europe/Paris");

    private final OpenDataManifestService manifestService;
    private final OpenDataConfig openDataConfig;
    private final OpenDataSampleService sampleService;

    /**
     * @param manifestService reader of the published manifest
     * @param openDataConfig  public base URL and public file names
     * @param sampleService   reader of the published CSV samples
     */
    public OpenDataMappingService(OpenDataManifestService manifestService, OpenDataConfig openDataConfig,
                                  OpenDataSampleService sampleService) {
        this.manifestService = manifestService;
        this.openDataConfig = openDataConfig;
        this.sampleService = sampleService;
    }

    /**
     * Build an overview of the GTIN and ISBN datasets (impact datasets excluded: same products).
     *
     * @param domainLanguage caller requested language
     * @return overview DTO with localised values, every field null without a readable manifest
     */
    public OpenDataOverviewDto overview(DomainLanguage domainLanguage) {
        Locale locale = resolveLocale(domainLanguage);
        OpenDataManifestSnapshot snapshot = manifestService.snapshot();
        List<DatasetManifest> datasets = Stream.of(OpenDataManifestService.GTIN, OpenDataManifestService.ISBN)
                .map(snapshot::dataset)
                .flatMap(Optional::stream)
                .toList();
        if (datasets.isEmpty()) {
            return new OpenDataOverviewDto(null, null, null);
        }

        long rows = datasets.stream().mapToLong(DatasetManifest::rows).sum();
        long bytes = datasets.stream().mapToLong(DatasetManifest::bytes).sum();
        return new OpenDataOverviewDto(
                formatInteger(rows, locale),
                formatInteger(datasets.size(), locale),
                bytes > 0 ? formatDataSize(bytes, locale) : null);
    }

    /**
     * Build DTO for the GTIN dataset.
     *
     * @param domainLanguage caller requested language
     * @return dataset DTO with localised values
     */
    public OpenDataDatasetDto gtin(DomainLanguage domainLanguage) {
        return dataset(domainLanguage,
                "GTIN",
                OpenDataManifestService.GTIN,
                openDataConfig.getGtinPublicFilename(),
                List.of(OpenDataService.GTIN_HEADER));
    }

    /**
     * Build DTO for the ISBN dataset.
     *
     * @param domainLanguage caller requested language
     * @return dataset DTO with localised values
     */
    public OpenDataDatasetDto isbn(DomainLanguage domainLanguage) {
        return dataset(domainLanguage,
                "ISBN",
                OpenDataManifestService.ISBN,
                openDataConfig.getIsbnPublicFilename(),
                List.of(OpenDataService.ISBN_HEADER));
    }

    /**
     * Build the description of every impact dataset listed in the manifest (GOU-2251), without the global
     * {@code impact-all} archive.
     *
     * @param domainLanguage caller requested language
     * @return impact datasets in manifest order, empty without a readable manifest
     */
    public List<OpenDataDatasetDto> impactDatasets(DomainLanguage domainLanguage) {
        OpenDataManifestSnapshot snapshot = manifestService.snapshot();
        if (snapshot.manifest() == null || snapshot.manifest().datasets() == null) {
            return List.of();
        }
        return snapshot.manifest().datasets().stream()
                .filter(Objects::nonNull)
                .filter(entry -> isImpactVertical(entry.id()))
                .map(entry -> impact(domainLanguage, entry))
                .toList();
    }

    /**
     * Build the description of the impact dataset of one vertical.
     *
     * @param domainLanguage caller requested language
     * @param vertical       vertical identifier ({@code dishwasher}), without the {@code impact-} prefix
     * @return the dataset, empty when the manifest has no such impact dataset
     */
    public Optional<OpenDataDatasetDto> impactDataset(DomainLanguage domainLanguage, String vertical) {
        return manifestService.snapshot().dataset(IMPACT_PREFIX + vertical)
                .filter(entry -> isImpactVertical(entry.id()))
                .map(entry -> impact(domainLanguage, entry));
    }

    private static boolean isImpactVertical(String id) {
        return id != null && id.startsWith(IMPACT_PREFIX) && !IMPACT_ALL.equals(id);
    }

    private OpenDataDatasetDto impact(DomainLanguage domainLanguage, DatasetManifest entry) {
        List<String> headers = entry.columns() == null ? List.of()
                : entry.columns().stream().map(ColumnManifest::name).toList();
        return dataset(domainLanguage, "IMPACT", entry.id(), entry.file(), headers);
    }

    /**
     * Resolve the {@link Locale} matching a {@link DomainLanguage}.
     *
     * @param domainLanguage requested language
     * @return resolved locale (defaults to system locale when null)
     */
    public Locale resolveLocale(DomainLanguage domainLanguage) {
        if (domainLanguage == null) {
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(domainLanguage.languageTag());
    }

    /**
     * Return the resolved locale tag.
     *
     * @param domainLanguage requested language
     * @return locale expressed as IETF BCP 47 tag
     */
    public String resolvedLocaleTag(DomainLanguage domainLanguage) {
        return resolveLocale(domainLanguage).toLanguageTag();
    }

    /**
     * Build a dataset description from its manifest entry.
     *
     * @param domainLanguage caller requested language
     * @param type           dataset name exposed to the frontend
     * @param manifestId     identifier of the dataset in the manifest
     * @param publicFilename public object name of the zip
     * @param headers        CSV headers used to describe the export format
     * @return assembled dataset DTO, manifest values null when the entry is unknown
     */
    private OpenDataDatasetDto dataset(DomainLanguage domainLanguage,
                                       String type,
                                       String manifestId,
                                       String publicFilename,
                                       List<String> headers) {
        Locale locale = resolveLocale(domainLanguage);
        Optional<DatasetManifest> entry = manifestService.snapshot().dataset(manifestId);
        return new OpenDataDatasetDto(
                type,
                entry.map(dataset -> formatInteger(dataset.rows(), locale)).orElse(null),
                entry.map(DatasetManifest::rows).orElse(null),
                entry.flatMap(OpenDataManifestSnapshot::generatedAt).map(at -> formatTimestamp(at, locale)).orElse(null),
                entry.map(DatasetManifest::bytes).filter(bytes -> bytes > 0)
                        .map(bytes -> formatDataSize(bytes, locale)).orElse(null),
                entry.map(DatasetManifest::sha256).filter(StringUtils::hasText).orElse(null),
                buildDownloadUrl(publicFilename),
                List.copyOf(headers),
                entry.map(DatasetManifest::id).orElse(null),
                entry.flatMap(OpenDataManifestSnapshot::generatedAt).map(Instant::toString).orElse(null),
                entry.map(DatasetManifest::bytes).filter(bytes -> bytes > 0).orElse(null),
                entry.map(DatasetManifest::license).filter(StringUtils::hasText).orElse(null),
                entry.map(DatasetManifest::columns).map(columns -> columns.stream()
                        .filter(Objects::nonNull)
                        .map(column -> new OpenDataColumnDto(column.name(), column.fillRate(), column.unit(),
                                column.description(), column.source()))
                        .toList()).orElse(List.of()),
                sampleService.rows(entry)
        );
    }

    /**
     * Format an integer according to the requested locale.
     *
     * @param value   number to format
     * @param locale  locale used for formatting
     * @return localised representation
     */
    private String formatInteger(long value, Locale locale) {
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(locale);
        return numberFormat.format(value);
    }

    /**
     * Format a positive byte size using SI units.
     *
     * @param bytes  raw size in bytes, greater than zero (an unknown size is null, never "0 B")
     * @param locale locale used for formatting
     * @return localised human readable size
     */
    private String formatDataSize(long bytes, Locale locale) {
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1000 && unitIndex < SIZE_UNITS.length - 1) {
            value /= 1000;
            unitIndex++;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        if (unitIndex == 0) {
            numberFormat.setMaximumFractionDigits(0);
        } else if (value < 10) {
            numberFormat.setMaximumFractionDigits(1);
            numberFormat.setMinimumFractionDigits(1);
        } else {
            numberFormat.setMaximumFractionDigits(0);
        }

        return numberFormat.format(value) + " " + SIZE_UNITS[unitIndex];
    }

    /**
     * Format the generation date of a dataset in the {@link #DISPLAY_ZONE}.
     *
     * @param instant generation date from the manifest
     * @param locale  locale used for formatting
     * @return formatted timestamp
     */
    private String formatTimestamp(Instant instant, Locale locale) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(DISPLAY_ZONE);
        return formatter.format(instant);
    }

    /**
     * Build the public download URL of a zip: {@code {open-data-config.public-base-url}/{name}}.
     *
     * @param publicFilename public object name of the zip
     * @return absolute URL, {@code null} when no public base URL is configured (ui no longer serves the files)
     */
    private String buildDownloadUrl(String publicFilename) {
        String publicBase = openDataConfig.getPublicBaseUrl();
        if (!StringUtils.hasText(publicBase)) {
            return null;
        }
        String base = publicBase.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + publicFilename;
    }
}
