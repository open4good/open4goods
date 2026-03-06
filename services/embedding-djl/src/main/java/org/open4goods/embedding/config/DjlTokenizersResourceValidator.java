package org.open4goods.embedding.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that DJL tokenizers metadata resources are visible from runtime classloaders.
 * <p>
 * Spring Boot executable jars can change resource lookup behaviour for nested jars. This
 * validator performs an early startup check so deployment issues fail fast with diagnostics
 * instead of surfacing later during asynchronous model loading.
 * </p>
 */
public class DjlTokenizersResourceValidator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DjlTokenizersResourceValidator.class);

    private static final String NATIVE_PROPERTIES_PATH = "native/lib/tokenizers.properties";
    private static final String ENGINE_PROPERTIES_PATH = "tokenizers-engine.properties";

    /**
     * Creates a validator and immediately performs the resource validation.
     */
    public DjlTokenizersResourceValidator()
    {
        validateResources(Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader());
    }

    /**
     * Validates both required tokenizers property resources.
     *
     * @param contextClassLoader current thread context classloader
     * @param systemClassLoader JVM system classloader
     */
    public static void validateResources(ClassLoader contextClassLoader, ClassLoader systemClassLoader)
    {
        validateResource(NATIVE_PROPERTIES_PATH, contextClassLoader, systemClassLoader);
        validateResource(ENGINE_PROPERTIES_PATH, contextClassLoader, systemClassLoader);
    }

    private static void validateResource(String resourcePath, ClassLoader contextClassLoader, ClassLoader systemClassLoader)
    {
        URL contextUrl = contextClassLoader == null ? null : contextClassLoader.getResource(resourcePath);
        URL systemUrl = systemClassLoader == null ? null : systemClassLoader.getResource(resourcePath);

        if (contextUrl != null || systemUrl != null)
        {
            LOGGER.info(
                    "Validated DJL tokenizer resource '{}' (contextClassLoaderUrl={}, systemClassLoaderUrl={})",
                    resourcePath,
                    contextUrl,
                    systemUrl);
            return;
        }

        List<String> diagnostics = new ArrayList<>();
        diagnostics.add("resource=" + resourcePath);
        diagnostics.add("contextClassLoader=" + classLoaderName(contextClassLoader));
        diagnostics.add("systemClassLoader=" + classLoaderName(systemClassLoader));

        throw new IllegalStateException(
                "Missing required DJL tokenizer metadata resource. "
                        + "This commonly happens when running from packaged executable jars with nested dependency resources. "
                        + String.join(", ", diagnostics));
    }

    private static String classLoaderName(ClassLoader classLoader)
    {
        return classLoader == null ? "null" : classLoader.getClass().getName();
    }
}
