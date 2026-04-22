---
title: Protobuf Protocol
description: Protocol Buffer message definitions used for client-server communication.
---

Flaggi uses Protocol Buffers v3 for all network serialization. Proto definitions live in `shared/src/main/proto/` and generate Java classes in the `flaggi.proto` package.

:::tip[Key Concept]
Treat the proto files as the source of truth. If a message changes there, the generated Java API changes with it.
:::

## Proto files

| File                    | Direction       | Purpose                    |
| ----------------------- | --------------- | -------------------------- |
| `client-messages.proto` | Client → Server | Input, commands, invites   |
| `server-messages.proto` | Server → Client | State updates, game events |

## Client messages

### `ClientMessage` (TCP wrapper)

Every TCP message from the client is wrapped in a `ClientMessage`:

```protobuf
message ClientMessage {
  string uuid = 1;
  oneof payload {
    Ping ping = 2;
    ClientHello hello = 3;
    ClientCommand command = 4;
    ClientInvite invite = 5;
    ClientInviteResponse invite_response = 6;
  }
}
```

The wrapper lets the server read one envelope and then dispatch by payload type.

### `ClientStateUpdate` (UDP)

Sent every tick over UDP - not wrapped in `ClientMessage`:

```protobuf
message ClientStateUpdate {
  string player_uuid = 1;
  string game_uuid = 2;
  ClientMouseInput mouse = 3;
  repeated ClientKey held_keys = 4;
}
```

### Key enums

```protobuf
enum ClientKey {
  KEY_UP = 0;
  KEY_DOWN = 1;
  KEY_LEFT = 2;
  KEY_RIGHT = 3;
  KEY_SHOOT = 4;
}

enum ClientCommandType {
  GET_IDLE_CLIENT_LIST = 0;
}
```

### Input mapping

| Key                     | ClientKey   |
| ----------------------- | ----------- |
| W / Arrow Up            | `KEY_UP`    |
| S / Arrow Down          | `KEY_DOWN`  |
| A / Arrow Left          | `KEY_LEFT`  |
| D / Arrow Right         | `KEY_RIGHT` |
| Space / F / Mouse Click | `KEY_SHOOT` |

## Server messages

### `ServerMessage` (TCP wrapper)

```protobuf
message ServerMessage {
  oneof payload {
    Pong pong = 1;
    ServerHello hello = 2;
    ServerCommand command = 3;
    ServerInvite invite = 4;
    IdleClientList idle_client_list = 5;
    ServerJoinGame join_game = 6;
    ServerEndGame end_game = 7;
  }
}
```

This is the server-side mirror of `ClientMessage`.

### `ServerStateUpdate` (UDP)

```protobuf
message ServerStateUpdate {
  ServerGameObject me = 1;
  repeated ServerGameObject other = 2;
  int32 tick = 3;
}
```

`me` is personalized for the receiving player, while `other` contains the rest of the visible world.

### `ServerGameObject`

The core game object transferred over the wire:

```protobuf
message ServerGameObject {
  int32 x = 1;
  int32 y = 2;
  int32 coll_x = 3;
  int32 coll_y = 4;
  int32 coll_width = 5;
  int32 coll_height = 6;
  GameObjectType type = 7;
  PlayerSkin skin = 8;
  PlayerAnimation animation = 9;
  bool facing_left = 10;
  double hp = 11;
  int32 flag_count = 12;
  string username = 13;
}
```

## Why this shape?

The protocol keeps the game state simple to consume:

- the client gets a ready-to-render snapshot every tick
- the server does not send unnecessary internal state
- each object can be updated, filtered, or extended independently

## Evolving the protocol

When changing a `.proto` file:

1. Add new fields instead of renumbering old ones.
2. Avoid reusing removed field numbers.
3. Keep client and server in sync.
4. Rebuild the shared module to regenerate Java classes.

### Game object enums

```protobuf
enum GameObjectType {
  PLAYER = 0;
  TREE = 1;
  FLAG = 2;
  BULLET = 3;
}

enum PlayerSkin {
  SKIN_BLUE = 0;
  SKIN_RED = 1;
  SKIN_JESTER = 2;
  SKIN_VENOM = 3;
}

enum PlayerAnimation {
  ANIM_IDLE = 0;
  ANIM_WALK_UP = 1;
  ANIM_WALK_DOWN = 2;
  ANIM_WALK_SIDE = 3;
  ANIM_WALK_DIAGONAL = 4;
}
```

## Regenerating proto classes

After editing `.proto` files, rebuild the shared module:

```bash title="Regenerate protobuf classes"
./gradlew :shared:build
```

The protobuf Gradle plugin (configured in `buildSrc/java-common.gradle.kts`) runs `protoc 3.25.1` and generates Java classes into the build output.

:::note[Note]
The generated classes land in the `flaggi.proto` package and are consumed by both the client and the server.
:::
