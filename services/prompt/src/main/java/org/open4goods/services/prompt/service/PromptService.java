package org.open4goods.services.prompt.service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.services.evaluation.exception.TemplateEvaluationException;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service handling interactions with generative AI services.
 * <p>
 * It loads prompt templates from YAML files, instantiates chat models based on configuration, and
 * processes prompt evaluations with provided variables. It also implements HealthIndicator,
 * checking that required external AI API keys are provided and that the latest external API call succeeded.
 * </p>
 * <p>
 * This implementation uses the OpenAiChatModel from Spring AI to communicate with the appropriate AI
 * backend (e.g., OpenAI or Perplexity).
 * </p>
 */
@Service
public class PromptService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(PromptService.class);

    /**
     * Cache for chat models (keyed by prompt config key).
     */
    private final Map<String, OpenAiChatModel> models = new HashMap<>();

    /**
     * Cache for prompt configurations (loaded from YAML files).
     */
    private final Map<String, PromptConfig> prompts = new HashMap<>();

    private final OpenAiApi openAiApi;
    private final OpenAiApi perplexityApi;
    private final SerialisationService serialisationService;
    private final EvaluationService evaluationService;
    private final PromptServiceConfig genAiConfig;
    private final MeterRegistry meterRegistry;

    /**
     * Flag indicating the health status of the external API calls.
     * True means the last external API call succeeded, false if an exception was encountered.
     */
    private volatile boolean externalApiHealthy = true;

    /**
     * Constructs a new PromptService with the given dependencies including a MeterRegistry for metrics.
     *
     * @param genAiConfig          The configuration for the GenAI service.
     * @param perplexityApi        The API instance for Perplexity.
     * @param openAiCustomApi      The API instance for OpenAI.
     * @param serialisationService Service to handle serialization/deserialization.
     * @param evaluationService    Service to evaluate prompt templates.
     * @param meterRegistry        The MeterRegistry for actuator metrics.
     */
    public PromptService(PromptServiceConfig genAiConfig, OpenAiApi perplexityApi, OpenAiApi openAiCustomApi,
                         SerialisationService serialisationService, EvaluationService evaluationService, MeterRegistry meterRegistry) {
        this.openAiApi = openAiCustomApi;
        this.perplexityApi = perplexityApi;
        this.evaluationService = evaluationService;
        this.serialisationService = serialisationService;
        this.genAiConfig = genAiConfig;
        this.meterRegistry = meterRegistry;

        // Check if the service is enabled (if supported by configuration)
        if (!genAiConfig.isEnabled()) {
            logger.error("GenAiService is disabled via configuration.");
        }

        // Load prompt templates and initialize chat models
        loadPrompts(genAiConfig.getPromptsTemplatesFolder());
        loadModels();
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
     * @return A {@link PromptResponse} containing the AI call response and additional metadata.
     * @throws ResourceNotFoundException if the prompt configuration is not found.
     * @throws SerialisationException    if a serialization error occurs.
     */
    private PromptResponse<CallResponseSpec> promptNativ(String promptKey, Map<String, Object> variables, String jsonSchema)
            throws ResourceNotFoundException, SerialisationException {

        PromptConfig pConf = getPromptConfig(promptKey);
        if (pConf == null) {
            logger.error("PromptConfig {} does not exist", promptKey);
            throw new ResourceNotFoundException("Prompt not found");
        }
        
        // Increment request metric
        meterRegistry.counter("prompt.requests", "model", promptKey).increment();

        PromptResponse<CallResponseSpec> ret = new PromptResponse<>();
        ret.setStart(System.currentTimeMillis());

        // Evaluate system and user prompts using Thymeleaf with TemplateEvaluationException handling
        String systemPromptEvaluated = "";
        if (pConf.getSystemPrompt() != null) {
            try {
                systemPromptEvaluated = evaluationService.thymeleafEval(variables, pConf.getSystemPrompt());
            } catch (TemplateEvaluationException e) {
                meterRegistry.counter("prompt.errors", "model", promptKey).increment();
                logger.error("Template evaluation error for system prompt in {}: {}", promptKey, e.getMessage());
                throw e;
            }
        }
        String userPromptEvaluated;
        try {
            userPromptEvaluated = evaluationService.thymeleafEval(variables, pConf.getUserPrompt());
        } catch (TemplateEvaluationException e) {
            meterRegistry.counter("prompt.errors", "model", promptKey).increment();
            logger.error("Template evaluation error for user prompt in {}: {}", promptKey, e.getMessage());
            throw e;
        }

        // Build the chat client request with evaluated prompts and options
        ChatClientRequestSpec chatRequest = ChatClient.create(models.get(promptKey))
                .prompt()
                .user(userPromptEvaluated);
      
                if (!org.apache.commons.lang3.StringUtils.isEmpty(jsonSchema)) {
                	OpenAiChatOptions opts = serialisationService.clone(pConf.getOptions());
                	opts.setResponseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, jsonSchema));
                	chatRequest.options(opts);
                } else {
                	chatRequest.options(pConf.getOptions());
                }

        if (StringUtils.hasText(systemPromptEvaluated)) {
            chatRequest = chatRequest.system(systemPromptEvaluated);
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

        // Execute the API call and record response if successful.
        CallResponseSpec genAiResponse;
        try {
            genAiResponse = chatRequest.call();
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
                    String responseContent = genAiResponse.content();
                    File responseFile = new File(recordDir, promptKey + "-response.txt");
                    FileUtils.writeStringToFile(responseFile, responseContent, Charset.defaultCharset());
                    logger.info("Recorded prompt response content to file: {}", responseFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.error("Error recording prompt response for key {}: {}", promptKey, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            externalApiHealthy = false; // Mark as unhealthy due to API call exception.
            meterRegistry.counter("prompt.errors", "model", promptKey).increment();
            logger.error("Error during API call for promptKey {}: {}", promptKey, e.getMessage(), e);
            throw e;
        }

        // Populate the response object
        ret.setBody(genAiResponse);
        ret.setRaw(genAiResponse.content());
        ret.setDuration(System.currentTimeMillis() - ret.getStart());

        return ret;
    }

    public PromptResponse<String> prompt(String promptKey, Map<String, Object> variables)
            throws ResourceNotFoundException, SerialisationException {
        
        PromptResponse<String> ret = new PromptResponse<String>();
        
        PromptResponse<CallResponseSpec> nativ = promptNativ(promptKey, variables, null);
        ret.setBody(nativ.getRaw());
        ret.setRaw(nativ.getRaw());
        ret.setDuration(nativ.getDuration());
        ret.setPrompt(nativ.getPrompt());
        ret.setStart(nativ.getStart());
        
        return ret;
    }

    
    public <T> PromptResponse<T> objectPrompt(String promptKey, Map<String, Object> variables, Class<T> type)
            throws ResourceNotFoundException, SerialisationException {
        
        PromptResponse<T> ret = new PromptResponse<T>();
        
        
        var outputConverter = new BeanOutputConverter<>(type);
        var jsonSchema = outputConverter.getJsonSchema();        
        logger.info("Deducted json schema for prompt {} with type {} is {}", promptKey, type, jsonSchema);
        
        PromptResponse<CallResponseSpec> nativ = promptNativ(promptKey, variables, jsonSchema);
        ret.setBody(outputConverter.convert(nativ.getRaw()));
        ret.setRaw(nativ.getRaw());
        ret.setDuration(nativ.getDuration());
        ret.setPrompt(nativ.getPrompt());
        ret.setStart(nativ.getStart());
        
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
        PromptResponse<CallResponseSpec> internal = promptNativ(promptKey, variables, null);

        ret.setDuration(internal.getDuration());
        ret.setStart(internal.getStart());
        ret.setPrompt(internal.getPrompt());

        // Clean up markdown formatting from the response
        String response = internal.getBody().content().replace("```json", "").replace("```", "");
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
     * Loads and instantiates chat models based on the prompt configurations.
     */
    public void loadModels() {
        for (PromptConfig promptConfig : prompts.values()) {
            OpenAiChatModel model = switch (promptConfig.getAiService()) {
                case OPEN_AI -> new OpenAiChatModel(openAiApi);
                case PERPLEXITY -> new OpenAiChatModel(perplexityApi);
                default -> throw new IllegalArgumentException("Unexpected value: " + promptConfig.getAiService());
            };
            models.put(promptConfig.getKey(), model);
        }
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
    private PromptConfig getPromptConfig(String promptKey) {
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
     * Returns DOWN if either the OpenAI or Perplexity API key is missing,
     * or if the last external API call resulted in an exception.
     * </p>
     *
     * @return the Health status.
     */
    @Override
    public Health health() {
        if (genAiConfig.getOpenaiApiKey() == null || genAiConfig.getOpenaiApiKey().isBlank() ||
            genAiConfig.getPerplexityApiKey() == null || genAiConfig.getPerplexityApiKey().isBlank()) {
            return Health.down().withDetail("Error", "Missing API keys for external AI services").build();
        }
        if (!externalApiHealthy) {
            return Health.down().withDetail("Error", "Exception encountered during external API call").build();
        }
        return Health.up().build();
    }
}
