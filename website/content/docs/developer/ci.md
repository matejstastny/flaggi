---
title: CI & Formatting
description: Continuous integration, formatting checks, and release automation for Flaggi.
---

Flaggi uses GitHub Actions for build checks, formatting checks, website deployment and releases.

## Release workflow

Releases are built in CI with a multi-OS matrix in [.github/workflows/release.yml](../../../../.github/workflows/release.yml).

### Trigger

- Automatic on Git tags matching `v*` (example: `v1.2.0`)
- Manual via `workflow_dispatch`

### What it builds

- Windows installer (`.exe`)
- macOS installer (`.dmg`)
- Linux app image (packaged as `.tar.gz`)

### Runtime bundling

Each artifact includes its own Java runtime.

The workflow runs:

1. `jdeps` to detect required Java modules
2. `jlink` to build a minimal runtime image
3. `jpackage --runtime-image` to bundle that runtime into the final artifact

### Publishing

On tag builds, the workflow creates a GitHub Release and uploads all packaged artifacts automatically.

## Build workflow

The main build workflow runs on pushes and pull requests to `main`.

It builds these modules in a matrix:

- `client`
- `server`
- `shared`

Each job uses Java 21.

## Formatting workflow

The formatting workflow checks the whole repository with Prettier.

```bash title="Local formatting check"
npx prettier --check .
```

Run it locally before opening a PR if you touch docs, scripts, or the website.

## Website deployment

The docs site is built separately and published through GitHub Pages.

## Local equivalents

```bash title="Java build checks"
./gradlew :shared:compileJava :client:compileJava :server:compileJava
```

```bash title="Full repository check"
./gradlew build
./gradlew spotlessCheck
```
