package org.open4goods.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.open4goods.evaluation.config.EvaluationConfig;
import org.open4goods.evaluation.exception.TemplateEvaluationException;
import org.open4goods.model.attribute.ProductAttributes;
import org.open4goods.model.product.Product;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * Unit tests for {@link EvaluationService}.
 */
@TestPropertySource(locations = "classpath:application-test.yml")
public class EvaluationServiceTest {

    private EvaluationService evaluationService;

    // Mocks for Product and its attributes
    @Mock
    private Product productMock;

    @Mock
    private ProductAttributes productAttributesMock;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        // Create EvaluationProperties with default value (assumed to be provided by application-test.yml)
        EvaluationConfig properties = new EvaluationConfig();
        properties.setCacheable(true);

        // Initialize EvaluationService with injected properties.
        evaluationService = new EvaluationService(properties);

        // Set up product mock behavior.
        when(productMock.getAttributes()).thenReturn(productAttributesMock);
        // Simulate an empty referential attributes map by default.
        when(productAttributesMock.getReferentielAttributes()).thenReturn(Collections.emptyMap());
    }

    /**
     * Test SpEL evaluation that returns true.
     */
    @Test
    public void testSpelEvalTrue() {
        // Using a simple literal expression that always returns true.
        Boolean result = evaluationService.spelEval(productMock, "true");
        assertTrue(result);
    }

    /**
     * Test SpEL computation that returns a string.
     */
    @Test
    public void testSpelCompute() {
        // Using a simple expression that returns a string literal.
        String result = evaluationService.spelCompute(productMock, "'computedString'");
        assertEquals("computedString", result);
    }

    /**
     * Test Thymeleaf evaluation with a valid template.
     */
    @Test
    public void testThymeleafEvalValidTemplate() {
        // Template that uses a parameter from the provided map.
        Map<String, Object> params = new HashMap<>();
        params.put("name", "TestProduct");
        String template = "Product name: [[${name}]]";
        String result = evaluationService.thymeleafEval(params, template);
        assertEquals("Product name: TestProduct", result);
    }

    /**
     * Test Thymeleaf evaluation that should fail due to unresolved variable.
     */
    @Test
    public void testThymeleafEvalUnresolvedVariable() {
        // Template with an unresolved variable (no value provided).
        String template = "Product vertical: [[${vertical}]]";
        TemplateEvaluationException exception = assertThrows(TemplateEvaluationException.class, () -> {
            evaluationService.thymeleafEval(productMock, template);
        });
        assertTrue(exception.getMessage().contains("unresolved variables"));
    }

    /**
     * Minimal test configuration to bootstrap the Spring context.
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {"org.open4goods.evaluation"})
    static class TestConfig {
        // This class remains empty; its purpose is to trigger component scanning in the
        // org.open4goods.evaluation package and enable auto-configuration.
    }
}
