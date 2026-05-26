# Contributing

Thanks for taking the time to help out. This repository hosts the
promptLM client SDKs: a Java library that's shipping today, plus
Python and TypeScript packages landing with the 0.1.0 release.

## Before you start

- Be kind. See `CODE_OF_CONDUCT.md` for the ground rules.
- All contributions land under Apache-2.0. See `LICENSE`.
- For anything security-related, follow `SECURITY.md` instead of
  opening a public issue.

## Working on a change

1. Fork the repo and branch off `main`.
2. Keep commits focused and use [Conventional Commits](https://www.conventionalcommits.org)
   for the subject line. Scopes we use: `java`, `python`, `ts`, `oss`.
3. Run the local build for the SDK you touched. For Java that's
   `./mvnw clean verify`; JDK 17 is required.
4. Open a pull request against `main` with a short description of
   the change and why it matters.

## Questions

Open a GitHub issue or discussion. We'll get back to you.
