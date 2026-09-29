"""Resolve the local-profile Spring configs the way Spring Boot actually
would (module application-local.yml, overridden by the ops/local/config
additional-location overlay, resolved against an environment), and assert
the resolved cross-service URLs land on the intended ports.

This replaces symbol-grepping: a stale hardcoded port anywhere in either
YAML layer would otherwise still pass an `rg` check for the new port's
presence elsewhere in the file.
"""
import re
import unittest
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]

PLACEHOLDER = re.compile(r"\$\{([A-Za-z0-9_]+)(?::([^}]*))?\}")

# Historical strict-local ports every buildhost cross-service reference must
# stop resolving to once the shared-host port plan is applied.
HISTORICAL_PORTS = {"9200", "5432", "6379", "8080", "3000", "3001", "8082", "8088", "8089"}


def load_env_file(path: Path) -> dict:
    env = {}
    for line in path.read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        env[key.strip()] = value.strip()
    return env


def resolve(value, env: dict):
    if isinstance(value, str):
        def sub(match):
            key, default = match.group(1), match.group(2)
            return env.get(key, default if default is not None else match.group(0))
        previous = None
        while previous != value:
            previous = value
            value = PLACEHOLDER.sub(sub, value)
        return value
    if isinstance(value, dict):
        return {k: resolve(v, env) for k, v in value.items()}
    if isinstance(value, list):
        return [resolve(v, env) for v in value]
    return value


def deep_merge(base: dict, overlay: dict) -> dict:
    merged = dict(base)
    for key, value in overlay.items():
        if isinstance(value, dict) and isinstance(merged.get(key), dict):
            merged[key] = deep_merge(merged[key], value)
        else:
            merged[key] = value
    return merged


def load_yaml(path: Path) -> dict:
    return yaml.safe_load(path.read_text()) or {}


def flatten_strings(value, prefix=""):
    if isinstance(value, dict):
        for key, sub in value.items():
            yield from flatten_strings(sub, f"{prefix}.{key}" if prefix else str(key))
    elif isinstance(value, list):
        for i, sub in enumerate(value):
            yield from flatten_strings(sub, f"{prefix}[{i}]")
    elif isinstance(value, str):
        yield prefix, value


# module -> (application-local.yml path, ops/local/config overlay name)
SERVICES = {
    "api": ("api/src/main/resources/application-local.yml", "api"),
    "ui": ("ui/src/main/resources/application-local.yml", "ui"),
    "admin": ("admin/src/main/resources/application-local.yml", "admin"),
    "front-api": ("front-api/src/main/resources/application-local.yml", "front-api"),
    "b2b-api": ("b2b-api/src/main/resources/application-local.yml", "b2b-api"),
}

# Cross-service keys each module must resolve away from a historical port
# once the shared-host env is applied. Keys are dotted paths into the
# resolved config tree.
EXPECTED = {
    "api": {"spring.elasticsearch.uris": "O4G_PORT_ELASTICSEARCH", "xwiki.baseUrl": "O4G_PORT_XWIKI"},
    "ui": {
        "spring.elasticsearch.uris": "O4G_PORT_ELASTICSEARCH",
        "xwiki.baseUrl": "O4G_PORT_XWIKI",
        "image-base-url": "O4G_PORT_UI",
        "namings.baseUrls.fr": "O4G_PORT_FRONTEND",
        "namings.baseUrls.default": "O4G_PORT_FRONTEND",
    },
    "admin": {"xwiki.baseUrl": "O4G_PORT_XWIKI"},
    "front-api": {
        "spring.elasticsearch.uris": "O4G_PORT_ELASTICSEARCH",
        "front.resource-root-path": "O4G_PORT_UI",
        "front.exposed-docs.base-url": "O4G_PORT_EXPOSED_DOCS",
        "front.geocode.base-url": "O4G_PORT_GEOCODE",
        "xwiki.base-url": "O4G_PORT_XWIKI",
    },
    "b2b-api": {
        "spring.datasource.url": "O4G_PORT_POSTGRES",
        "spring.data.redis.port": "O4G_PORT_REDIS",
        "spring.elasticsearch.uris": "O4G_PORT_ELASTICSEARCH",
        "b2b.public-base-url": "O4G_PORT_B2B_FRONTEND",
        "b2b.security.allowed-origins": "O4G_PORT_B2B_FRONTEND",
    },
}


def get_path(tree: dict, dotted: str):
    node = tree
    for part in dotted.split("."):
        node = node[part]
    return node


class ResolvedBuildhostConfigTest(unittest.TestCase):
    """Resolve every module's local-profile config against the buildhost
    port plan and assert cross-service URLs land on the assigned ports,
    never on a historical/beta one."""

    @classmethod
    def setUpClass(cls):
        cls.buildhost_env = load_env_file(ROOT / ".env.buildhost.example")

    def _merged(self, module_path: str, overlay_name: str) -> dict:
        base = load_yaml(ROOT / module_path)
        overlay = load_yaml(ROOT / "ops/local/config" / f"{overlay_name}.yml.example")
        return deep_merge(base, overlay)

    def test_buildhost_ports_resolve_and_avoid_historical_ports(self):
        for service, (module_path, overlay_name) in SERVICES.items():
            merged = self._merged(module_path, overlay_name)
            resolved = resolve(merged, self.buildhost_env)
            for dotted, port_var in EXPECTED[service].items():
                with self.subTest(service=service, field=dotted):
                    value = str(get_path(resolved, dotted))
                    expected_port = self.buildhost_env[port_var]
                    self.assertIn(
                        expected_port, value,
                        f"{service}.{dotted} did not resolve to {port_var}={expected_port}: {value}",
                    )
                    for historical in HISTORICAL_PORTS - {expected_port}:
                        self.assertNotRegex(
                            value, rf":{historical}(/|$)",
                            f"{service}.{dotted} still resolves to historical port {historical}: {value}",
                        )

    def test_strict_local_defaults_still_resolve_without_buildhost_env(self):
        # Without O4G_SHARED_HOST env vars set, every placeholder must fall
        # back to its documented historical default, not go unresolved.
        for service, (module_path, overlay_name) in SERVICES.items():
            merged = self._merged(module_path, overlay_name)
            resolved = resolve(merged, {})
            for dotted in EXPECTED[service]:
                value = str(get_path(resolved, dotted))
                with self.subTest(service=service, field=dotted):
                    self.assertNotIn("${", value, f"{service}.{dotted} left unresolved: {value}")


if __name__ == "__main__":
    unittest.main()
