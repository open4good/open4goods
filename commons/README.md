# Commons module

This module provides shared configuration classes and utilities used across the Open4goods project. It centralises helpers that do not fit other modules so the rest of the codebase stays lean.

## Features

* Common Spring configurations
* Utility services and helpers
* Reusable filters and interceptors
* Model objects shared between modules

The source layout follows the standard pattern described in the [root agents guide](../AGENTS.md):

```
org.open4goods.commons
├─ config
├─ controller
├─ service
├─ repository
├─ model
├─ util
└─ Application
```

## Build and test

Build the module from this directory:

```bash
mvn clean install
```

Run only the tests:

```bash
mvn test
```

For more information on the overall project, see the [root README](../README.md).
