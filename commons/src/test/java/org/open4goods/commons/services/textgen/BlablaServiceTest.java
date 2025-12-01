package org.open4goods.commons.services.textgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.product.Product;
import org.open4goods.services.evaluation.service.EvaluationService;

class BlablaServiceTest {

    private EvaluationService evaluationService;
    private BlablaService blablaService;

    @Test
    void generateBlablaShouldBeDeterministicForSameHash() throws InvalidParameterException {
        // Mock thymeleafEval to behave as an identity function for the template text
        Mockito.when(evaluationService.thymeleafEval(Mockito.any(Product.class), Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Product product = new Product();
        product.setId(42L);
        String template = "Hello ||Alice|Bob|John|Toto|| !";

        for (int i = 0; i < 100; i++) {

        	String firstResult = blablaService.generateBlabla(template, product);
        	String secondResult = blablaService.generateBlabla(template, product);
        	Assertions.assertEquals(firstResult, secondResult);
            Assertions.assertTrue(firstResult.startsWith("Hello "));
        }

        try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			fail(e);
		}

        product.setId(100L);

        for (int i = 0; i < 100; i++) {
        	String firstResult = blablaService.generateBlabla(template, product);
        	String secondResult = blablaService.generateBlabla(template, product);
        	Assertions.assertEquals(firstResult, secondResult);
            Assertions.assertTrue(firstResult.startsWith("Hello "));
        }
    }

    @BeforeEach
    void setUp() {
        evaluationService = mock(EvaluationService.class);
        blablaService = new BlablaService(evaluationService);
    }

    @Test
    void fastOrIsDeterministicAndVariedAcrossSeeds() {
        final String template = "Start ||red|blue|| and ||small|large|| end";

        final String deterministic = blablaService.fastOr(template, new BlaBlaSecGenerator(42));
        final String deterministicAgain = blablaService.fastOr(template, new BlaBlaSecGenerator(42));

        assertThat(deterministic).isEqualTo(deterministicAgain);
        assertThat(deterministic).containsAnyOf("red", "blue");
        assertThat(deterministic).containsAnyOf("small", "large");

        final String differentSeed = blablaService.fastOr(template, new BlaBlaSecGenerator(43));
        assertThat(differentSeed).isNotEqualTo(deterministic);
    }

    @Test
    void fastOrOptionalSegmentsCoverBothBranchesDeterministically() {
        final String template = "Hello|| world||!";

        String included = null;
        Integer includedSeed = null;
        String excluded = null;
        Integer excludedSeed = null;
        int seed = 0;
        while (seed < 100 && (included == null || excluded == null)) {
            final BlaBlaSecGenerator generator = new BlaBlaSecGenerator(seed);
            final String result = blablaService.fastOr(template, generator);

            if (result.contains("world")) {
                included = result;
                includedSeed = seed;
            } else {
                excluded = result;
                excludedSeed = seed;
            }
            seed++;
        }

        assertThat(included).isNotNull();
        assertThat(excluded).isNotNull();
        assertThat(included)
                .isEqualTo(blablaService.fastOr(template, new BlaBlaSecGenerator(includedSeed)));
        assertThat(excluded)
                .isEqualTo(blablaService.fastOr(template, new BlaBlaSecGenerator(excludedSeed)));
    }

    @Test
    void generateBlablaReturnsEmptyWhenTemplateEvaluationFails() throws InvalidParameterException {
        final String template = "text";
        when(evaluationService.thymeleafEval((org.open4goods.model.product.Product) null, template)).thenReturn(null);

        final String result = blablaService.generateBlabla(template, null);

        assertThat(result).isEmpty();
    }

    @Test
    void generatorRejectsNonPositiveBounds() {
        final BlaBlaSecGenerator generator = new BlaBlaSecGenerator(12);

        assertThatThrownBy(() -> generator.getNextAlea(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }
}
