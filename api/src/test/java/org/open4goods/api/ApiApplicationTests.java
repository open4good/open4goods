package org.open4goods.api;

import org.junit.jupiter.api.Test;
import org.open4goods.brand.repository.BrandScoresRepository;
import org.open4goods.brand.service.BrandService;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.services.eprelservice.repository.EprelProductRepository;
import org.open4goods.services.productrepository.repository.ElasticProductRepository;
import org.open4goods.services.wikidataservice.repository.WikidataEntityRepository;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.prompt.service.provider.GeminiProvider;
import org.open4goods.services.prompt.service.provider.ProviderRegistry;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies the Spring application context loads without errors.
 * Catches startup failures (bean conflicts, missing auto-configurations) early in CI.
 *
 * Spring AI is incompatible with Spring Framework 7 (HttpHeaders.addAll API change),
 * so all OpenAI auto-configurations are excluded.
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration," +
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration," +
        "org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration," +
        "org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration," +
        "org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration," +
        "org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration," +
        "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration"
})
class ApiApplicationTests {

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private BrandScoresRepository brandScoresRepository;

    @MockitoBean
    private IndexationRepository indexationRepository;

    @MockitoBean
    private ElasticProductRepository elasticProductRepository;

    @MockitoBean
    private EprelProductRepository eprelProductRepository;

    @MockitoBean
    private IcecatFeatureRepository icecatFeatureRepository;

    @MockitoBean
    private IcecatFeatureGroupRepository icecatFeatureGroupRepository;

    @MockitoBean
    private IcecatCategoryRepository icecatCategoryRepository;

    @MockitoBean
    private IcecatSupplierRepository icecatSupplierRepository;

    @MockitoBean
    private WikidataEntityRepository wikidataEntityRepository;

    @MockitoBean
    private ElasticsearchOperations elasticsearchOperations;

    @MockitoBean
    private GeminiProvider geminiProvider;

    @MockitoBean
    private PromptService promptService;

    @MockitoBean
    private ProviderRegistry providerRegistry;

    @MockitoBean
    private OpenAiChatModel openAiChatModel;

    @MockitoBean
    private GoogleGenAiChatModel googleGenAiChatModel;

    @Test
    void contextLoads() {
    }

}
