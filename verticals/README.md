# open4goods-verticals

This module defines **product verticals** and their eco-score rules through YAML files. Vertical definitions describe how products are grouped and scored.

## Directory layout

```
verticals/
├─ src/main/resources/verticals  # YAML vertical definitions
├─ src/test/java                 # tests for vertical parsing
```

## Build and test

From the repository root build only this module:

```bash
mvn -pl verticals -am clean install
```

Run tests:

```bash
mvn test
```

## Contributing new verticals

Contributions are welcome! Add new YAML files under `src/main/resources/verticals` and open a pull request. For project-wide details see the [root README](../README.md).
