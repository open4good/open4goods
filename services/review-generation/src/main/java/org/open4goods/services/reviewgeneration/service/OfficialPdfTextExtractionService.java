package org.open4goods.services.reviewgeneration.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Extracts text from official product PDFs for limited review-source fallback.
 */
@Service
public class OfficialPdfTextExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(OfficialPdfTextExtractionService.class);
    private static final int MAX_PDF_BYTES = 12 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Fetches a PDF and returns extracted plain text when parsing succeeds.
     *
     * @param url PDF URL
     * @return extracted text, or empty when the PDF cannot be fetched or parsed
     */
    public Optional<String> extractText(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/pdf,*/*")
                    .header("User-Agent", "open4goods-review-generation/1.0")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.info("Official PDF fetch rejected for {}: status={}", url, response.statusCode());
                return Optional.empty();
            }
            byte[] bytes = response.body().readNBytes(MAX_PDF_BYTES + 1);
            if (bytes.length > MAX_PDF_BYTES) {
                logger.info("Official PDF fetch rejected for {}: file exceeds {} bytes", url, MAX_PDF_BYTES);
                return Optional.empty();
            }
            try (PDDocument document = Loader.loadPDF(bytes)) {
                String text = new PDFTextStripper().getText(document);
                return text == null || text.isBlank() ? Optional.empty() : Optional.of(text);
            }
        } catch (IOException e) {
            logger.info("Official PDF extraction failed for {}: {}", url, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Official PDF extraction interrupted for {}", url);
            return Optional.empty();
        } catch (RuntimeException e) {
            logger.info("Official PDF extraction rejected for {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }
}
