# open4goods Agents Guide

This file guides AI agents and other collaborators working with the repository. It summarizes the project layout and the conventions you should follow when contributing code or documentation.

## Project structure

- `/commons` – shared utilities and configurations
- `/crawler` – web crawler component
- `/model` – domain model classes
- `/verticals` – YAML configuration for product verticals
- `/api` – main Spring Boot API exposing REST endpoints. Also runs the batches and handle the data pipelines that populate the elastic search instance
- `/ui` – Spring Boot UI, as a spring thymeleaf app. Ui is based on an existing bootstrap 5 kit and jquery. Saas / gulp is used to build custom css components.
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

Most modules are regular Maven projects packaged as JARs.

## Coding conventions

- Use **Java 21** and **Spring Boot 3** features where appropriate.
- Follow the existing 4‑space indentation and brace style.
- Keep class, method and variable names meaningful.
- Document complex logic with Javadoc.
- Document code with inline comment
- Unit tests must accompany new features or fixes.
- Prefer `@Component`/`@Service` and constructor injection for Spring beans.


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