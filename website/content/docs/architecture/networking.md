---
title: Networking
description: How Flaggi's TCP + UDP networking stack works.
---

Flaggi uses two network channels per connected client: TCP for reliable control messages and UDP for high-frequency game state.

## Channel overview

| Channel    | Protocol      | Port  | Purpose                        |
| ---------- | ------------- | ----- | ------------------------------ |
| Control    | TCP (Javalin) | 54321 | Handshake, lobby, game events  |
| Game state | UDP           | 54322 | Per-tick input and world state |

TCP handles everything that needs reliability: initial connection, lobby management, game invites, and end-of-match messages. UDP handles the latency-sensitive game state - player input going up and world snapshots coming down - where occasional packet loss is acceptable.

:::tip[Key Concept]
TCP serves as the control plane for reliable, ordered communication, while UDP operates as the simulation plane for fast, real-time data transmission without delivery guarantees.
:::

## Connection flow

```
Client                          Server
  │                               │
  │── TCP: ClientHello ──────────→│  username + UDP port
  │←── TCP: ServerHello ─────────│  assigned UUID + server UDP port
  │                               │
  │── TCP: GET_IDLE_CLIENT_LIST ─→│
  │←── TCP: IdleClientList ──────│  available players
  │                               │
  │── TCP: ClientInvite ─────────→│  invite target player
  │           ┌───────────────────│── TCP: ServerInvite ──→ Target
  │           │                   │←── TCP: InviteResponse ── Target
  │←── TCP: ServerJoinGame ──────│  gameUuid + room size
  │                               │
  │── UDP: ClientStateUpdate ────→│  keys, mouse (every tick)
  │←── UDP: ServerStateUpdate ───│  full world state (every tick)
  │           ...                 │
  │←── TCP: ServerEndGame ───────│  winner
```

The handshake is intentionally simple:

1. The client opens a local UDP socket.
2. It sends `ClientHello` over TCP with the username and UDP port.
3. The server assigns a UUID and replies with `ServerHello` containing the server UDP port.
4. The client stores that UUID and starts sending `ClientStateUpdate` packets.
5. The server creates a match once enough players are connected.

## UDP game loop

Both client and server tick at roughly 60 Hz (16 ms intervals).

### Client → Server (per tick)

The client sends a `ClientStateUpdate` containing:

- Player UUID and game UUID
- Mouse position (in world coordinates)
- Set of currently held keys (`KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT`, `KEY_SHOOT`)

Updates are only sent when input changes (dirty flag optimization).

### Server → Client (per tick)

The server sends a `ServerStateUpdate` containing:

- `me` - the local player's `ServerGameObject` (position, HP, animation, flags)
- `other` - all other objects in the game (other players, trees, bullets, flags)
- `tick` - the current server tick number

Each client receives a personalized update where their own player is in the `me` field.

## Server authority and prediction

The server is authoritative for collisions, health, bullets, flags, and win conditions.

The client predicts movement locally so the game stays responsive, then corrects itself when the authoritative server position differs too much.

That means:

- your movement feels immediate
- the server still decides the real outcome
- small network hiccups do not desync the match permanently

## Rate limiting

The server's `UdpManager` enforces per-IP rate limiting: one packet per IP per 16 ms minimum. This prevents bandwidth exhaustion from misbehaving or malicious clients.

## Thread model

### Server

- **TCP listener thread** - accepts new connections
- **Per-client handler thread** - reads TCP messages, queues them
- **UDP listener thread** - receives datagrams, queues them
- **Update loop thread** - processes queued messages, ticks game logic

All mutable state uses `ConcurrentHashMap` and `CopyOnWriteArrayList` - no explicit locks.

The server's main game loop consumes queued updates, advances the simulation, and then sends a separate `ServerStateUpdate` to each connected player.

### Client

- **Swing EDT** - rendering and input handling (single-threaded)
- **UDP listener thread** - receives server state updates
- **Update loop thread** - processes input, sends client updates

Input fields use `volatile` for cross-thread visibility without locking.

The client keeps the latest UDP snapshot in memory and renders from that snapshot on the Swing event thread.
