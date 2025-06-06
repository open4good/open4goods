# open4goods Agents Guide

This file guides AI agents and other collaborators working with the repository.
It summarizes the project layout and the conventions you should follow when contributing code or documentation.

## Project structure

This project is a maven multi module project.

- `/commons` – shared utilities and configurations
- `/crawler` – web crawler component. Can be distibuted as executable spring jar, and is orchestrated with the api component. A crawler in embeded in the API component 
- `/model` – domain model classes
- `/verticals` – YAML configuration for product verticals
- `/api` – main Spring Boot API exposing REST endpoints, distibuted as executable spring jar. Also runs the batches and handle the data pipelines that populate the elastic search instance
- `/ui` – Spring Boot UI, as a spring executable thymeleaf app. UI is based on an existing bootstrap 5 kit and jquery. Saas / gulp is used to build custom css components.
- `/admin` – admin web application built with Spring Boot Admin
- `/services` – collection of standalone Spring Boot services. Team is engaging a long term refactoring to extract services from monolythics projects (commons, ui, api) to move them in this "micro services" like scheme.
    - `captcha`
    - `evaluation`
    - `favicon`
    - `github-feedback`
    - `googlesearch`
    - `product-repository`
    - `prompt`
    - `remotefilecaching`
    - `review-generation`
    - `serialisation`
    - `urlfetching`


## Coding conventions

- Use **Java 21** and **Spring Boot 3** features where appropriate.
- Follow the existing 4‑space indentation and brace style.
- Keep class, method and variable names meaningful.
- Unit tests must accompany new features or fixes.
- Prefer `@Component`/`@Service` and constructor injection for Spring beans.
- **Javadoc:** Improve or generate detailed Javadoc comments at both the class and method levels, clearly explaining purpose and behavior.
- **Inline Comments:** Add or refine in-method comments to clarify complex logic and overall flow.
- generate or refine the `additional-spring-configuration-metadata.json` if relevant

**Code Quality and Safety**
   - Code clarity : Variables and methods renaming when needed
   - **Modern Java Features:**
     - Leverage Java records for immutable data structures where applicable (Java 16+).
     - Use sealed classes for well-defined type hierarchies if needed (Java 17+).
     - Employ pattern matching for more concise and readable type checks.
   - **Dependency Injection & Immutability:**
     - Use constructor injection with `final` fields.
   - **Security:** Identify and fix potential security issues.
   - **Performance:** Optimize code where performance gains are achievable.
   - **Readability & Maintainability:**
     - Adhere to SOLID principles.
     - Refactor methods/classes to reduce complexity and improve separation of concerns.
   - **Exception Management:**
     - Enhance error handling with custom exceptions if needed.
   **Logging Enhancements**
   - Improve logging by using SLF4J’s parameterized messages.
   - Add structured logging and ensure appropriate log levels (INFO, WARN, ERROR) are used.

  
   **Testing**
   - Create or improve tests, by writing comprehensive unit tests and integration tests.

   **Structural Changes**
    - Refactor or split code into multiple classes if it improves separation of concerns.
    - **Important:** If you require additional context (e.g., details on dependency classes or intended functionality), ask clarifying questions before making significant structural changes.

**Caching & Monitoring:**
  - Consider emiting actuator metrics if pertinent
  - consider implementing HealthCheck (by making the service implements HealthIndicator and overriding the health() method) if pertinent
  - Evaluate potential caching improvements using Spring Cache abstraction.





## Programmatic checks

Before submitting changes, run the Maven build which compiles and executes tests across all modules:

```bash
mvn clean install
```

Running this command at the repository root ensures all services are built and their test suites executed.

## Pull request guidelines

When creating a pull request:

1. Provide a clear description of the change and reference related issues.
2. Ensure `mvn clean install` completes successfully.
3. Limit each PR to a focused set of changes.
4. Include screenshots for any UI modifications in `/ui`.
5. Keep documentation updates in sync with code changes.