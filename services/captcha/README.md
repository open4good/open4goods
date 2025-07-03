# Captcha Service

Validates hCaptcha challenges and assigns a Spring Security role on success.

## Features

- Verifies tokens against the hCaptcha API.
- Assigns the configured role to the authenticated user.
- Exposes a health indicator via Spring Boot Actuator.

## Configuration

```yaml
captcha:
  key: "site-key"
  secretKey: "secret-key"
  validRole: "ROLE_HUMAN"
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for more information.
This module is distributed under the [AGPL v3 license](../../LICENSE).
