package org.open4goods.services.prompt.service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.open4goods.model.ai.AiFieldScanner;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.services.evaluation.exception.TemplateEvaluationException;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.config.RetrievalMode;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.services.prompt.service.provider.GenAiProvider;
import org.open4goods.services.prompt.service.provider.ProviderEvent;
import org.open4goods.services.prompt.service.provider.ProviderEventAccumulator;
import org.open4goods.services.prompt.service.provider.ProviderRegistry;
import org.open4goods.services.prompt.service.provider.ProviderRequest;
import org.open4goods.services.prompt.service.provider.ProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Flux;

/**
 * Service handling interactions with generative AI services.
 * <p>
 * It loads prompt templates from YAML files, instantiates chat models based on configuration, and
 * processes prompt evaluations with provided variables. It also implements HealthIndicator,
 * checking that required external AI API keys are provided and that the latest external API call succeeded.
 * </p>
 * <p>
 * This implementation uses Spring AI chat models to communicate with the appropriate AI
 * backend (OpenAI or Gemini).
 * </p>
 */
@Service
public class PromptService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(PromptService.class);

    private static final String INSTRUCTION_HEADER_FR = "\n. En complément du schéma JSON, voici les instructions concernant chaque champs que tu dois fournir.\n";

    /**
     * Cache for prompt configurations (loaded from YAML files).
     */
    private final Map<String, PromptConfig> prompts = new HashMap<>();

    private final SerialisationService serialisationService;
    private final EvaluationService evaluationService;
    private final PromptServiceConfig genAiConfig;
    private final MeterRegistry meterRegistry;
    private final ProviderRegistry providerRegistry;

    /**
     * Flag indicating the health status of the external API calls.
     * True means the last external API call succeeded, false if an exception was encountered.
     */
    private volatile boolean externalApiHealthy = true;

    /**
     * Constructs a new PromptService with the given dependencies including a MeterRegistry for metrics.
     *
     * @param genAiConfig          The configuration for the GenAI service.
     * @param serialisationService Service to handle serialization/deserialization.
     * @param evaluationService    Service to evaluate prompt templates.
     * @param meterRegistry        The MeterRegistry for actuator metrics.
     */
    public PromptService(PromptServiceConfig genAiConfig, SerialisationService serialisationService,
                         EvaluationService evaluationService, MeterRegistry meterRegistry,
                         ProviderRegistry providerRegistry) {
        this.evaluationService = evaluationService;
        this.serialisationService = serialisationService;
        this.genAiConfig = genAiConfig;
        this.meterRegistry = meterRegistry;
        this.providerRegistry = providerRegistry;

        // Check if the service is enabled (if supported by configuration)
        if (!genAiConfig.isEnabled()) {
            logger.error("GenAiService is disabled via configuration.");
            return;
        }

        // Load prompt templates and initialize chat models
        loadPrompts(genAiConfig.getPromptsTemplatesFolder());
    }

    /**
     * Executes a prompt against an AI service based on a prompt key and provided variables.
     * <p>
     * The evaluated prompt request is recorded to a YAML file before invoking the external API.
     * Additionally, if recording is enabled, a human-readable text file containing both the system
     * and user prompts (with header metadata) is generated.
     * This ensures that the request is captured even if the API call fails.
     * </p>
     *
     * @param promptKey The key identifying the prompt configuration.
     * @param variables The variables to resolve within the prompt templates.
     * @param jsonSchema Optional jsonschema we must conform on
     * @param instructions
     * @return A {@link PromptResponse} containing the AI call response and additional metadata.
     * @throws ResourceNotFoundException if the prompt configuration is not found.
     * @throws SerialisationException    if a serialization error occurs.
     */
    private PromptResponse<ProviderResult> promptNativ(String promptKey, Map<String, Object> variables, String jsonSchema,
                                                       Map<String, String> instructions)
            throws ResourceNotFoundException, SerialisationException {

        PromptConfig pConf = getPromptConfig(promptKey);
        if (pConf == null) {
            logger.error("PromptConfig {} does not exist", promptKey);
            throw new ResourceNotFoundException("Prompt not found");
        }

        // Increment request metric
        meterRegistry.counter("prompt.requests",
                "prompt", promptKey,
                "provider", pConf.getAiService() != null ? pConf.getAiService().name() : "UNKNOWN",
                "retrievalMode", pConf.getRetrievalMode().name()).increment();

        PromptResponse<ProviderResult> ret = new PromptResponse<>();
        ret.setStart(System.currentTimeMillis());

        // Evaluate system and user prompts using Thymeleaf with TemplateEvaluationException handling
        String systemPromptEvaluated = "";
        if (pConf.getSystemPrompt() != null) {
            try {
                systemPromptEvaluated = evaluationService.thymeleafEval(variables, pConf.getSystemPrompt());
            } catch (TemplateEvaluationException e) {
                meterRegistry.counter("prompt.errors",
                        "prompt", promptKey,
                        "provider", pConf.getAiService() != null ? pConf.getAiService().name() : "UNKNOWN",
                        "retrievalMode", pConf.getRetrievalMode().name()).increment();
                logger.error("Template evaluation error for system prompt in {}: {}", promptKey, e.getMessage());
                throw e;
            }
        }
        String userPromptEvaluated;
        try {
            userPromptEvaluated = evaluationService.thymeleafEval(variables, pConf.getUserPrompt());
        } catch (TemplateEvaluationException e) {
            meterRegistry.counter("prompt.errors",
                    "prompt", promptKey,
                    "provider", pConf.getAiService() != null ? pConf.getAiService().name() : "UNKNOWN",
                    "retrievalMode", pConf.getRetrievalMode().name()).increment();
            logger.error("Template evaluation error for user prompt in {}: {}", promptKey, e.getMessage());
            throw e;
        }

        // Adding the instructions at the end of system prompt if presents

        if (null != instructions && instructions.size() > 0) {
        	systemPromptEvaluated += INSTRUCTION_HEADER_FR;
        	for (Entry<String, String> entry : instructions.entrySet()) {
        		systemPromptEvaluated+=entry.getKey() + " : " + entry.getValue()+"\n";
        	}
        }


        // Clone prompt configuration and update with evaluated prompts
        String yamlPromptConfig = serialisationService.toYamLiteral(pConf);
        PromptConfig updatedConfig = serialisationService.fromYaml(yamlPromptConfig, PromptConfig.class);
        updatedConfig.setSystemPrompt(systemPromptEvaluated);
        updatedConfig.setUserPrompt(userPromptEvaluated);

        ret.setPrompt(updatedConfig);
        logger.info("Resolved prompt config for {} is : {} \n", promptKey, serialisationService.toYamLiteral(updatedConfig));

        // --- Recording Request (YAML) BEFORE API call ---
        if (genAiConfig.isRecordEnabled() && genAiConfig.getRecordFolder() != null) {
            try {
                File recordDir = new File(genAiConfig.getRecordFolder());
                if (!recordDir.exists()) {
                    recordDir.mkdirs();
                }
                String requestYaml = serialisationService.toYamLiteral(updatedConfig);
                File requestFile = new File(recordDir, promptKey + "-request.yaml");
                FileUtils.writeStringToFile(requestFile, requestYaml, Charset.defaultCharset());
                logger.info("Recorded prompt request to file: {}", requestFile.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Error recording prompt request for key {}: {}", promptKey, e.getMessage(), e);
            }
        }
        // ------------------------------------------------

        // --- Recording Prompt Texts (Readable) BEFORE API call ---
        if (genAiConfig.isRecordEnabled() && genAiConfig.getRecordFolder() != null) {
            try {
                File recordDir = new File(genAiConfig.getRecordFolder());
                if (!recordDir.exists()) {
                    recordDir.mkdirs();
                }
                int systemTokens = estimateTokens(systemPromptEvaluated);
                int userTokens = estimateTokens(userPromptEvaluated);
                String headerSystem = "####################################################################\n" +
                        "Generation Date: " + new Date() + "\n" +
                        "Prompt Type: SYSTEM\n" +
                        "Estimated Tokens: " + systemTokens + "\n" +
                        "####################################################################\n";
                String headerUser = "####################################################################\n" +
                        "Generation Date: " + new Date() + "\n" +
                        "Prompt Type: USER\n" +
                        "Estimated Tokens: " + userTokens + "\n" +
                        "####################################################################\n";
                String promptRecordContent = headerSystem + systemPromptEvaluated + "\n\n" + headerUser + userPromptEvaluated;
                File promptRecordFile = new File(recordDir, promptKey + "-request-prompts.txt");
                FileUtils.writeStringToFile(promptRecordFile, promptRecordContent, Charset.defaultCharset());
                logger.info("Recorded prompt texts to file: {}", promptRecordFile.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Error recording prompt texts for key {}: {}", promptKey, e.getMessage(), e);
            }
        }
        // ------------------------------------------------

        ProviderResult providerResult;
        String responseContent = null;
        try {
            GenAiProvider provider = providerRegistry.getProvider(pConf.getAiService());
            ProviderRequest providerRequest = new ProviderRequest(
                    promptKey,
                    systemPromptEvaluated,
                    userPromptEvaluated,
                    pConf.getOptions(),
                    pConf.getRetrievalMode(),
                    jsonSchema,
                    pConf.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH,
                    pConf.getProviderOptions()
            );
            providerResult = provider.generateText(providerRequest);
            responseContent = providerResult.getContent();
            externalApiHealthy = true; // API call succeeded, mark healthy.
            // --- Recording Response AFTER API call ---
            if (genAiConfig.isRecordEnabled() && genAiConfig.getRecordFolder() != null) {
                try {
                    File recordDir = new File(genAiConfig.getRecordFolder());
                    if (!recordDir.exists()) {
                        recordDir.mkdirs();
                    }
                    // Instead of serializing the whole CallResponseSpec (which cannot be done),
                    // record only its raw string content.
                    if (responseContent != null) {
                        File responseFile = new File(recordDir, promptKey + "-response.txt");
                        FileUtils.writeStringToFile(responseFile, responseContent, Charset.defaultCharset());
                        logger.info("Recorded prompt response content to file: {}", responseFile.getAbsolutePath());
                    } else {
                        logger.warn("Prompt response for key {} was null; skipping response recording.", promptKey);
                    }
                } catch (Exception e) {
                    logger.error("Error recording prompt response for key {}: {}", promptKey, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            externalApiHealthy = false; // Mark as unhealthy due to API call exception.
            meterRegistry.counter("prompt.errors",
                    "prompt", promptKey,
                    "provider", pConf.getAiService() != null ? pConf.getAiService().name() : "UNKNOWN",
                    "retrievalMode", pConf.getRetrievalMode().name()).increment();
            logger.error("Error during API call for promptKey {}: {}", promptKey, e.getMessage(), e);
            throw e;
        }

        // Populate the response object
        ret.setBody(providerResult);
        ret.setRaw(responseContent);
        ret.setMetadata(providerResult.getMetadata());
        ret.setDuration(System.currentTimeMillis() - ret.getStart());

        return ret;
    }

    public PromptResponse<String> prompt(String promptKey, Map<String, Object> variables)
            throws ResourceNotFoundException, SerialisationException {

        PromptResponse<String> ret = new PromptResponse<String>();

        PromptResponse<ProviderResult> nativ = promptNativ(promptKey, variables, null, null);
        ret.setBody(nativ.getBody().getContent());
        ret.setRaw(nativ.getBody().getContent());
        ret.setMetadata(nativ.getMetadata());
        ret.setDuration(nativ.getDuration());
        ret.setPrompt(nativ.getPrompt());
        ret.setStart(nativ.getStart());

        return ret;
    }


    public <T> PromptResponse<T> objectPrompt(String promptKey, Map<String, Object> variables, Class<T> type)
            throws ResourceNotFoundException, SerialisationException {

        PromptResponse<T> ret = new PromptResponse<T>();

        // TODO(p2, perf) : should cache instructions scanner and bean output converter
        var outputConverter = new BeanOutputConverter<>(type);
        var jsonSchema = outputConverter.getJsonSchema();
        logger.info("Deducted json schema for prompt {} with type {} is {}", promptKey, type, jsonSchema);

       Map<String, String> instructions = AiFieldScanner.getGenAiInstruction(type);

       if (null == instructions || instructions.size() == 0) {
    	   logger.error("Error while retrieving AifieldScanner instructions");
       }


        PromptResponse<ProviderResult> nativ = promptNativ(promptKey, variables, jsonSchema, instructions);
        String raw = nativ.getBody().getContent();
        try {
            ret.setBody(outputConverter.convert(raw));
            ret.setRaw(raw);
        } catch (Exception e) {
            meterRegistry.counter("prompt.json.repair",
                    "prompt", promptKey,
                    "provider", nativ.getPrompt().getAiService() != null
                            ? nativ.getPrompt().getAiService().name()
                            : "UNKNOWN",
                    "retrievalMode", nativ.getPrompt().getRetrievalMode().name()).increment();
            logger.warn("JSON parsing failed for prompt {}. Attempting repair.", promptKey, e);
            String repaired = repairJson(promptKey, raw, jsonSchema, nativ.getPrompt());
            ret.setBody(outputConverter.convert(repaired));
            ret.setRaw(repaired);
        }
        ret.setMetadata(nativ.getMetadata());
        ret.setDuration(nativ.getDuration());
        ret.setPrompt(nativ.getPrompt());
        ret.setStart(nativ.getStart());

        return ret;
    }

    /**
     * Executes a prompt with streaming callbacks while still producing a typed response.
     *
     * @param promptKey      The key identifying the prompt configuration.
     * @param variables      The variables to resolve within the prompt templates.
     * @param type           The response type to convert to.
     * @param eventConsumer  A consumer receiving streaming provider events.
     * @return A {@link PromptResponse} with the final parsed body and metadata.
     */
    public <T> PromptResponse<T> objectPromptStream(String promptKey, Map<String, Object> variables, Class<T> type,
                                                    Consumer<ProviderEvent> eventConsumer)
            throws ResourceNotFoundException, SerialisationException {

        PromptConfig pConf = getPromptConfig(promptKey);
        if (pConf == null) {
            logger.error("PromptConfig {} does not exist", promptKey);
            throw new ResourceNotFoundException("Prompt not found");
        }

        meterRegistry.counter("prompt.requests",
                "prompt", promptKey,
                "provider", pConf.getAiService() != null ? pConf.getAiService().name() : "UNKNOWN",
                "retrievalMode", pConf.getRetrievalMode().name()).increment();

        PromptResponse<T> ret = new PromptResponse<>();
        ret.setStart(System.currentTimeMillis());

        var outputConverter = new BeanOutputConverter<>(type);
        var jsonSchema = outputConverter.getJsonSchema();
        Map<String, String> instructions = AiFieldScanner.getGenAiInstruction(type);

        String systemPromptEvaluated = "";
        if (pConf.getSystemPrompt() != null) {
            try {
                systemPromptEvaluated = evaluationService.thymeleafEval(variables, pConf.getSystemPrompt());
            } catch (TemplateEvaluationException e) {
                meterRegistry.counter("prompt.errors",
                        "prompt", promptKey,
                        "provider", pConf.getAiService() != null ? pConf.getAiService().name() : "UNKNOWN",
                        "retrievalMode", pConf.getRetrievalMode().name()).increment();
                logger.error("Template evaluation error for system prompt in {}: {}", promptKey, e.getMessage());
                throw e;
            }
        }
        String userPromptEvaluated;
        try {
            userPromptEvaluated = evaluationService.thymeleafEval(variables, pConf.getUserPrompt());
        } catch (TemplateEvaluationException e) {
            meterRegistry.counter("prompt.errors",
                    "prompt", promptKey,
                    "provider", pConf.getAiService() != null ? pConf.getAiService().name() : "UNKNOWN",
                    "retrievalMode", pConf.getRetrievalMode().name()).increment();
            logger.error("Template evaluation error for user prompt in {}: {}", promptKey, e.getMessage());
            throw e;
        }

        if (null != instructions && !instructions.isEmpty()) {
            systemPromptEvaluated += INSTRUCTION_HEADER_FR;
            for (Entry<String, String> entry : instructions.entrySet()) {
                systemPromptEvaluated += entry.getKey() + " : " + entry.getValue() + "\n";
            }
        }

        String yamlPromptConfig = serialisationService.toYamLiteral(pConf);
        PromptConfig updatedConfig = serialisationService.fromYaml(yamlPromptConfig, PromptConfig.class);
        updatedConfig.setSystemPrompt(systemPromptEvaluated);
        updatedConfig.setUserPrompt(userPromptEvaluated);
        ret.setPrompt(updatedConfig);

        if (genAiConfig.isRecordEnabled() && genAiConfig.getRecordFolder() != null) {
            recordPromptRequest(promptKey, updatedConfig, systemPromptEvaluated, userPromptEvaluated);
        }

        GenAiProvider provider = providerRegistry.getProvider(pConf.getAiService());
        ProviderRequest providerRequest = new ProviderRequest(
                promptKey,
                systemPromptEvaluated,
                userPromptEvaluated,
                pConf.getOptions(),
                pConf.getRetrievalMode(),
                jsonSchema,
                pConf.getRetrievalMode() == RetrievalMode.MODEL_WEB_SEARCH,
                pConf.getProviderOptions()
        );

        ProviderEventAccumulator accumulator = new ProviderEventAccumulator();
        try {
            Flux<ProviderEvent> stream = provider.generateTextStream(providerRequest)
                    .doOnNext(event -> {
                        accumulator.accept(event);
                        if (eventConsumer != null) {
                            eventConsumer.accept(event);
                        }
                    });
            stream.blockLast();
            externalApiHealthy = true;
        } catch (Exception e) {
            externalApiHealthy = false;
            meterRegistry.counter("prompt.errors",
                    "prompt", promptKey,
                    "provider", pConf.getAiService() != null ? pConf.getAiService().name() : "UNKNOWN",
                    "retrievalMode", pConf.getRetrievalMode().name()).increment();
            logger.error("Error during streaming API call for promptKey {}: {}", promptKey, e.getMessage(), e);
            throw e;
        }

        String raw = accumulator.getContent();
        try {
            ret.setBody(outputConverter.convert(raw));
            logger.info("Raw LLM output : \n {}",raw);
            ret.setRaw(raw);
        } catch (Exception e) {
            meterRegistry.counter("prompt.json.repair",
                    "prompt", promptKey,
                    "provider", pConf.getAiService() != null
                            ? pConf.getAiService().name()
                            : "UNKNOWN",
                    "retrievalMode", pConf.getRetrievalMode().name()).increment();
            logger.warn("JSON parsing failed for prompt {}. Attempting repair.", promptKey, e);
            String repaired = repairJson(promptKey, raw, jsonSchema, updatedConfig);
            ret.setBody(outputConverter.convert(repaired));
            ret.setRaw(repaired);
        }
        ret.setMetadata(accumulator.getMetadata());
        ret.setDuration(System.currentTimeMillis() - ret.getStart());
        if (genAiConfig.isRecordEnabled() && genAiConfig.getRecordFolder() != null && ret.getRaw() != null) {
            recordPromptResponse(promptKey, ret.getRaw());
        }
        return ret;
    }

    /**
     * Executes a prompt and converts the response into a JSON map.
     *
     * @param promptKey The key identifying the prompt configuration.
     * @param variables The variables to resolve within the prompt templates.
     * @return A {@link PromptResponse} containing the response as a JSON map.
     * @throws ResourceNotFoundException if the prompt configuration is not found.
     * @throws SerialisationException    if a serialization error occurs.
     */
    public PromptResponse<Map<String, Object>> jsonPrompt(String promptKey, Map<String, Object> variables)
            throws ResourceNotFoundException, SerialisationException {

        PromptResponse<Map<String, Object>> ret = new PromptResponse<>();
        PromptResponse<ProviderResult> internal = promptNativ(promptKey, variables, null, null);

        ret.setDuration(internal.getDuration());
        ret.setStart(internal.getStart());
        ret.setPrompt(internal.getPrompt());
        ret.setMetadata(internal.getMetadata());

        // Clean up markdown formatting from the response
        String response = internal.getBody().getContent().replace("```json", "").replace("```", "");
        ret.setRaw(response);
        try {
            ret.setBody(serialisationService.fromJsonTypeRef(response, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception e) {
            logger.error("Unable to map to JSON structure: {} \nResponse: {}", e.getMessage(), response);
            throw new IllegalStateException("Response mapping to JSON failed", e);
        }
        return ret;
    }

    /**
     * Retrieves the prompt configuration for the given key.
     * <p>
     * If caching is enabled, the configuration is retrieved from memory; otherwise, it is loaded from the file.
     * </p>
     *
     * @param promptKey The unique key identifying the prompt configuration.
     * @return The {@link PromptConfig} or {@code null} if not found.
     */
    public PromptConfig getPromptConfig(String promptKey) {
        if (genAiConfig.isCacheTemplates()) {
            return prompts.get(promptKey);
        } else {
            String path = genAiConfig.getPromptsTemplatesFolder() + "/" + promptKey + ".yml";
            return loadPrompt(new File(path));
        }
    }

    /**
     * Loads all prompt configuration files from the specified folder.
     *
     * @param folderPath The folder containing YAML prompt templates.
     */
    public void loadPrompts(String folderPath) {
        try {
            File folder = new File(folderPath);
            if (folder.exists() && folder.isDirectory()) {
                // Filter YAML files and load each prompt configuration
                List<File> promptsFile = Arrays.stream(folder.listFiles())
                        .filter(e -> e.getName().endsWith(".yml"))
                        .toList();

                promptsFile.forEach(f -> {
                    PromptConfig pc = loadPrompt(f);
                    if (pc != null) {
                        this.prompts.put(pc.getKey(), pc);
                    } else {
                        logger.error("Failed to load prompt configuration from file: {}", f.getAbsolutePath());
                    }
                });
            } else {
                logger.error("Cannot load prompts: folder {} is invalid", folderPath);
            }
        } catch (Exception e) {
            logger.error("Error loading prompts from {}", folderPath, e);
        }
    }

    /**
     * Loads a single {@link PromptConfig} from a YAML file.
     *
     * @param file The YAML file containing the prompt configuration.
     * @return The loaded {@link PromptConfig} or {@code null} if an error occurs.
     */
    private PromptConfig loadPrompt(File file) {
        try {
            String content = FileUtils.readFileToString(file, Charset.defaultCharset());
            return serialisationService.fromYaml(content, PromptConfig.class);
        } catch (Exception e) {
            logger.error("Error while reading prompt config file: {}", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Protected getter for the GenAi configuration.
     *
     * @return the PromptServiceConfig
     */
    protected PromptServiceConfig getGenAiConfig() {
        return genAiConfig;
    }

    /**
     * Protected getter for the SerialisationService.
     *
     * @return the SerialisationService
     */
    protected SerialisationService getSerialisationService() {
        return serialisationService;
    }

    /**
     * Estimate the number of tokens for a given text.
     *
     * @param text the text to estimate tokens for
     * @return the estimated token count
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // Approximate: 1.3 tokens per word
        int words = text.split("\\s+").length;
        return (int) Math.ceil(words * 1.3);
    }

    /**
     * Estimate the number of tokens for a given prompt config.
     *
     * @param text the text to estimate tokens for
     * @return the estimated token count
     */
    public int estimateTokens(PromptConfig promptConfig) {
    	return estimateTokens(promptConfig.getSystemPrompt()) + estimateTokens(promptConfig.getUserPrompt());

    }


    /**
     * Health check for the Prompt Service.
     * <p>
     * Returns DOWN if a required provider is missing, or if the last external API call resulted
     * in an exception.
     * </p>
     *
     * @return the Health status.
     */
    @Override
    public Health health() {
        if (!hasRequiredProviders()) {
            return Health.down().withDetail("Error", "Missing GenAI provider configuration").build();
        }
        if (!externalApiHealthy) {
            return Health.down().withDetail("Error", "Exception encountered during external API call").build();
        }
        return Health.up().build();
    }

    private boolean hasRequiredProviders() {
        for (PromptConfig promptConfig : prompts.values()) {
            if (promptConfig.getAiService() == null) {
                continue;
            }
            if (!providerRegistry.hasProvider(promptConfig.getAiService())) {
                return false;
            }
        }
        return true;
    }

    private String repairJson(String promptKey, String raw, String jsonSchema, PromptConfig promptConfig)
            throws SerialisationException {
        String systemPrompt = "You are a JSON repair assistant. Return only valid JSON.";
        if (StringUtils.hasText(jsonSchema)) {
            systemPrompt += "\n\nEnsure the output matches this schema:\n" + jsonSchema;
        }
        String userPrompt = "Repair the following JSON response to be valid and match the schema.\n\n" + raw;
        ProviderRequest repairRequest = new ProviderRequest(
                promptKey + "-repair",
                systemPrompt,
                userPrompt,
                promptConfig.getOptions(),
                RetrievalMode.EXTERNAL_SOURCES,
                jsonSchema,
                false,
                promptConfig.getProviderOptions()
        );
        ProviderResult repairResult = providerRegistry.getProvider(promptConfig.getAiService()).generateText(repairRequest);
        if (genAiConfig.isRecordEnabled() && genAiConfig.getRecordFolder() != null) {
            try {
                File recordDir = new File(genAiConfig.getRecordFolder());
                if (!recordDir.exists()) {
                    recordDir.mkdirs();
                }
                File responseFile = new File(recordDir, promptKey + "-response-repaired.txt");
                FileUtils.writeStringToFile(responseFile, repairResult.getContent(), Charset.defaultCharset());
                logger.info("Recorded repaired prompt response content to file: {}", responseFile.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Error recording repaired prompt response for key {}: {}", promptKey, e.getMessage(), e);
            }
        }
        return repairResult.getContent();
    }

    private void recordPromptRequest(String promptKey, PromptConfig updatedConfig, String systemPromptEvaluated,
                                     String userPromptEvaluated) {
        try {
            File recordDir = new File(genAiConfig.getRecordFolder());
            if (!recordDir.exists()) {
                recordDir.mkdirs();
            }
            String requestYaml = serialisationService.toYamLiteral(updatedConfig);
            File requestFile = new File(recordDir, promptKey + "-request.yaml");
            FileUtils.writeStringToFile(requestFile, requestYaml, Charset.defaultCharset());
            logger.info("Recorded prompt request to file: {}", requestFile.getAbsolutePath());

            int systemTokens = estimateTokens(systemPromptEvaluated);
            int userTokens = estimateTokens(userPromptEvaluated);
            String headerSystem = "####################################################################\n" +
                    "Generation Date: " + new Date() + "\n" +
                    "Prompt Type: SYSTEM\n" +
                    "Estimated Tokens: " + systemTokens + "\n" +
                    "####################################################################\n";
            String headerUser = "####################################################################\n" +
                    "Generation Date: " + new Date() + "\n" +
                    "Prompt Type: USER\n" +
                    "Estimated Tokens: " + userTokens + "\n" +
                    "####################################################################\n";
            String promptRecordContent = headerSystem + systemPromptEvaluated + "\n\n" + headerUser + userPromptEvaluated;
            File promptRecordFile = new File(recordDir, promptKey + "-request-prompts.txt");
            FileUtils.writeStringToFile(promptRecordFile, promptRecordContent, Charset.defaultCharset());
            logger.info("Recorded prompt texts to file: {}", promptRecordFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error recording prompt request for key {}: {}", promptKey, e.getMessage(), e);
        }
    }

    private void recordPromptResponse(String promptKey, String rawResponse) {
        try {
            File recordDir = new File(genAiConfig.getRecordFolder());
            if (!recordDir.exists()) {
                recordDir.mkdirs();
            }
            File responseFile = new File(recordDir, promptKey + "-response.txt");
            FileUtils.writeStringToFile(responseFile, rawResponse, Charset.defaultCharset());
            logger.info("Recorded prompt response content to file: {}", responseFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error recording prompt response for key {}: {}", promptKey, e.getMessage(), e);
        }
    }
}
