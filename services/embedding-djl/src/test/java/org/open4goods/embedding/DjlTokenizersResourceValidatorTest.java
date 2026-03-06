package org.open4goods.embedding;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.net.URL;
import java.net.URLClassLoader;

import org.junit.jupiter.api.Test;
import org.open4goods.embedding.config.DjlTokenizersResourceValidator;

/**
 * Tests startup validation for tokenizers metadata resources.
 */
class DjlTokenizersResourceValidatorTest
{
    @Test
    void shouldValidateResourcesForRuntimeClassLoaders()
    {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

        assertThatCode(() -> DjlTokenizersResourceValidator.validateResources(contextClassLoader, systemClassLoader))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldFailFastWhenTokenizerResourcesAreMissing()
    {
        try (URLClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null))
        {
            assertThatIllegalStateException()
                    .isThrownBy(() -> DjlTokenizersResourceValidator.validateResources(emptyClassLoader, emptyClassLoader))
                    .withMessageContaining("Missing required DJL tokenizer metadata resource")
                    .withMessageContaining("native/lib/tokenizers.properties");
        }
        catch (Exception e)
        {
            throw new AssertionError("Unable to create test classloader", e);
        }
    }
}
