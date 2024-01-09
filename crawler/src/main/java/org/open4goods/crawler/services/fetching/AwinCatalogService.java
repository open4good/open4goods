//package org.open4goods.crawler.services.fetching;
//
//import java.io.File;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//import org.apache.commons.io.FileUtils;
//import org.open4goods.crawler.model.AwinCatalogEntry;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.scheduling.annotation.Scheduled;
//
//import com.fasterxml.jackson.databind.MappingIterator;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.ObjectReader;
//import com.fasterxml.jackson.dataformat.csv.CsvMapper;
//import com.fasterxml.jackson.dataformat.csv.CsvParser;
//import com.fasterxml.jackson.dataformat.csv.CsvSchema;
//
//public class AwinCatalogService {
//
//	private final ObjectMapper csvMapper = new CsvMapper().enable((CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE));
//
//	private static final Logger logger = LoggerFactory.getLogger(AwinCatalogService.class);
//
//	private String catalogUrl;
//
//	private Map<String, List<AwinCatalogEntry>> entries = new ConcurrentHashMap<>();
//
//	public AwinCatalogService(String catalogUrl) {
//		this.catalogUrl = catalogUrl;
//	}
//
//	@Scheduled(initialDelay = 0, fixedRate = 1000 * 3600 * 24)
//	public void loadCatalog() {
////		ISSUE : Test that issue
//		try {
//			logger.info("Loading CSV catalog");
//
//			CsvSchema schema;
//
//			schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(',')
////					.withEscapeChar(SANITIZED_ESCAPE_CHAR)
//					.withQuoteChar('"');
//
//			final ObjectReader oReader = csvMapper.readerFor(AwinCatalogEntry.class).with(schema);
//
//			File destFile = File.createTempFile("csv", "awincatalog.csv");
//			FileUtils.copyURLToFile(new URL(catalogUrl), destFile);
//			logger.info("Downloading awin CSV catalog from {} to {}", catalogUrl, destFile);
//
//			final MappingIterator<AwinCatalogEntry> mi = oReader.readValues(destFile);
//
//			List<AwinCatalogEntry> catalog = mi.readAll();
//
//			entries.clear();
//
//			for (AwinCatalogEntry awinCatalogEntry : catalog.stream().filter(e -> e.getStatus().equals("active"))
//					.toList()) {
//				String key = awinCatalogEntry.getAdvertiserName();
//				entries.computeIfAbsent(key, k -> new ArrayList<>()).add(awinCatalogEntry);
//			}
//
//
//			logger.info("Loaded {} entries", catalog.size());
//
//
//		} catch (Exception e) {
//			logger.error("Error handling awin catalog at {}", catalogUrl, e);
//		}
//	}
//
//	public List<AwinCatalogEntry> getEntriesFor(String provider) {
//		return entries.get(provider);
//	}
//
//	public Map<String, List<AwinCatalogEntry>> getEntries() {
//		return entries;
//	}
//
//	public void setEntries(Map<String, List<AwinCatalogEntry>> entries) {
//		this.entries = entries;
//	}
//
//}
