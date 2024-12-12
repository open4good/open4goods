package org.open4goods.commons.services.ai;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.open4goods.commons.config.yml.GenAiServiceType;
import org.open4goods.commons.config.yml.PromptConfig;
import org.open4goods.commons.config.yml.ui.GenAiConfig;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.services.EvaluationService;
import org.open4goods.commons.services.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.ParameterizedTypeReference;

/**
 * This service handle gen ai service capabilities, based on the OpenAiChatModel
 * and OpenAiChatOptions from Spring AI. It is used to prompt precisly from open
 * ai and perplexity It is based on files containing model options and
 * templatized prompts
 */
public class GenAiService {

	private final Logger logger = LoggerFactory.getLogger(GenAiService.class);

	private Map<String, OpenAiChatModel> models = new HashMap<String, OpenAiChatModel>();

	private Map<String, PromptConfig> prompts = new HashMap<String, PromptConfig>();

	private OpenAiApi openAiApi;
	private OpenAiApi perplexityApi;

	private SerialisationService serialisationService;
	private EvaluationService evaluationService;
	private GenAiConfig genAiConfig;

	public GenAiService(GenAiConfig genAiConfig, OpenAiApi perplexityApi, OpenAiApi openAiCustomApi, SerialisationService serialisationService, EvaluationService evaluationService) {
		super();
		this.openAiApi = openAiCustomApi;
		this.perplexityApi = perplexityApi;
		this.evaluationService = evaluationService;
		this.serialisationService = serialisationService;
		this.genAiConfig = genAiConfig;

		// Loading the prompts template files
		loadPrompts(genAiConfig.getPromptsTemplatesFolder());

		// Instanciating the chat models
		loadModels();
	}

	/**
	 * Run a prompt against an IA Service, given the config file corresponding to
	 * promptKey
	 * 
	 * @param promptKey
	 * @param variables
	 * @return
	 * @throws ResourceNotFoundException
	 */
	public CallResponseSpec prompt(String promptKey, Map<String, Object> variables) throws ResourceNotFoundException {

		PromptConfig pConf = prompts.get(promptKey);

		if (null == pConf) {
			logger.error("PromptConfig {}  does not exists", promptKey);
			throw new ResourceNotFoundException("Prompt not found");
		}

		// Evaluating prompts,
		String systemPrompt = evaluationService.thymeleafEval(variables, pConf.getSystemPrompt());
		String userPrompt = evaluationService.thymeleafEval(variables, pConf.getUserPrompt());

		// Checking there are no remainings unevaluated expression

	// TODO Detect if remaining variables

		CallResponseSpec ret = ChatClient.create(models.get(promptKey)).prompt()
				.system(systemPrompt)
				.user(userPrompt)
				.options(pConf.getOptions()).call();
//				.entity(new ParameterizedTypeReference<Map<String, String>>() {});

		return ret;

	}
	
	public Map<String, String> jsonPrompt(String promptKey, Map<String, Object> variables) throws ResourceNotFoundException {
		
		CallResponseSpec response = prompt(promptKey, variables);
		return response.entity(new ParameterizedTypeReference<Map<String, String>>() {});
	}

	
	public SamplePromptEntity entityPrompt(String promptKey, Map<String, Object> variables) throws ResourceNotFoundException {
		
		CallResponseSpec response = prompt(promptKey, variables);
		return response.entity(SamplePromptEntity.class);
	}
	
	
	

	/**
	 * Load the models, depending on the api type
	 */
	public void loadModels() {

		for (PromptConfig promptConfig : prompts.values()) {

			OpenAiChatModel model = switch (promptConfig.getAiService()) {
			case GenAiServiceType.OPEN_AI -> new OpenAiChatModel(openAiApi);
			case GenAiServiceType.PERPLEXITY -> new OpenAiChatModel(perplexityApi);
			default -> throw new IllegalArgumentException("Unexpected value: " + promptConfig.getAiService());
			};

			models.put(promptConfig.getKey(), model);
		}

	}

	/**
	 * Load the prompts config files in memory
	 * 
	 * @param folderPath
	 */
	public void loadPrompts(String folderPath) {

		List<File> promptsFile = Arrays.asList(new File(folderPath).listFiles()).stream().filter(e -> e.getName().endsWith(".yml")).toList();

		promptsFile.forEach(f -> {
			try {
				PromptConfig pc = serialisationService.fromYaml(FileUtils.readFileToString(f, Charset.defaultCharset()), PromptConfig.class);
				this.prompts.put(pc.getKey(), pc);
			} catch (Exception e) {
				logger.error("Error while reading prompt config file : {}", f.getAbsolutePath(), e);
			}

		});

	}

}