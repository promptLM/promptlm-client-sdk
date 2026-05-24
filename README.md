# promptlm-client-sdk

Standalone promptLM client SDK workspace.

## Modules

- `client-sdk-java`: Java prompt loading library.
- `client-sdk-ts`: TypeScript SDK for JSON prompt bundles.
- `client-sdk-python`: Python SDK for JSON prompt bundles.

## Runtime Contract

The TypeScript and Python SDKs consume JSON runtime artifacts:

- `prompts/prompt-index.json`
- prompt JSON files referenced by each index entry `path`

Each index entry uses the shape `{ id, version, name, path }`. Each prompt JSON file contains at least `{ id, version, name, prompt }`.

## Build

Java:

```bash
./build-jdk.sh
```

## Release

Java Maven Central release documentation lives in [docs/releasing-java.md](docs/releasing-java.md).
