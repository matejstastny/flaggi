# Flaggi - Claude Context

## Project structure

Multi-module Gradle project (Java 21, Kotlin DSL):

```
flaggi/
  client/      # Java Swing game client
  server/      # Game server (TCP + UDP, Javalin, SQLite)
  shared/      # Shared proto + utilities (Logger, FileUtil, VhGraphics, etc.)
  dev-console/ # Electron app for launching client/server during development
  docs/        # Astro Starlight documentation site
  scripts/     # run.sh and helpers
  buildSrc/    # Gradle convention plugin (java-common)
  assets/      # Distribution assets (banners, icons)
```

Key client packages:

- `flaggi.client.ui` - UI panels, `GameUi.java` is the main game renderer
- `flaggi.client.common` - `Sprite.java`, `GameManager.java`
- `flaggi.shared.util` - `FileUtil`, `ImageUtil`
- `flaggi.shared.ui` - `VhGraphics`, `GPanel`, `Renderable`

## Running the app

Testing is done via the `dev-console/` Electron app, which ultimately calls:

```bash
scripts/run.sh <client|server>
```

This builds a **shadowJar** and runs it with `java -jar`. There is no `./gradlew :client:run` dev workflow - always use `run.sh` / dev-console.

Compile check only (no run):

```bash
./gradlew :shared:compileJava :client:compileJava :server:compileJava
```

## Formatting

Each language has a dedicated formatter, all enforced in CI:

| Language | Formatter | Config location | Run |
| --- | --- | --- | --- |
| Java | palantir-java-format | `buildSrc/.../java-common.gradle.kts` (Spotless) | `./gradlew spotlessApply` |
| JS/TS/CSS/JSON/YAML/MD/Astro | Prettier | `.prettierrc` (root) | `npx prettier --write .` |
| Proto | clang-format | `.clang-format` (root) | auto via VSCode extension |
| Kotlin DSL | ktlint | via VSCode extension | auto via VSCode extension |
| Shell | shfmt | via VSCode extension | auto via VSCode extension |

Format check in CI: `./gradlew spotlessCheck` (Java) and `npx prettier --check .` (everything else).

## Protobuf

Protos live in `shared/src/main/proto/`. Generated Java classes are in `flaggi.proto.*`.

Key types from `server-messages.proto`:

- `ServerGameObject` - x, y, collX/Y/Width/Height, type, skin, animation, facingLeft, hp, flagCount, username
- `GameObjectType` - PLAYER, TREE, FLAG, BULLET
- `PlayerSkin` - SKIN_BLUE, SKIN_RED, SKIN_JESTER, SKIN_VENOM
- `PlayerAnimation` - ANIM_IDLE, ANIM_WALK_UP, ANIM_WALK_DOWN, ANIM_WALK_SIDE, ANIM_WALK_DIAGONAL

## Sprite system

**`Sprite.java`** (`flaggi.client.common`) - tick-based, no threads.

Folder conventions under `client/src/main/resources/sprites/`:

- Animated: `player-blue/{idle,walk-up,walk-down,walk-side,walk-diagonal}/0.png, 1.png ...`
- Static: `tree/tree.png`, `bullet/bullet.png`, `flag-blue/flag-blue.png`, `flag-red/flag-red.png`

Usage:

```java
Sprite s = new Sprite("player-blue", 8); // 8 fps
s.setAnimation("walk-up");
// per frame:
s.tick();
s.render(g2, x, y, root);  // renders centered on (x, y)
```

Extension passed to `FileUtil.listResourceFiles` must include the dot: `".png"`.

## FileUtil.listResourceFiles - known quirks

`shared/.../util/FileUtil.java` - `listResourceFiles(path, extension)`:

- Extension semantics: `""` = list directories, `null` = all files, `".png"` = files ending in `.png`
- Supports both **JAR** (production/shadowJar) and **filesystem** (`file://`, dev mode) protocols
- JAR directory listing works by **inferring** subdirectory names from entry paths (not relying on explicit directory entries, which may be absent or get skipped due to trailing `/`)

If you see "No PNG frames found" or "Sprite folder not found" errors, suspect that:

1. The sprite folder/files are missing from resources
2. The resource isn't on the classpath (check Gradle resource config)

## GameUi rendering

`client/.../ui/GameUi.java`:

- Camera: world is translated so the local player stays centered at `px(50), px(50)`
- Zoom: `ZOOM = 0.5` constant - zooms out by scaling around the player's screen position
- Player sprites are cached by username in a `HashMap<String, Sprite>`
- Facing-left flip: `g2.scale(-1, 1)` applied after translating to the player's world position
- Static sprites (tree, bullet, flags) are loaded once in the constructor

Object type → sprite folder mapping:

| Type   | Folder                                     |
| ------ | ------------------------------------------ |
| PLAYER | `player-{blue,red,jester,venom}` (by skin) |
| TREE   | `tree`                                     |
| BULLET | `bullet`                                   |
| FLAG   | `flag-blue` or `flag-red` (by skin field)  |

Animation → folder name mapping:

| Animation          | Folder          |
| ------------------ | --------------- |
| ANIM_IDLE          | `idle`          |
| ANIM_WALK_UP       | `walk-up`       |
| ANIM_WALK_DOWN     | `walk-down`     |
| ANIM_WALK_SIDE     | `walk-side`     |
| ANIM_WALK_DIAGONAL | `walk-diagonal` |

## Assets

Game assets **must** stay in `client/src/main/resources/` — Java classpath loading requires it. Distribution assets (banners, icons for packaging) live in `assets/`. Doc site images live in `docs/public/`. Each build system needs assets in its own conventional location; symlinks or copies would add fragile complexity.

## CI workflows

- **build.yml** — Java build matrix (client, server, shared) + Spotless check. Path-filtered to Gradle/Java files.
- **format.yml** — Prettier check on all PRs/pushes (fast, no path filter needed).
- **docs.yml** — Astro build + GitHub Pages deploy on docs/ changes.
