# open4goods – Agents Guide (Root)

This guide defines **project-wide conventions** for all human contributors **and AI coding agents** working on the open4goods repository.  
Adhering to these rules keeps every sub-project consistent, maintainable, and predictable for automated tools (LLMs included).

> **Where to look next**  
> Each sub-module (e.g. `/api`, `/crawler`, `/services/*`) ships its own `agents.md` that may add or override rules.  
> The standards below are **mandatory everywhere** unless a module’s guide explicitly says otherwise.

---

## 1  Technology stack

| Area            | Standard                           |
|-----------------|------------------------------------|
| **JDK**         | Java 21                            |
| **Framework**   | Spring Boot 3.x                    |
| **Build tool**  | Maven (reactor / multi-module)     |
| **Testing**     | JUnit 5, AssertJ, Testcontainers   |

---

## 2  Standard package layout

Every module follows a *type-based* (layered) structure under  
`org.open4goods.<module>`:

```
org.open4goods.<module>
├─ config        ← @Configuration
├─ config.yml    ← @ConfigurationProperties, or yaml related configuration properties
├─ controller    ← @RestController / @Controller
├─ service       ← @Service
├─ repository    ← @Repository (Spring Data or custom DAO)
├─ dto           ← Records / simple POJOs for transport
├─ model         ← Domain entities / JPA @Entity
├─ util          ← Generic helpers (use sparingly)
└─ Application   ← <ModuleName>Application (@SpringBootApplication)
```

**Layering rule**

```
controller → service → repository
```

No class in a lower layer may depend on a higher one.

---

## 3  Naming conventions

| Layer / Role        | Suffix example                     |
|---------------------|------------------------------------|
| Main application    | `ApiApplication`                   |
| Controller          | `ProductController`                |
| Service (interface) | `ProductService`                   |
| Service impl        | `ProductServiceImpl` (or clearer)  |
| Repository          | `ProductRepository`, `ElasticProductRepository` |
| DTO                 | `ProductDto`, `CreateOrderRequest` |
| Config              | `WebSecurityConfig`                |
| Exception           | `ProductNotFoundException`         |

---

## 4  Spring stereotype usage

| Package        | Required annotation                             |
|----------------|-------------------------------------------------|
| `controller`   | `@RestController` (REST) or `@Controller` (MVC) |
| `service`      | `@Service`                                      |
| `repository`   | `@Repository` (or Spring Data interface)        |
| `config`       | `@Configuration` (+ `@ConfigurationProperties`) |
| Misc beans     | `@Component` (or a specialised stereotype)      |

- **Injection**: Constructor-based, `final` fields. Never use field injection.  
- **DTOs**: Prefer Java 21 records for immutability and zero boiler-plate.

---

## 5  Coding standards

- **Formatting**: 4-space indent, braces on a new line.  
- **Language features**: Records, sealed classes, pattern matching where useful.  
- **Error handling**: Custom exceptions + RFC 9457 / Problem-Detail responses.  
- **Logging**: SLF4J parameterised messages and structured logging.  
- **Tests**: Unit + integration tests for every feature or bug-fix.  
- **Javadoc**: Keep class/method Javadoc up-to-date.  
- **TODOs**: Resolve or raise an issue; none should remain in committed code.

---

## 7  Build & test

To compile **all modules**:

```bash
mvn clean install
```

To build a **single** module (example `api`):

```bash
mvn -pl api -am clean install
```

---

## 8  Pull-request checklist

1. Clear commit & PR description (**why** and **what**).  
2. `mvn clean install` passes locally.  
3. Tests added/updated; coverage not reduced.  
4. Docs (agents.md, README, Javadoc, spring-configuration-metadata.json) updated.  

---

## 9  Rationale

A predictable, enforced structure lowers cognitive load for humans and gives large-language models a deterministic environment in which to operate. This accelerates onboarding, reduces bugs, and keeps architecture sound as we migrate monolith parts to independent Spring Boot services (see the services/**) .

> **Questions?**  
> Open a GitHub Discussion or justify any deviation in your PR description.