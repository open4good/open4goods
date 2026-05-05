# rag-engine

`rag-engine` is a reusable Maven module designed for multi-project RAG usage.

## Scope

- GitHub-based ingestion (runtime, configurable include patterns)
- Markdown-first processing (`.md` default, extensible later)
- Use-case-based provider routing (`chat`, `embedding`, `summary`)
- Tag enrichment by extension and folder pattern

## Initial metadata contract (v2 draft)

Mandatory fields:

- `scope` (access-control binding; role-oriented)
- `author`
- `contributors`
- `tags`
- `locale`

Document modes:

- **raw**: unannotated markdown indexed as-is
- **annotated**: markdown plus explicit custom header schema

## Example configuration

```yaml
rag-engine:
  github:
    repository: open4good/open4goods
    branch: main
    include-patterns:
      - "**/*.md"
    extension-tag-mappings:
      - extension: "*.vue"
        tags: ["frontend", "technical"]
    folder-tag-mappings:
      - folder-pattern: "frontend/**"
        tags: ["frontend"]
  providers:
    - name: infera
      endpoint: https://api.infera.example/v1
      api-key: ${INFERA_API_KEY}
      model: infera-embed-1
    - name: openai-compatible
      endpoint: https://example-openai-compatible/v1
      api-key: ${OPENAI_COMPAT_KEY}
      model: text-embedding-3-large
  use-cases:
    chat-provider: infera
    embedding-provider: openai-compatible
    summary-provider: infera
```

## Notes

This module currently provides contracts and bootstrap auto-configuration.
Implementation adapters (GitHub scanner, chunkers/parsers, vector stores) should be added in follow-up slices.
