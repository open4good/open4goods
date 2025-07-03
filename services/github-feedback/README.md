# GitHub Feedback Service

Publishes feedback issues to GitHub and manages per‑IP voting quotas.

## Features

- Create "bug" or "idea" issues through the GitHub API.
- Limit votes per IP and store total votes via comments.
- Health indicator checks repository connectivity.

## Configuration

```yaml
feedback:
  github:
    accessToken: "your-token"
    organization: "open4good"
    repo: "open4goods"
    user: "bot-user"
  voting:
    maxVotesPerIpPerDay: 5
    requiredLabel: "votable"
    defaultVotable: true
```

## Build & Test

```bash
mvn clean install
mvn test
```

## Project Links

See the [main open4goods project](../../README.md) for more information.
This module is released under the [AGPL v3 license](../../LICENSE).
