package org.open4goods.nudgerfrontapi.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.open4goods.commons.helper.ThrottlingInputStream;
import org.open4goods.model.exceptions.TechnicalException;
import org.open4goods.model.product.BarcodeType;
import org.open4goods.nudgerfrontapi.config.OpenDataProperties;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Service exposing dataset metadata and file streaming with throttling.
 */
@Service
public class OpenDataService {

    private final ProductRepository repository;
    private final OpenDataProperties properties;
    private final AtomicInteger downloads = new AtomicInteger();

    public OpenDataService(ProductRepository repository, OpenDataProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Create a throttled stream for the given file. */
    private InputStream limitedRateStream(File file) throws FileNotFoundException, TechnicalException {
        if (downloads.get() >= properties.getConcurrentDownloads()) {
            throw new TechnicalException("Too many requests");
        }
        downloads.incrementAndGet();
        RateLimiter limiter = RateLimiter.create(properties.getDownloadSpeedKb() * 1024.0);
        return new ThrottlingInputStream(new BufferedInputStream(new FileInputStream(file)), limiter) {
            @Override
            public void close() throws java.io.IOException {
                super.close();
                downloads.decrementAndGet();
            }
        };
    }

    public Resource gtinResource() throws TechnicalException, FileNotFoundException {
        return new InputStreamResource(limitedRateStream(properties.gtinZipFile()));
    }

    public Resource isbnResource() throws TechnicalException, FileNotFoundException {
        return new InputStreamResource(limitedRateStream(properties.isbnZipFile()));
    }

    public long countGtin() {
        return repository.countItemsByBarcodeType(BarcodeType.GTIN_8, BarcodeType.GTIN_12,
                BarcodeType.GTIN_13, BarcodeType.GTIN_14);
    }

    public long countIsbn() {
        return repository.countItemsByBarcodeType(BarcodeType.ISBN_13);
    }

    public String gtinFileSize() {
        return humanReadableByteCountBin(properties.gtinZipFile().length());
    }

    public String isbnFileSize() {
        return humanReadableByteCountBin(properties.isbnZipFile().length());
    }

    public Date gtinLastUpdated() {
        return Date.from(Instant.ofEpochMilli(properties.gtinZipFile().lastModified()));
    }

    public Date isbnLastUpdated() {
        return Date.from(Instant.ofEpochMilli(properties.isbnZipFile().lastModified()));
    }

    /** Convert bytes to human readable string. */
    public static String humanReadableByteCountBin(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        String units = "kMGTPE";
        int unitIndex = 0;
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            unitIndex++;
        }
        return String.format("%.1f %cB", bytes / 1000.0, units.charAt(unitIndex));
    }
}
