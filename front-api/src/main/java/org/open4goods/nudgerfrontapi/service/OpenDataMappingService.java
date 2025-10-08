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
                formatTimestamp(file, locale),
                formatDataSize(safeLength(file), locale),
                buildDownloadUrl(relativeDownloadPath),
                List.copyOf(headers)
        );
    }

    private long safeLength(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        return file.length();
    }

    private String formatInteger(long value, Locale locale) {
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(locale);
        return numberFormat.format(value);
    }

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

    private String formatDownloadSpeed(int downloadSpeedKb, Locale locale) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat.format(downloadSpeedKb) + " kB/s";
    }

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
