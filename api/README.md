# API Module

This module exposes the REST endpoints for the open4goods platform.

## Build

From the repository root run:

```bash
mvn -pl api -am clean install
```

Or from this directory:

```bash
mvn clean install
```

## Run

After building, start the application with:

```bash
java -Dspring.profiles.active=dev -jar target/api-<version>.jar
```

The API listens on port `8081` by default.

## Test

Execute the test suite only for this module with:

```bash
mvn -pl api test
```

