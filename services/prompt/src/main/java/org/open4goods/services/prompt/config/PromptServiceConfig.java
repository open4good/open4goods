package org.open4goods.services.prompt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "gen-ai-config")
public class PromptServiceConfig {
	
	
	  /** Maximum total tokens per batch file (to avoid exceeding limits). */
    private int batchMaxTokens = 200_000_000;  // default 200k

    /** Folder path where batch files (input/output) and job IDs will be stored. */
    @NotBlank
    private String batchFolder = "/opt/open4goods/.cached/batch-ia/";

    /** The OpenAI Batch API endpoint. */
    private String batchApiEndpoint = "https://api.openai.com/v1/batches";

    /** Poll interval for batch status checks. */
    private Duration batchPollInterval = Duration.ofSeconds(30);
    
	/**
	 * The folder to the yaml prompt files 
	 */
	private String promptsTemplatesFolder;
	
	private boolean cacheTemplates = false;
	
	private boolean enabled = false;
	
	/**
	 * Flag to enable recording of prompt responses as mocks.
	 */
	private boolean recordEnabled = false;
	
	/**
	 * Folder path where recorded prompt responses (mocks) are stored.
	 */
	private String recordFolder;

	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getPromptsTemplatesFolder() {
		return promptsTemplatesFolder;
	}
	public void setPromptsTemplatesFolder(String promptsTemplatesFoler) {
		this.promptsTemplatesFolder = promptsTemplatesFoler;
	}
	public boolean isCacheTemplates() {
		return cacheTemplates;
	}
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}
	
	public boolean isRecordEnabled() {
	    return recordEnabled;
	}
	
	public void setRecordEnabled(boolean recordEnabled) {
	    this.recordEnabled = recordEnabled;
	}
	
	public String getRecordFolder() {
	    return recordFolder;
	}
	
	public void setRecordFolder(String recordFolder) {
	    this.recordFolder = recordFolder;
	}
	public int getBatchMaxTokens() {
		return batchMaxTokens;
	}
	public void setBatchMaxTokens(int batchMaxTokens) {
		this.batchMaxTokens = batchMaxTokens;
	}
	public String getBatchFolder() {
		return batchFolder;
	}
	public void setBatchFolder(String batchFolder) {
		this.batchFolder = batchFolder;
	}
    public String getBatchApiEndpoint() {
        return batchApiEndpoint;
    }
    public void setBatchApiEndpoint(String batchApiEndpoint) {
        this.batchApiEndpoint = batchApiEndpoint;
    }

    public Duration getBatchPollInterval() {
        return batchPollInterval;
    }

    public void setBatchPollInterval(Duration batchPollInterval) {
        this.batchPollInterval = batchPollInterval;
    }
    
    /**
     * JSON content for Google Cloud authentication (SA key).
     */
    private String googleApiJson;

    public String getGoogleApiJson() {
        return googleApiJson;
    }

    public void setGoogleApiJson(String googleApiJson) {
        this.googleApiJson = googleApiJson;
    }
    
    /**
     * Map of string replacements to apply to generated text.
     * Key: target string, Value: replacement string
     */
    private java.util.Map<String, String> replacements;

    public java.util.Map<String, String> getReplacements() {
        return replacements;
    }

    public void setReplacements(java.util.Map<String, String> replacements) {
        this.replacements = replacements;
    }
}
