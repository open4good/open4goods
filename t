* Directory structure *
- pom.xml
- src
--- main
---- java
----- org
------ open4goods
------- services
-------- prompt
--------- config
---------- GenAiServiceType.java
---------- PromptConfig.java
--------- dto
---------- PromptResponse.java
--------- service
---------- GenAiService.java
---- resources
--- test
---- java
---- resources

* Files content *

** [services/prompt//pom.xml] **
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.open4goods</groupId>
    <artifactId>org.open4goods</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </parent>
  <artifactId>prompt</artifactId>
  
  
    <dependencies>
       


       <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
        
                
    <dependency>
        <groupId>commons-codec</groupId>
        <artifactId>commons-codec</artifactId>
    </dependency>
        
    <dependency>
        <groupId>org.open4goods</groupId>
        <artifactId>serialisation</artifactId>
        <version>${global.version}</version>
    </dependency> 
        
    <dependency>
        <groupId>org.open4goods</groupId>
        <artifactId>evaluation</artifactId>
        <version>${global.version}</version>
    </dependency> 
       
    </dependencies>
  
  
</project>

** [services/prompt//src/main/java/org/open4goods/services/prompt/service/GenAiService.java] **
package org.open4goods.services.prompt.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.groovy.parser.antlr4.util.StringUtils;
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
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public PromptResponse<CallResponseSpec> prompt(String promptKey, Map<String, Object> variables) throws ResourceNotFoundException, JsonParseException, JsonMappingException, IOException {

		// TODO : Enable only if genaiconfig.enabled
		PromptConfig pConf =  getPromptConfig(promptKey);
		PromptResponse<CallResponseSpec> ret = new PromptResponse<ChatClient.CallResponseSpec>();

		if (null == pConf) {
			logger.error("PromptConfig {}  does not exists", promptKey);
			throw new ResourceNotFoundException("Prompt not found");
		}

		ret.setStart(System.currentTimeMillis());
		// Evaluating prompts,
		String systemPrompt = null == pConf.getSystemPrompt() ? "" : evaluationService.thymeleafEval(variables, pConf.getSystemPrompt());
		String userPrompt = evaluationService.thymeleafEval(variables, pConf.getUserPrompt());

		// Checking there are no remainings unevaluated expression

		// TODO(p2,safety)  Detect if remaining variables

		ChatClientRequestSpec chatRequest = ChatClient.create( models.get(promptKey)).prompt()
				.user(userPrompt)
				.options(pConf.getOptions());
		
		if (!StringUtils.isEmpty(systemPrompt)) {
			chatRequest = chatRequest.system(systemPrompt); 
		}
		
		CallResponseSpec genAiResponse = chatRequest.call();
		
		
		ret.setBody(genAiResponse);
		ret.setRaw(genAiResponse.content());
		
		// Cloning 
		PromptConfig updateConfig = serialisationService.fromJson(serialisationService.toJson(pConf), PromptConfig.class);
		updateConfig.setSystemPrompt(systemPrompt);
		updateConfig.setUserPrompt(userPrompt);
		ret.setPrompt(updateConfig);
		
		ret.setDuration(System.currentTimeMillis()-ret.getStart());
		
		return ret;

	}
	
	
	/**
	 * Prompt as json map
	 * @param promptKey
	 * @param variables
	 * @return
	 * @throws ResourceNotFoundException
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	public PromptResponse<Map<String, Object>> jsonPrompt(String promptKey, Map<String, Object> variables) throws ResourceNotFoundException, JsonParseException, JsonMappingException, IOException {
		
		PromptResponse<Map<String, Object>>  ret = new PromptResponse<Map<String, Object>>();
		
		PromptResponse<CallResponseSpec> internal = prompt(promptKey, variables);
		// Copy
		ret.setDuration(internal.getDuration());
		ret.setStart(internal.getStart());
		ret.setPrompt(internal.getPrompt());
		
		String response = internal.getBody().content();
		response = response.replace("```json", "").replace("```", "");
		ret.setRaw(response);
		try {
			ret.setBody(serialisationService.fromJsonTypeRef(response, new TypeReference<Map<String, Object>>() {}));
		} catch (Exception e) {
			logger.error("Unable to map to json structure : {} \n {}",e.getMessage(),response );		
			return null;
		}
		return ret;
	}

	
//	public EcoscoreResponse entityPrompt(String promptKey, Map<String, Object> variables) throws ResourceNotFoundException {
//		
//		CallResponseSpec response = prompt(promptKey, variables);
//		return response.entity(new ParameterizedTypeReference<EcoscoreResponse>() {});
//	}
//	
	
	

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

		
		try {
			File folder = new File(folderPath);
			if (folder.exists() && folder.isDirectory()) {
				
				List<File> promptsFile = Arrays.asList(folder.listFiles()).stream().filter(e -> e.getName().endsWith(".yml")).toList();
				
				promptsFile.forEach(f -> {
					PromptConfig pc = loadPrompt(f);
					this.prompts.put(pc.getKey(), pc);
					
				});
			} else {
				logger.error("!!!  Can not load prompts, folder {} is invalid", folderPath);
			}
		} catch (Exception e) {
			logger.error("Error loading prompts at {}",folderPath,  e);
		}

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
** [services/prompt//src/main/java/org/open4goods/services/prompt/dto/PromptResponse.java] **
package org.open4goods.services.prompt.dto;

import org.open4goods.services.prompt.config.PromptConfig;

public class PromptResponse<T> {
	
	/**
	 * The body of the response
	 */
	private T body;
	
	
	/**
	 * The resolved prompt (with variables resolved)
	 */
	private PromptConfig prompt = new PromptConfig();
	
	/**
	 * The response raw content
	 */
	private String raw;
	
	/**
	 * The duration this gen ai task has 
	 */
	private long duration;

	/**
	 * The date this generation occured, in epoch ms 
	 */
	private long start;
	
	
	public T getBody() {
		return body;
	}

	public void setBody(T body) {
		this.body = body;
	}

	public PromptConfig getPrompt() {
		return prompt;
	}

	public void setPrompt(PromptConfig prompt) {
		this.prompt = prompt;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}

	public String getRaw() {
		return raw;
	}

	public void setRaw(String raw) {
		this.raw = raw;
	}
	
	
	

}

** [services/prompt//src/main/java/org/open4goods/services/prompt/config/PromptConfig.java] **
package org.open4goods.services.prompt.config;

import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * Represents a prompt config (a chat model configuration and a chat model options) 
 */
public class PromptConfig {
	
	/**
	 * The unique key used to identify this prompt config
	 */
	private String key;
	
	/**
	 * The Gen ai service to use
	 */
	private GenAiServiceType aiService;
	
	/**
	 * The system prompt
	 */
	private String systemPrompt;
	
	
	/**
	 * The user prompt
	 */
	private String userPrompt;
	
	
	/**
	 * The options (temperature, top k..)given to the chat model
	 */
	private OpenAiChatOptions options;
	

	public String getSystemPrompt() {
		return systemPrompt;
	}


	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}


	public String getUserPrompt() {
		return userPrompt;
	}


	public void setUserPrompt(String userPrompt) {
		this.userPrompt = userPrompt;
	}


	public OpenAiChatOptions getOptions() {
		return options;
	}


	public void setOptions(OpenAiChatOptions options) {
		this.options = options;
	}


	public String getKey() {
		return key;
	}


	public void setKey(String key) {
		this.key = key;
	}


	public GenAiServiceType getAiService() {
		return aiService;
	}


	public void setAiService(GenAiServiceType aiService) {
		this.aiService = aiService;
	}
	

	
	
}

** [services/prompt//src/main/java/org/open4goods/services/prompt/config/GenAiServiceType.java] **
package org.open4goods.services.prompt.config;

public enum GenAiServiceType {
	OPEN_AI,
	PERPLEXITY

}

