# Releasing The Java SDK

This repository publishes the Java SDK to Maven Central by running the `release-java` Maven profile from the repository root.

## Coordinates

- Group ID: `dev.promptlm`
- Artifact ID: `promptlm-client`

## Required Secrets

The GitHub Actions release workflow expects these repository secrets:

- `MAVEN_CENTRAL_USERNAME`: Sonatype Central user token username
- `MAVEN_CENTRAL_PASSWORD`: Sonatype Central user token password
- `MAVEN_GPG_PRIVATE_KEY`: ASCII-armored private key used to sign Maven artifacts
- `MAVEN_GPG_PASSPHRASE`: passphrase for `MAVEN_GPG_PRIVATE_KEY`

## Local Dry Run

Use this to verify the release profile without publishing or signing:

```bash
./mvnw -B -Prelease-java -pl client-sdk-java -am -Dgpg.skip=true -DskipTests verify
```

This verifies the release profile and produces the main, sources, and javadocs artifacts without contacting Sonatype Central.

## Maven Settings

Deploying through the Sonatype Central plugin requires a Maven `settings.xml` entry named `central`, even when `-Dcentral.skipPublishing=true` is used.

An example settings file is available at `docs/maven-central-settings.xml.example`.

## GitHub Release Workflow

Run the `Release Java SDK` workflow and provide:

- `version`: the release version to publish, for example `0.1.0`
- `auto_publish`: `true` to automatically publish after validation, `false` to stop after upload and validation

The workflow will:

1. Build the Java module with sources and javadocs JARs.
2. Sign the published artifacts.
3. Upload the bundle to Sonatype Central.
4. Optionally publish immediately when `auto_publish=true`.

## Manual Central Upload

If you want to validate the full signed deploy path locally without uploading to Central, run:

```bash
export MAVEN_GPG_PASSPHRASE=your-passphrase
./mvnw -B -s docs/maven-central-settings.xml.example -Prelease-java -pl client-sdk-java -am -DskipTests deploy -Dcentral.skipPublishing=true
```

This validates the deploy configuration, signatures, and Central plugin wiring without publishing to Maven Central. Replace the environment variable placeholders in the settings file before use and make sure your local GPG key is available to `gpg`.
