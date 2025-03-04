package org.open4goods.services.prompt.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.vertical.GenAiConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.prompt.config.GenAiServiceType;
import org.open4goods.services.prompt.config.PromptConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Service handling interactions with generative AI services.
 * <p>
 * It loads prompt templates from YAML files, instantiates chat models based on configuration, and
 * processes prompt evaluations with provided variables.
 * </p>
 * <p>
 * This implementation uses the OpenAiChatModel from Spring AI to communicate with the appropriate AI
 * backend (e.g., OpenAI or Perplexity).
 * </p>
 */
@Service
public class GenAiService {

    private static final Logger logger = LoggerFactory.getLogger(GenAiService.class);

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
    private final GenAiConfig genAiConfig;

    /**
     * Constructs a new GenAiService with the given dependencies.
     *
     * @param genAiConfig          The configuration for the GenAI service.
     * @param perplexityApi        The API instance for Perplexity.
     * @param openAiCustomApi      The API instance for OpenAI.
     * @param serialisationService Service to handle serialization/deserialization.
     * @param evaluationService    Service to evaluate prompt templates.
     */
    public GenAiService(GenAiConfig genAiConfig, OpenAiApi perplexityApi, OpenAiApi openAiCustomApi,
                        SerialisationService serialisationService, EvaluationService evaluationService) {
        this.openAiApi = openAiCustomApi;
        this.perplexityApi = perplexityApi;
        this.evaluationService = evaluationService;
        this.serialisationService = serialisationService;
        this.genAiConfig = genAiConfig;

        // Check if the service is enabled (if supported by configuration)
        if (!genAiConfig.isEnabled()) {
            logger.warn("GenAiService is disabled via configuration.");
            throw new IllegalStateException("GenAiService is disabled by configuration");
        }

        // Load prompt templates and initialize chat models
        loadPrompts(genAiConfig.getPromptsTemplatesFolder());
        loadModels();
    }

    /**
     * Executes a prompt against an AI service based on a prompt key and provided variables.
     *
     * @param promptKey The key identifying the prompt configuration.
     * @param variables The variables to resolve within the prompt templates.
     * @return A {@link PromptResponse} containing the AI call response and additional metadata.
     * @throws ResourceNotFoundException if the prompt configuration is not found.
     * @throws IOException               if an error occurs during prompt processing.
     * @throws JsonMappingException      if the response mapping fails.
     * @throws JsonParseException        if the JSON parsing fails.
     */
    public PromptResponse<CallResponseSpec> prompt(String promptKey, Map<String, Object> variables)
            throws ResourceNotFoundException, JsonParseException, JsonMappingException, IOException {

        PromptConfig pConf = getPromptConfig(promptKey);
        if (pConf == null) {
            logger.error("PromptConfig {} does not exist", promptKey);
            throw new ResourceNotFoundException("Prompt not found");
        }

        PromptResponse<CallResponseSpec> ret = new PromptResponse<>();
        ret.setStart(System.currentTimeMillis());

        // Evaluate system and user prompts using Thymeleaf
        String systemPromptEvaluated = pConf.getSystemPrompt() == null ? "" :
                evaluationService.thymeleafEval(variables, pConf.getSystemPrompt());
        String userPromptEvaluated = evaluationService.thymeleafEval(variables, pConf.getUserPrompt());


        // Build the chat client request with evaluated prompts and options
        ChatClientRequestSpec chatRequest = ChatClient.create(models.get(promptKey))
                .prompt()
                .user(userPromptEvaluated)
                .options(pConf.getOptions());

        if (StringUtils.hasText(systemPromptEvaluated)) {
            chatRequest = chatRequest.system(systemPromptEvaluated);
        }

        // Execute the request
        CallResponseSpec genAiResponse = chatRequest.call();

        // Populate the response object
        ret.setBody(genAiResponse);
        ret.setRaw(genAiResponse.content());

        // Clone prompt configuration and update with evaluated prompts
        PromptConfig updatedConfig = serialisationService.fromJson(
                serialisationService.toJson(pConf), PromptConfig.class);
        updatedConfig.setSystemPrompt(systemPromptEvaluated);
        updatedConfig.setUserPrompt(userPromptEvaluated);
        ret.setPrompt(updatedConfig);

        ret.setDuration(System.currentTimeMillis() - ret.getStart());
        return ret;
    }

    /**
     * Executes a prompt and converts the response into a JSON map.
     *
     * @param promptKey The key identifying the prompt configuration.
     * @param variables The variables to resolve within the prompt templates.
     * @return A {@link PromptResponse} containing the response as a JSON map.
     * @throws ResourceNotFoundException if the prompt configuration is not found.
     * @throws IOException               if an error occurs during prompt processing.
     * @throws JsonMappingException      if the response mapping fails.
     * @throws JsonParseException        if the JSON parsing fails.
     */
    public PromptResponse<Map<String, Object>> jsonPrompt(String promptKey, Map<String, Object> variables)
            throws ResourceNotFoundException, JsonParseException, JsonMappingException, IOException {

        PromptResponse<Map<String, Object>> ret = new PromptResponse<>();
        PromptResponse<CallResponseSpec> internal = prompt(promptKey, variables);

        ret.setDuration(internal.getDuration());
        ret.setStart(internal.getStart());
        ret.setPrompt(internal.getPrompt());

        // Clean up markdown formatting from the response
        String response = internal.getBody().content().replace("```json", "").replace("```", "");
        ret.setRaw(response);
        try {
            ret.setBody(serialisationService.fromJsonTypeRef(response,
                    new TypeReference<Map<String, Object>>() {
                    }));
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
}
