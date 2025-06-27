# XWiki Spring Boot Starter

This module provides helper services and configuration to consume the XWiki REST API within a Spring Boot application.  It exposes a set of beans for fetching pages, attachments and user information and can also act as a Spring `AuthenticationProvider`.

## Provided Services

* **XwikiFacadeService** – high level facade combining the read/object/html services.
* **XWikiReadService** – access pages, page lists and attachments via REST calls.
* **XWikiHtmlService** – fetch HTML content from the XWiki web endpoints.
* **XWikiObjectService** – retrieve page properties and objects.
* **XWikiAuthenticationService** – authenticate against XWiki and obtain user groups.
* **XwikiAuthenticationProvider** – Spring Security provider delegating to `XWikiAuthenticationService`.

All services are auto-configured by `XWikiServiceConfiguration` when the starter is on the classpath.

## Configuration Properties

Properties are loaded under the prefix `xwiki` and can be configured in `application.yml`:

| Property | Description | Default |
|----------|-------------|---------|
| `xwiki.baseUrl` | Base URL to the XWiki instance. | – |
| `xwiki.username` | Username used for API calls. | – |
| `xwiki.password` | Password used for API calls. | – |
| `xwiki.httpsOnly` | Force HTTPS for all generated URLs. | `false` |
| `xwiki.media` | Media format requested from the API. | `json` |
| `xwiki.apiEntrypoint` | REST entry point path. | `rest` |
| `xwiki.apiWiki` | Target wiki name. | `xwiki` |

## Sample `application.yml`

```yaml
xwiki:
    baseUrl: "https://wiki.example.com"
    username: "api-user"
    password: "secret"
    httpsOnly: true
    media: "json"
    apiEntrypoint: "rest"
    apiWiki: "xwiki"
```

## Usage Example

Include the dependency in your project:

```xml
<dependency>
    <groupId>org.open4goods</groupId>
    <artifactId>o4g-xwiki-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Autowire `XwikiFacadeService` in your Spring components:

```java
@Autowired
private XwikiFacadeService xwikiFacadeService;

public void loadPage() {
    FullPage page = xwikiFacadeService.getFullPage("MySpace:MyPage");
    // use page.getHtmlContent(), page.getWikiPage(), ...
}
```

Build the module with:

```bash
mvn -pl services/xwiki-spring-boot-starter -am clean install
```
