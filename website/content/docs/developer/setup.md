---
title: Developer Setup
description: How to set up the development environment and run Flaggi locally.
---

## Requirements

- **Java 21** — full JDK, not just a JRE
- **Gradle** — use the included `./gradlew` wrapper
- **Node.js + npm** — needed for the dev console and the docs site

:::note[Note]
If you want the least setup, open the repository in the dev container and use the dev console.
:::

## Recommended workflow: dev console

The dev console is an Electron app that launches the server and two clients, then shows the logs and live game state side by side.

```bash title="Dev console terminal"
cd dev-console
npm install
npm start
```

Use the toolbar buttons to:

- **Rebuild** the server and client artifacts
- **Stop all** running processes
- **Clear logs** between test runs

This is the fastest way to test multiplayer behavior without manually juggling terminals.

## Manual workflow

If you prefer separate processes, use the helper script from the repository root.

```bash title="Server terminal"
./scripts/run.sh server
```

```bash title="Client terminal"
./scripts/run.sh client
```

The script builds the shadow JAR automatically before launching. Useful flags:

| Flag | Effect |
| --- | --- |
| `-r`, `--rebuild` | Clear the server temp directory before launching |
| `--skip-build` | Skip the Gradle build step and use the existing JAR |
| `-h`, `--help` | Show usage information |

:::note[Note]
`--rebuild` only applies to the server target.
:::

## Dev container

A Docker-based dev environment lives in `.devcontainer/`.

It includes:

- OpenJDK 21
- Protobuf compiler
- Gradle
- Common VS Code extensions for Java and formatting

Open the repository in VS Code and run **Dev Containers: Reopen in Container**.

## Compile-only checks

For fast verification without launching the game:

```bash title="Compile shared, client, and server"
./gradlew :shared:compileJava :client:compileJava :server:compileJava
```

To build everything:

```bash title="Full build"
./gradlew build
```

To build just the runnable jars:

```bash title="Shadow jars"
./gradlew shadowJar
```

The runnable artifacts are written to each module's `build/libs/` directory.

## Docs website

The docs site itself lives in `website/` and runs as a separate Astro app.

```bash title="Docs preview"
cd website
npm install
npm run dev
```

## Packaging dependencies

If you plan to package native installers, install these optional tools:

- **create-dmg** on macOS
- **WiX 3** on Windows
