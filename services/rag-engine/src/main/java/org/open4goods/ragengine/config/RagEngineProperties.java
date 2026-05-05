package org.open4goods.ragengine.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the reusable RAG engine module.
 */
@ConfigurationProperties(prefix = "rag-engine")
@Validated
public class RagEngineProperties
{
    @Valid
    private Github github = new Github();

    @Valid
    private List<Provider> providers = new ArrayList<>();

    @Valid
    private UseCases useCases = new UseCases();

    public Github getGithub()
    {
        return github;
    }

    public void setGithub(final Github github)
    {
        this.github = github;
    }

    public List<Provider> getProviders()
    {
        return providers;
    }

    public void setProviders(final List<Provider> providers)
    {
        this.providers = providers;
    }

    public UseCases getUseCases()
    {
        return useCases;
    }

    public void setUseCases(final UseCases useCases)
    {
        this.useCases = useCases;
    }

    public static class Github
    {
        @NotBlank
        private String repository;

        private String branch = "main";

        @NotEmpty
        private List<String> includePatterns = new ArrayList<>(List.of("**/*.md"));

        private List<ExtensionTagMapping> extensionTagMappings = new ArrayList<>();

        private List<FolderTagMapping> folderTagMappings = new ArrayList<>();

        public String getRepository()
        {
            return repository;
        }

        public void setRepository(final String repository)
        {
            this.repository = repository;
        }

        public String getBranch()
        {
            return branch;
        }

        public void setBranch(final String branch)
        {
            this.branch = branch;
        }

        public List<String> getIncludePatterns()
        {
            return includePatterns;
        }

        public void setIncludePatterns(final List<String> includePatterns)
        {
            this.includePatterns = includePatterns;
        }

        public List<ExtensionTagMapping> getExtensionTagMappings()
        {
            return extensionTagMappings;
        }

        public void setExtensionTagMappings(final List<ExtensionTagMapping> extensionTagMappings)
        {
            this.extensionTagMappings = extensionTagMappings;
        }

        public List<FolderTagMapping> getFolderTagMappings()
        {
            return folderTagMappings;
        }

        public void setFolderTagMappings(final List<FolderTagMapping> folderTagMappings)
        {
            this.folderTagMappings = folderTagMappings;
        }
    }

    public static class Provider
    {
        @NotBlank
        private String name;

        @NotBlank
        private String endpoint;

        @NotBlank
        private String apiKey;

        @NotBlank
        private String model;

        private Duration timeout = Duration.ofSeconds(30);

        public String getName()
        {
            return name;
        }

        public void setName(final String name)
        {
            this.name = name;
        }

        public String getEndpoint()
        {
            return endpoint;
        }

        public void setEndpoint(final String endpoint)
        {
            this.endpoint = endpoint;
        }

        public String getApiKey()
        {
            return apiKey;
        }

        public void setApiKey(final String apiKey)
        {
            this.apiKey = apiKey;
        }

        public String getModel()
        {
            return model;
        }

        public void setModel(final String model)
        {
            this.model = model;
        }

        public Duration getTimeout()
        {
            return timeout;
        }

        public void setTimeout(final Duration timeout)
        {
            this.timeout = timeout;
        }
    }

    public static class UseCases
    {
        @NotBlank
        private String chatProvider;

        @NotBlank
        private String embeddingProvider;

        @NotBlank
        private String summaryProvider;

        public String getChatProvider()
        {
            return chatProvider;
        }

        public void setChatProvider(final String chatProvider)
        {
            this.chatProvider = chatProvider;
        }

        public String getEmbeddingProvider()
        {
            return embeddingProvider;
        }

        public void setEmbeddingProvider(final String embeddingProvider)
        {
            this.embeddingProvider = embeddingProvider;
        }

        public String getSummaryProvider()
        {
            return summaryProvider;
        }

        public void setSummaryProvider(final String summaryProvider)
        {
            this.summaryProvider = summaryProvider;
        }
    }

    public record ExtensionTagMapping(@NotBlank String extension, @NotEmpty List<String> tags)
    {
    }

    public record FolderTagMapping(@NotBlank String folderPattern, @NotEmpty List<String> tags)
    {
    }
}
