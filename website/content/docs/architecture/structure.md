---
title: Project Structure
description: How the Flaggi codebase is organized across modules and packages.
---

Flaggi is a multi-module Gradle project using Kotlin DSL for build configuration. Each module has a specific role and clear boundaries.

::: note[Note]
The docs website lives in `website/` and is separate from the game runtime.
:::

## Module overview

```
flaggi/
├── assets/         Branding, icons, and installer artwork
├── buildSrc/       Shared Gradle convention plugin
├── client/         Java Swing client
├── dev-console/    Electron launcher for running server + clients
├── legacy-docs/    Archived MDX docs
├── scripts/        Shell helpers for running and packaging
├── server/         Game server (TCP + UDP)
├── server-temp/    Runtime staging area for the server jar/config
├── shared/         Shared objects, protobuf models, utilities
└── website/        Astro + Starlight documentation site
```

## Dependency graph

```
client ──→ shared
server ──→ shared
dev-console ──→ client + server
website ──→ docs only
```

`client` and `server` both depend on `shared`. They do not depend on each other. `shared` contains the code that must stay identical on both sides: generated protobuf classes, game objects, hitboxes, utilities, and rendering helpers.

## Runtime flow

1. The client starts first and opens a local UDP listener.
2. The client sends a TCP hello to the server with its username and UDP port.
3. The server assigns a UUID and replies with the server UDP port.
4. Once enough players are connected, the server creates a match and starts ticking game state.
5. The client renders the latest server snapshot while also predicting movement locally.

## Client packages

| Package                   | Key files                                     | Purpose                             |
| ------------------------- | --------------------------------------------- | ----------------------------------- |
| `flaggi.client`           | `App.java`                                    | Entry point and app lifecycle       |
| `flaggi.client.common`    | `GameManager.java`, `Sprite.java`             | Input, state sync, sprite helpers   |
| `flaggi.client.network`   | `TcpManager.java`, `UdpManager.java`          | Client networking                   |
| `flaggi.client.ui`        | `GameUi.java`, `LoginUi.java`, `LobbyUi.java` | Swing UI panels                     |
| `flaggi.client.constants` | `Constants.java`                              | Config, resources, and app settings |

## Server packages

| Package | Key files | Purpose |
| --- | --- | --- |
| `flaggi.server` | `Server.java` | Entry point and main loop |
| `flaggi.server.client` | `Client.java`, `ClientHandler.java` | TCP connection lifecycle |
| `flaggi.server.common` | `GameManager.java`, `GameData.java`, `TcpListener.java`, `UdpManager.java` | Match logic and networking |
| `flaggi.server.constants` | `Constants.java`, `Hitboxes.java` | Ports, config, and collision shapes |

## Shared packages

| Package | Key files | Purpose |
| --- | --- | --- |
| `flaggi.shared.common` | `GameObject.java`, `PlayerGameObject.java`, `FlagGameObject.java`, `Logger.java`, `UpdateLoop.java` | Game objects, logging, and the shared game loop |
| `flaggi.shared.util` | `FileUtil.java`, `ImageUtil.java`, `NetUtil.java`, `ProtoUtil.java` | Files, resources, networking, protobuf helpers |
| `flaggi.shared.ui` | `GPanel.java`, `Renderable.java`, `VhGraphics.java` | Lightweight UI/rendering abstractions |
| `flaggi.proto` | Generated classes | Protobuf messages shared by client and server |

## Build system

### `buildSrc/java-common.gradle.kts`

A custom Gradle plugin applied to all Java modules. It configures:

- Java 21 toolchain
- Protobuf compilation (protoc 3.25.1)
- Maven Central repository
- Protobuf Java library dependency

### Module build files

Each module's `build.gradle.kts` adds its own dependencies and configures the shadow JAR:

- **client** - depends on `:shared`, main class `flaggi.client.App`, shadow JAR name `flaggi-client.jar`
- **server** - depends on `:shared`, `sqlite-jdbc`, `javalin`, `slf4j-simple`, shadow JAR name `flaggi-server.jar`
- **shared** - library module with protobuf generation and no runnable JAR

## Resources

Game resources are bundled in each module's `src/main/resources/`:

- **Client**: sprites, fonts, UI images
- **Server**: map definitions, config, and runtime resources copied next to the jar
- **Shared**: protobuf definitions in `src/main/proto/`

## Generated and temporary directories

- `build/` contains Gradle output for each module
- `server-temp/` is used by `scripts/run.sh server` to stage the server jar and config
- `website/dist/` contains the built docs site
- `client/bin/`, `server/bin/`, and `shared/bin/` are convenience output folders used by the project tooling
