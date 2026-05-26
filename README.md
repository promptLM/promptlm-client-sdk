# promptlm-client-sdk

Client SDKs for loading and consuming promptLM prompt bundles in your
applications. The repository is a polyglot workspace: one runtime
contract (`prompts/prompt-index.json` plus referenced prompt files),
multiple language bindings.

## SDKs

| Language   | Module               | Status                              |
|------------|----------------------|-------------------------------------|
| Java       | `client-sdk-java`    | Available                           |
| Python     | `client-sdk-python`  | Work in progress — coming in 0.1.0  |
| TypeScript | `client-sdk-ts`      | Work in progress — coming in 0.1.0  |

## Install (Java)

```xml
<dependency>
    <groupId>dev.promptlm</groupId>
    <artifactId>promptlm-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

See [`client-sdk-java/README.md`](client-sdk-java/README.md) for a
usage example, the Gradle snippet, and module-specific notes.

## Runtime contract

The SDKs consume JSON runtime artifacts:

- `prompts/prompt-index.json` — index of available prompts.
- Prompt files referenced from each index entry's `path`.

Each index entry has the shape `{ id, version, name, path }`. Each
prompt file contains at least `{ id, version, name, prompt }`.

## Build (Java)

```bash
./build-jdk.sh
```

JDK 17 is required.

## Release

Java Maven Central release mechanics live in
[docs/releasing-java.md](docs/releasing-java.md).

## Contributing and license

- See [CONTRIBUTING.md](CONTRIBUTING.md) for how to file changes.
- See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community ground rules.
- See [SECURITY.md](SECURITY.md) for reporting vulnerabilities.
- Licensed under Apache-2.0. See [LICENSE](LICENSE).
