# Releasing promptlm-clients

This repository ships three client SDKs:

| Language   | Coordinates                    | Channel        |
|------------|--------------------------------|----------------|
| Java       | `dev.promptlm:promptlm-client` | Maven Central  |
| Python     | `promptlm-client`              | PyPI           |
| TypeScript | `@promptlm/client`             | npm            |

Per-language mechanical details (build commands, signing, secrets) live
under [`docs/`](docs/):

- [`docs/releasing-java.md`](docs/releasing-java.md)
- `docs/releasing-python.md` — _to be added when the Python client ships_
- `docs/releasing-typescript.md` — _to be added when the TS client ships_

## Versioning

- **Major and minor are synced** across all three clients. `X.Y.0` means
  the same feature surface across Java / Python / TS and a compatible
  `promptlm-app` / `promptlm-platform`.
- **Patch is independent per language.** A Python-only bugfix can ship
  as `0.1.2` without forcing Java or TS to bump.

### Per-language bump rules

- **Major (`X.0.0`)** — at least one client has a breaking API change
  (interface removed, method signature change, return-type change).
- **Minor (`X.Y.0`)** — additive in any client (new method, new loader
  variant, new source kind).
- **Patch (`X.Y.Z`)** — bugfix only in a single client.

While on the `0.x` line the API surface is treated as not-yet-stable
and minor bumps may include breaking changes.

## Cutting a release

### Patch — single language

Dispatch the per-language workflow with an explicit `version` input
from the Actions tab:

- **Java** → `release-java` → `version=X.Y.Z`
- **Python** → `release-python` → `version=X.Y.Z`
- **TypeScript** → `release-node` → `version=X.Y.Z`

Each workflow runs its verify gate, builds, signs (GPG for Maven
Central; OIDC provenance for npm), publishes to the channel, tags the
repo, and bumps the source back to the next pre-release version.

### Coordinated minor or major (all three languages together)

When a feature ships in all three clients, cut from a single point:

1. Open a tracking issue titled `release: promptlm-clients X.Y.0`.
2. Confirm all per-language verify workflows are green on `main`.
3. Dispatch the per-language release workflows with the same `version`
   input (or use `release-sdks` once that umbrella workflow exists).

Maven Central syncs roughly 30 minutes after publish; PyPI and npm
appear within a minute.

## Changelog

The canonical changelog is the
[GitHub releases page](https://github.com/promptLM/promptlm-clients/releases).
`CHANGELOG.md` at the repo root carries a hand-curated entry per
coordinated release; per-language patch releases are recorded only on
the GitHub releases page and in the respective registry metadata.
