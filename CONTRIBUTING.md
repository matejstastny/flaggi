# Contributing to Flaggi

Thanks for your interest in contributing! This document covers the basics for getting started.

## Getting Started

1. Fork the repository
2. Clone your fork
3. Set up the development environment - see the [docs](https://matejstastny.github.io/flaggi/building/dev-environment/)
4. Create a branch for your change

## Development

The recommended way to run the project is through the dev console:

```bash
cd dev-console
npm install
npm start
```

Or use the run script directly:

```bash
scripts/run.sh server   # Start the server
scripts/run.sh client   # Start a client
```

For a compile-only check:

```bash
./gradlew :shared:compileJava :client:compileJava :server:compileJava
```

## Submitting Changes

1. Make sure your code compiles cleanly
2. Keep commits focused - one logical change per commit
3. Write clear commit messages
4. Open a pull request against `main`
5. Describe what your PR does and why

## Project Structure

```
client/      - Java Swing game client
server/      - Game server (TCP + UDP)
shared/      - Shared protos, game objects, utilities
editor/      - Map/level editor (WIP)
dev-console/ - Electron dev launcher
scripts/     - Build and run helpers
docs/        - Astro Starlight documentation site
```

For detailed architecture docs, see the [developer documentation](https://matejstastny.github.io/flaggi/introduction/).

## Code of Conduct

Be respectful and constructive. We follow the [Contributor Covenant](https://www.contributor-covenant.org/version/2/1/code_of_conduct/) code of conduct.

## Questions?

Open an issue or start a discussion on the repository.
