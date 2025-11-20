package org.open4goods.commons.services.textgen;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.product.Product;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.mockito.Mockito;

class BlablaServiceTest {

    private final EvaluationService evaluationService = Mockito.mock(EvaluationService.class);
    private final BlablaService blablaService = new BlablaService(evaluationService);

    @Test
    void generateBlablaShouldBeDeterministicForSameHash() throws InvalidParameterException {
        // Mock thymeleafEval to behave as an identity function for the template text
        Mockito.when(evaluationService.thymeleafEval(Mockito.any(Product.class), Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Product product = new Product();
        product.setId(42L);
        String template = "Hello ||Alice|Bob||!";

        String firstResult = blablaService.generateBlabla(template, product);
        String secondResult = blablaService.generateBlabla(template, product);

        Assertions.assertEquals(firstResult, secondResult);
        Assertions.assertTrue(firstResult.startsWith("Hello "));
    }

    @Test
    void fastOrUsesSameGeneratorSeedConsistently() {
        String template = "Pick ||Yes|No|| option.";

        // Use the same seed to ensure deterministic choices across generator instances
        String resultWithFirstGenerator = blablaService.fastOr(template, new BlaBlaSecGenerator(12345));
        String resultWithSecondGenerator = blablaService.fastOr(template, new BlaBlaSecGenerator(12345));

        Assertions.assertEquals(resultWithFirstGenerator, resultWithSecondGenerator);
        Assertions.assertTrue(resultWithFirstGenerator.startsWith("Pick "));
    }

    @Test
    void generateBlablaShouldRejectEmptyInput() {
        Product product = new Product();

        Assertions.assertThrows(InvalidParameterException.class, () -> blablaService.generateBlabla("", product));
    }
}
