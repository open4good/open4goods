package org.open4goods.commons.services.ai;

import java.io.File;
import java.io.IOException;
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
import org.open4goods.commons.model.EcoscoreResponse;
import org.open4goods.commons.services.EvaluationService;
import org.open4goods.commons.services.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.ParameterizedTypeReference;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

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

		// TODO : Enable only if genaiconfig.enabled
		PromptConfig pConf =  getPromptConfig(promptKey);

		if (null == pConf) {
			logger.error("PromptConfig {}  does not exists", promptKey);
			throw new ResourceNotFoundException("Prompt not found");
		}

		// Evaluating prompts,
		String systemPrompt = evaluationService.thymeleafEval(variables, pConf.getSystemPrompt());
		String userPrompt = evaluationService.thymeleafEval(variables, pConf.getUserPrompt());

		// Checking there are no remainings unevaluated expression

	// TODO(p2,safety)  Detect if remaining variables

		CallResponseSpec ret = ChatClient.create( models.get(promptKey)).prompt()
				.system(systemPrompt)
				.user(userPrompt)
				.options(pConf.getOptions()).call();

		return ret;

	}
	
	
	/**
	 * Prompt as json map
	 * @param promptKey
	 * @param variables
	 * @return
	 * @throws ResourceNotFoundException
	 */
	public Map<String, Object> jsonPrompt(String promptKey, Map<String, Object> variables) throws ResourceNotFoundException {
		String response = prompt(promptKey, variables).content();
		response = response.replace("```json", "").replace("```", "");
		try {
			return serialisationService.fromJsonTypeRef(response, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			logger.error("Unable to map to json structure : {} \n {}",e.getMessage(),response );		
			return new HashMap<String, Object>();
		}
	}

	
	public EcoscoreResponse entityPrompt(String promptKey, Map<String, Object> variables) throws ResourceNotFoundException {
		
		CallResponseSpec response = prompt(promptKey, variables);
		return response.entity(new ParameterizedTypeReference<EcoscoreResponse>() {});
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
	 * 
	 * @param promptKey
	 * @return
	 */
	private PromptConfig getPromptConfig(String promptKey) {
		if (genAiConfig.isCacheTemplates()) {
			return prompts.get(promptKey);
		} else {
			
			String path = genAiConfig.getPromptsTemplatesFolder()+"/"+promptKey+".yml";
			return loadPrompt(new File(path));
		}
	}

	
	/**
	 * Load all the prompts config files in memory
	 * 
	 * @param folderPath
	 */
	public void loadPrompts(String folderPath) {

		List<File> promptsFile = Arrays.asList(new File(folderPath).listFiles()).stream().filter(e -> e.getName().endsWith(".yml")).toList();

		promptsFile.forEach(f -> {
			PromptConfig pc = loadPrompt(f);
			this.prompts.put(pc.getKey(), pc);

		});

	}

	/**
	 * Load a PromptConfig from a file
	 * @param f
	 * @return
	 */
	private PromptConfig loadPrompt(File f) {
		PromptConfig pc = null;
		try {
			pc = serialisationService.fromYaml(FileUtils.readFileToString(f, Charset.defaultCharset()), PromptConfig.class);
			
		} catch (Exception e) {
			logger.error("Error while reading prompt config file : {}", f.getAbsolutePath(), e);
		}
		return pc;
	}

}