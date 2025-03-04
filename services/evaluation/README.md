# Evaluation Microservice

This microservice provides evaluation functionality for products using Spring Expression Language (SpEL) and Thymeleaf templates.

## Features

- **SpEL Evaluations:** Validate and compute product properties using trusted SpEL expressions.
- **Thymeleaf Template Evaluations:** Generate dynamic strings based on product data with Thymeleaf templates.
- **Configuration Driven:** Template caching is configurable via YAML with Spring IDE metadata for auto-completion.
- **Robust Error Handling:** Critical failures in SpEL and template evaluations are logged at the ERROR level, and unresolved template variables raise a custom exception.
- **Unit Testing:** Tests are written with Mockito and bootstrapped with a dedicated test configuration.

## Directory Structure

```
.
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── org
│   │   │       └── open4goods
│   │   │           └── evaluation
│   │   │               ├── config
│   │   │               │   └── EvaluationProperties.java
│   │   │               ├── exception
│   │   │               │   └── TemplateEvaluationException.java
│   │   │               └── service
│   │   │                   └── EvaluationService.java
│   │   └── resources
│   └── test
│       ├── java
│       │   └── org
│       │       └── open4goods
│       │           └── evaluation
│       │               └── service
│       │                   └── EvaluationServiceTest.java
│       └── resources
│           └── application-test.yml
```

## Configuration

The microservice reads its configuration from YAML files. In particular, the template caching property is configured via:

```yaml
evaluation:
  template:
    cacheable: true
```

This property controls whether Thymeleaf template caching is enabled. IDE metadata is provided for enhanced Spring configuration support.

## How to Build

Use Maven to build the project:

```bash
mvn clean install
```

## Running

Run the microservice as a Spring Boot application using your preferred method.

## How to Use

The `EvaluationService` is a Spring-managed service that can be injected into your controllers or other services. Here are some example usages:

```java
import org.open4goods.evaluation.service.EvaluationService;
import org.open4goods.evaluation.exception.TemplateEvaluationException;
import org.open4goods.model.product.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductController {

    private final EvaluationService evaluationService;

    @Autowired
    public ProductController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    public void evaluateProduct(Product product) {
        // Evaluate a condition using SpEL (e.g., check if the product price is greater than 100)
        boolean isValid = evaluationService.spelEval(product, "p.price > 100");
        System.out.println("Product valid: " + isValid);

        // Compute a string value using SpEL (e.g., generate a product identifier)
        String computedId = evaluationService.spelCompute(product, "'Product-' + p.id");
        System.out.println("Computed Product ID: " + computedId);

        // Evaluate a Thymeleaf template using product data
        try {
            String evaluatedTemplate = evaluationService.thymeleafEval(product, "Product Name: [[${p.name}]]");
            System.out.println(evaluatedTemplate);
        } catch (TemplateEvaluationException ex) {
            // Handle unresolved variable exception
            System.err.println("Error evaluating template: " + ex.getMessage());
        }
    }
}
```

You can also pass a map of additional parameters to the Thymeleaf evaluation:

```java
Map<String, Object> params = new HashMap<>();
params.put("customMessage", "Welcome to our product service!");
String result = evaluationService.thymeleafEval(params, "Message: [[${customMessage}]]");
System.out.println(result);
```

## Testing

Unit tests are provided in the `src/test` directory. To run tests, execute:

```bash
mvn test
```

## Logging

Critical failures in SpEL and Thymeleaf evaluations are logged at the `ERROR` level for better traceability.

## Contributing

Contributions are welcome. Please ensure that any new code is accompanied by appropriate unit tests and documentation.
