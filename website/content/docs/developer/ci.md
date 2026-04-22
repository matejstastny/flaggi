---
title: CI & Formatting
description: Continuous integration, formatting checks, and release automation for Flaggi.
---

Flaggi uses GitHub Actions for build checks, formatting checks, and website deployment.

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
