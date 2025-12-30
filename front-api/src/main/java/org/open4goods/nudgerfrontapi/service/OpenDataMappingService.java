package org.open4goods.nudgerfrontapi.service;

import java.io.File;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataDatasetDto;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataDownloadLimitsDto;
import org.open4goods.nudgerfrontapi.dto.opendata.OpenDataOverviewDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.service.OpenDataService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service assembling OpenData DTOs for the frontend REST API.
 * <p>
 * The service translates raw data statistics into localised DTOs so controllers can remain thin and deterministic.
 * </p>
 */
@Service
public class OpenDataMappingService {

    private static final String[] SIZE_UNITS = {"B", "kB", "MB", "GB", "TB", "PB", "EB"};

    private final OpenDataService openDataService;
    private final OpenDataConfig openDataConfig;
    private final ApiProperties apiProperties;

    public OpenDataMappingService(OpenDataService openDataService,
                                OpenDataConfig openDataConfig,
                                ApiProperties apiProperties) {
        this.openDataService = openDataService;
        this.openDataConfig = openDataConfig;
        this.apiProperties = apiProperties;
    }

    /**
     * Build an overview of the available datasets.
     *
     * @param domainLanguage caller requested language
     * @return overview DTO with localised values
     */
    public OpenDataOverviewDto overview(DomainLanguage domainLanguage) {
        Locale locale = resolveLocale(domainLanguage);

        long isbnCount = openDataService.totalItemsISBN();
        long gtinCount = openDataService.totalItemsGTIN();
        long totalProducts = isbnCount + gtinCount;
        int datasetCount = 2; // GTIN + ISBN datasets
        long totalSize = safeLength(openDataConfig.isbnZipFile()) + safeLength(openDataConfig.gtinZipFile());

        OpenDataDownloadLimitsDto limits = new OpenDataDownloadLimitsDto(
                openDataConfig.getDownloadSpeedKb(),
                formatDownloadSpeed(openDataConfig.getDownloadSpeedKb(), locale),
                openDataConfig.getConcurrentDownloads());

        return new OpenDataOverviewDto(
                formatInteger(totalProducts, locale),
                formatInteger(datasetCount, locale),
                formatDataSize(totalSize, locale),
                limits
        );
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
                openDataService.totalItemsGTIN(),
                openDataConfig.gtinZipFile(),
                "/opendata/gtin-open-data.zip",
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
                openDataService.totalItemsISBN(),
                openDataConfig.isbnZipFile(),
                "/opendata/isbn-open-data.zip",
                List.of(OpenDataService.ISBN_HEADER));
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
     * Build a dataset description for the specified OpenData export.
     *
     * @param domainLanguage caller requested language
     * @param type human readable dataset name
     * @param recordCount number of records advertised by the export
     * @param file physical file reference used to determine size and timestamp
     * @param relativeDownloadPath path exposed by the static assets application
     * @param headers CSV headers used to describe the export format
     * @return assembled dataset DTO
     */
    private OpenDataDatasetDto dataset(DomainLanguage domainLanguage,
                                       String type,
                                       long recordCount,
                                       File file,
                                       String relativeDownloadPath,
                                       List<String> headers) {
        Locale locale = resolveLocale(domainLanguage);
        return new OpenDataDatasetDto(
                type,
                formatInteger(recordCount, locale),
                recordCount,
                formatTimestamp(file, locale),
                formatDataSize(safeLength(file), locale),
                buildDownloadUrl(relativeDownloadPath),
                List.copyOf(headers)
        );
    }

    /**
     * Return the file size while gracefully handling missing files.
     *
     * @param file target file
     * @return file size in bytes, {@code 0} when the file is missing
     */
    private long safeLength(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        return file.length();
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
     * Format a raw byte size using SI units.
     *
     * @param bytes  raw size in bytes
     * @param locale locale used for formatting
     * @return localised human readable size
     */
    private String formatDataSize(long bytes, Locale locale) {
        if (bytes <= 0) {
            return NumberFormat.getIntegerInstance(locale).format(0) + " " + SIZE_UNITS[0];
        }

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
     * Format the last modification timestamp of a dataset.
     *
     * @param file   dataset file
     * @param locale locale used for formatting
     * @return formatted timestamp or {@code null} when the file is missing
     */
    private String formatTimestamp(File file, Locale locale) {
        if (file == null || !file.exists()) {
            return null;
        }
        Instant instant = Instant.ofEpochMilli(file.lastModified());
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale);
        return formatter.format(dateTime);
    }

    /**
     * Format the download speed limit defined in the configuration.
     *
     * @param downloadSpeedKb speed expressed in kilobytes per second
     * @param locale          locale used for formatting
     * @return human readable download speed
     */
    private String formatDownloadSpeed(int downloadSpeedKb, Locale locale) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat.format(downloadSpeedKb) + " kB/s";
    }

    /**
     * Construct an absolute download URL by combining the configured static asset root and the provided path.
     *
     * @param relativePath path exposed by the static assets application
     * @return absolute URL accessible by the frontend
     */
    private String buildDownloadUrl(String relativePath) {
        String root = apiProperties.getResourceRootPath();

        if (!StringUtils.hasText(root)) {
            return relativePath;
        }

        String normalisedRoot = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
        String normalisedPath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return normalisedRoot + normalisedPath;
    }
}
