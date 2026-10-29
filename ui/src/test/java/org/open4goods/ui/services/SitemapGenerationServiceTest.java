package org.open4goods.ui.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.model.Localisable;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCanonicalUrl;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.SiteNaming;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class SitemapGenerationServiceTest {

	private static final String BASE_URL = "https://nudger.fr/";

	@TempDir
	Path rootFolder;

	private ProductRepository repository;
	private VerticalsConfigService verticalsConfigService;
	private SimpleMeterRegistry meterRegistry;
	private SitemapGenerationService service;
	private VerticalConfig tv;

	@BeforeEach
	void setUp() {
		UiConfig uiConfig = mock(UiConfig.class);
		when(uiConfig.siteMapFolder()).thenReturn(rootFolder.resolve("sitemap").toFile());
		when(uiConfig.logsFolder()).thenReturn(rootFolder.resolve("logs") + File.separator);
		SiteNaming naming = new SiteNaming();
		Localisable<String, String> baseUrls = new Localisable<>();
		baseUrls.put("fr", BASE_URL);
		naming.setBaseUrls(baseUrls);
		when(uiConfig.getNamings()).thenReturn(naming);

		tv = vertical("tv", "televiseurs");
		verticalsConfigService = mock(VerticalsConfigService.class);
		when(verticalsConfigService.getConfigsWithoutDefault(true)).thenReturn(List.of(tv));
		when(verticalsConfigService.getDefaultGuides()).thenReturn(List.of());

		repository = mock(ProductRepository.class);
		meterRegistry = new SimpleMeterRegistry();
		service = new SitemapGenerationService(repository, uiConfig, verticalsConfigService, meterRegistry);
	}

	@Test
	void productSitemapListsExactlyTheCanonicalFullSlugs() throws Exception {
		Product renamed = product(1L, "tv", "1-smart-tv-qe32ls03cbu-32-pouces");
		Product other = product(2L, "tv", "/2-oled-55/");
		Product movedToDishwashers = product(3L, "dishwasher", "3-lave-vaisselle");
		Product withoutSlug = new Product(4L);
		withoutSlug.setVertical("tv");
		Product duplicate = product(1L, "tv", "1-smart-tv-qe32ls03cbu-32-pouces");
		when(repository.exportVerticalWithValidDateOrderByEcoscore("tv", false))
				.thenReturn(Stream.of(renamed, other, movedToDishwashers, withoutSlug, duplicate));

		assertThat(service.generate()).isTrue();

		Document xml = parse(rootFolder.resolve("sitemap/fr/product-pages.xml"));
		assertThat(texts(xml, "loc")).containsExactly(
				"https://nudger.fr" + ProductCanonicalUrl.fullSlug(renamed, tv, DomainLanguage.fr),
				"https://nudger.fr" + ProductCanonicalUrl.fullSlug(other, tv, DomainLanguage.fr));
		assertThat(texts(xml, "loc")).containsExactly(
				"https://nudger.fr/televiseurs/1-smart-tv-qe32ls03cbu-32-pouces",
				"https://nudger.fr/televiseurs/2-oled-55");
		assertThat(texts(xml, "lastmod")).hasSize(2).allMatch(value -> value.startsWith("2026-10-2"));

		assertThat(meterRegistry.get("sitemap.generation").tag("result", "success").counter().count()).isEqualTo(1);
		assertThat(meterRegistry.get("sitemap.generation.last.success").gauge().value()).isPositive();
		assertThat(meterRegistry.get("sitemap.product.urls").tag("language", "fr").gauge().value()).isEqualTo(2);
		assertThat(Files.exists(rootFolder.resolve("sitemap/fr/category-pages.xml"))).isTrue();
		assertThat(Files.exists(rootFolder.resolve("sitemap/fr/sitemap.xml"))).isTrue();
		assertThat(Files.exists(rootFolder.resolve("sitemap/fr/guides.xml"))).isFalse();
		assertThat(leftoverTemporaryFiles()).isEmpty();
	}

	@Test
	void failedProductExportKeepsThePreviousFileAndIsCounted() throws Exception {
		Path productPages = rootFolder.resolve("sitemap/fr/product-pages.xml");
		Files.createDirectories(productPages.getParent());
		Files.writeString(productPages, "previous complete sitemap");
		when(repository.exportVerticalWithValidDateOrderByEcoscore("tv", false))
				.thenThrow(new NullPointerException("elasticsearchOperations is null"));

		assertThat(service.generate()).isFalse();

		assertThat(Files.readString(productPages)).isEqualTo("previous complete sitemap");
		assertThat(meterRegistry.get("sitemap.generation").tag("result", "failure").counter().count()).isEqualTo(1);
		assertThat(meterRegistry.get("sitemap.generation.last.success").gauge().value()).isZero();
		// The other files of the language are still refreshed.
		assertThat(Files.exists(rootFolder.resolve("sitemap/fr/category-pages.xml"))).isTrue();
		assertThat(leftoverTemporaryFiles()).isEmpty();
	}

	@Test
	void languageKeysMapToDomainLanguages() {
		assertThat(SitemapGenerationService.toDomainLanguage("fr")).isEqualTo(DomainLanguage.fr);
		assertThat(SitemapGenerationService.toDomainLanguage("default")).isEqualTo(DomainLanguage.en);
		assertThat(SitemapGenerationService.toDomainLanguage("en")).isEqualTo(DomainLanguage.en);
		assertThat(SitemapGenerationService.toDomainLanguage("de")).isNull();
	}

	private List<Path> leftoverTemporaryFiles() throws IOException {
		try (Stream<Path> files = Files.walk(rootFolder.resolve("sitemap"))) {
			return files.filter(path -> path.getFileName().toString().endsWith(".tmp")).toList();
		}
	}

	private static Product product(long gtin, String vertical, String frSlug) {
		Product product = new Product(gtin);
		product.setVertical(vertical);
		product.getNames().getUrl().put("fr", frSlug);
		product.setLastChange(java.time.Instant.parse("2026-10-28T08:00:00Z").toEpochMilli());
		return product;
	}

	private static VerticalConfig vertical(String id, String frHomeUrl) {
		VerticalConfig config = new VerticalConfig();
		config.setId(id);
		ProductI18nElements fr = new ProductI18nElements();
		fr.setVerticalHomeUrl(frHomeUrl);
		config.getI18n().put("fr", fr);
		return config;
	}

	private static Document parse(Path file) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newDocumentBuilder().parse(new InputSource(new StringReader(Files.readString(file))));
	}

	private static List<String> texts(Document xml, String localName) {
		NodeList nodes = xml.getElementsByTagNameNS("*", localName);
		List<String> values = new ArrayList<>();
		for (int i = 0; i < nodes.getLength(); i++) {
			values.add(nodes.item(i).getTextContent().trim());
		}
		return values;
	}
}
