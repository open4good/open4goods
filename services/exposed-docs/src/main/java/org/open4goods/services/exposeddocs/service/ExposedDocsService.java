package org.open4goods.services.exposeddocs.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.open4goods.services.exposeddocs.config.ExposedDocsProperties;
import org.open4goods.services.exposeddocs.dto.ExposedDocsCategoryDto;
import org.open4goods.services.exposeddocs.dto.ExposedDocsContentDto;
import org.open4goods.services.exposeddocs.dto.ExposedDocsOverviewDto;
import org.open4goods.services.exposeddocs.dto.ExposedDocsSearchResultDto;
import org.open4goods.services.exposeddocs.dto.ExposedDocsTreeNodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

/**
 * Service that indexes embedded documentation resources and provides lookup APIs
 * for navigation, content retrieval, and in-memory search.
 */
@Service
@ConditionalOnProperty(prefix = "exposed-docs", name = "service-enabled", havingValue = "true", matchIfMissing = true)
public class ExposedDocsService
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposedDocsService.class);
    private static final Comparator<ExposedDocsTreeNodeDto> NODE_ORDER = Comparator
            .comparing(ExposedDocsTreeNodeDto::directory)
            .reversed()
            .thenComparing(ExposedDocsTreeNodeDto::name, String.CASE_INSENSITIVE_ORDER);

    private final ExposedDocsProperties properties;
    private final ResourcePatternResolver resolver;
    private final Map<String, CategoryIndex> categoryIndex = new ConcurrentHashMap<>();

    public ExposedDocsService(ExposedDocsProperties properties)
    {
        this.properties = properties;
        this.resolver = new PathMatchingResourcePatternResolver();
    }

    /**
     * Loads the documentation resources into memory for fast lookup.
     */
    @PostConstruct
    public void initialize()
    {
        refreshIndex();
    }

    /**
     * Reloads the resource index from the classpath based on configuration.
     */
    public void refreshIndex()
    {
        Map<String, CategoryIndex> updatedIndex = new LinkedHashMap<>();
        for (ExposedDocsProperties.Category category : properties.getCategories()) {
            String normalizedRoot = normalizeRoot(category.getClasspathRoot());
            List<String> extensions = normalizeExtensions(category.getExtensions());
            List<ResourceDescriptor> resources = resolveResources(normalizedRoot, extensions, category.getId());
            Map<String, ResourceDescriptor> byPath = resources.stream()
                    .collect(Collectors.toMap(ResourceDescriptor::path, descriptor -> descriptor));
            updatedIndex.put(category.getId(), new CategoryIndex(category, normalizedRoot, resources, byPath));
            LOGGER.info("Indexed {} resources for category '{}'", resources.size(), category.getId());
        }
        categoryIndex.clear();
        categoryIndex.putAll(updatedIndex);
    }

    /**
     * Returns the overview of all exposed resource categories.
     *
     * @return overview DTO listing all categories
     */
    public ExposedDocsOverviewDto getOverview()
    {
        List<ExposedDocsCategoryDto> categories = categoryIndex.values().stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList());
        return new ExposedDocsOverviewDto(categories);
    }

    /**
     * Builds a tree view of a resource category.
     *
     * @param categoryId category identifier
     * @return tree root
     */
    public ExposedDocsTreeNodeDto getCategoryTree(String categoryId)
    {
        CategoryIndex index = getCategoryIndex(categoryId);
        ExposedDocsTreeNodeDto root = new ExposedDocsTreeNodeDto(index.category().getLabel(), "", true, new ArrayList<>());
        for (ResourceDescriptor descriptor : index.resources()) {
            insertNode(root, descriptor.path());
        }
        sortTree(root);
        return root;
    }

    /**
     * Retrieves a resource content by category and path.
     *
     * @param categoryId category identifier
     * @param path relative path to the resource
     * @return resource content DTO
     */
    public ExposedDocsContentDto getContent(String categoryId, String path)
    {
        CategoryIndex index = getCategoryIndex(categoryId);
        ResourceDescriptor descriptor = index.byPath().get(path);
        if (descriptor == null) {
            throw new ExposedDocsNotFoundException("Resource not found for path: " + path);
        }
        String content = readResourceContent(descriptor.resource());
        return new ExposedDocsContentDto(categoryId, path, descriptor.extension(), content, descriptor.lastModified());
    }

    /**
     * Searches resources by query terms in path or content.
     *
     * @param query search query, optional
     * @param categories categories to search, optional
     * @param pathPrefix optional path prefix filter
     * @param searchContent whether to search within file content
     * @param includeContent whether to include file content in results
     * @return matching resources
     */
    public List<ExposedDocsSearchResultDto> search(String query,
                                                   List<String> categories,
                                                   String pathPrefix,
                                                   boolean searchContent,
                                                   boolean includeContent)
    {
        String normalizedQuery = normalizeQuery(query);
        Set<String> categoryFilter = categories == null || categories.isEmpty()
                ? new LinkedHashSet<>(categoryIndex.keySet())
                : new LinkedHashSet<>(categories);
        String normalizedPrefix = StringUtils.hasText(pathPrefix) ? pathPrefix.trim() : null;

        List<ExposedDocsSearchResultDto> results = new ArrayList<>();
        for (String categoryId : categoryFilter) {
            CategoryIndex index = getCategoryIndex(categoryId);
            for (ResourceDescriptor descriptor : index.resources()) {
                if (!matchesPrefix(descriptor.path(), normalizedPrefix)) {
                    continue;
                }
                boolean pathMatch = matchesQuery(descriptor.path(), normalizedQuery);
                boolean contentMatch = false;
                String content = null;
                if (searchContent || includeContent) {
                    content = readResourceContent(descriptor.resource());
                    if (searchContent) {
                        contentMatch = matchesQuery(content, normalizedQuery);
                    }
                }
                if (normalizedQuery == null || pathMatch || contentMatch) {
                    results.add(new ExposedDocsSearchResultDto(
                            categoryId,
                            descriptor.path(),
                            descriptor.filename(),
                            descriptor.extension(),
                            contentMatch,
                            includeContent ? content : null,
                            descriptor.lastModified()));
                }
            }
        }
        return results;
    }

    private CategoryIndex getCategoryIndex(String categoryId)
    {
        CategoryIndex index = categoryIndex.get(categoryId);
        if (index == null) {
            throw new ExposedDocsNotFoundException("Unknown category: " + categoryId);
        }
        return index;
    }

    private ExposedDocsCategoryDto toCategoryDto(CategoryIndex index)
    {
        return new ExposedDocsCategoryDto(
                index.category().getId(),
                index.category().getLabel(),
                "/exposed/" + index.category().getId(),
                index.classpathRoot(),
                index.category().getExtensions(),
                index.resources().size());
    }

    private List<ResourceDescriptor> resolveResources(String classpathRoot, List<String> extensions, String categoryId)
    {
        if (!StringUtils.hasText(classpathRoot) || extensions.isEmpty()) {
            return Collections.emptyList();
        }
        String extensionPattern = String.join(",", extensions);
        String pattern = String.format("classpath*:%s/**/*.{%s}", classpathRoot, extensionPattern);

        List<ResourceDescriptor> descriptors = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources(pattern);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                String relativePath = resolveRelativePath(resource, classpathRoot);
                if (!StringUtils.hasText(relativePath)) {
                    continue;
                }
                String extension = fileExtension(relativePath);
                if (!extensions.contains(extension)) {
                    continue;
                }
                descriptors.add(new ResourceDescriptor(
                        categoryId,
                        relativePath,
                        fileName(relativePath),
                        extension,
                        resource,
                        resolveLastModified(resource)));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to resolve resources for pattern {}", pattern, e);
        }
        return descriptors;
    }

    private void insertNode(ExposedDocsTreeNodeDto root, String path)
    {
        String[] parts = path.split("/");
        ExposedDocsTreeNodeDto current = root;
        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (currentPath.length() > 0) {
                currentPath.append("/");
            }
            currentPath.append(part);
            boolean isDirectory = i < parts.length - 1;
            current = findOrCreateChild(current, part, currentPath.toString(), isDirectory);
        }
    }

    private ExposedDocsTreeNodeDto findOrCreateChild(ExposedDocsTreeNodeDto parent,
                                                     String name,
                                                     String path,
                                                     boolean directory)
    {
        for (ExposedDocsTreeNodeDto child : parent.children()) {
            if (child.name().equals(name) && child.directory() == directory) {
                return child;
            }
        }
        ExposedDocsTreeNodeDto child = new ExposedDocsTreeNodeDto(name, path, directory, new ArrayList<>());
        parent.children().add(child);
        return child;
    }

    private void sortTree(ExposedDocsTreeNodeDto node)
    {
        node.children().sort(NODE_ORDER);
        node.children().forEach(this::sortTree);
    }

    private String normalizeRoot(String classpathRoot)
    {
        if (!StringUtils.hasText(classpathRoot)) {
            return "docs";
        }
        String root = classpathRoot.trim();
        if (root.startsWith("/")) {
            root = root.substring(1);
        }
        if (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }
        return root;
    }

    private List<String> normalizeExtensions(List<String> extensions)
    {
        if (extensions == null) {
            return Collections.emptyList();
        }
        return extensions.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.startsWith(".") ? value.substring(1) : value)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }

    private String resolveRelativePath(Resource resource, String classpathRoot) throws IOException
    {
        String url = resource.getURL().toString();
        String marker = "/" + classpathRoot + "/";
        int index = url.lastIndexOf(marker);
        if (index >= 0) {
            return url.substring(index + marker.length());
        }
        String fileName = resource.getFilename();
        return fileName != null ? fileName : "";
    }

    private String fileName(String path)
    {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    private String fileExtension(String path)
    {
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private Instant resolveLastModified(Resource resource)
    {
        try {
            return Instant.ofEpochMilli(resource.lastModified());
        } catch (IOException e) {
            LOGGER.debug("Unable to resolve last modified for resource", e);
            return null;
        }
    }

    private String readResourceContent(Resource resource)
    {
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Unable to read resource content", e);
            return "";
        }
    }

    private String normalizeQuery(String query)
    {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.toLowerCase(Locale.ROOT).trim();
    }

    private boolean matchesQuery(String value, String query)
    {
        if (query == null) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesPrefix(String value, String pathPrefix)
    {
        if (!StringUtils.hasText(pathPrefix)) {
            return true;
        }
        return value != null && value.startsWith(pathPrefix);
    }

    private record ResourceDescriptor(String categoryId,
                                      String path,
                                      String filename,
                                      String extension,
                                      Resource resource,
                                      Instant lastModified)
    {
        private ResourceDescriptor
        {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(resource, "resource");
        }
    }

    private record CategoryIndex(ExposedDocsProperties.Category category,
                                 String classpathRoot,
                                 List<ResourceDescriptor> resources,
                                 Map<String, ResourceDescriptor> byPath)
    {
    }
}
