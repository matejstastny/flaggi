---
title: Play the Game
description: Quick-start instructions for running Flaggi as a player.
---

# Play the game

Flaggi is a local multiplayer capture-the-flag game. There is no public server, so one player runs the server and the others connect to it with the client.

> [!TIP] If everyone is on the same machine, use `localhost:54321`. If you are on a LAN, use the host machine's local IP address instead.

## What you need

- A running server
- One client for each player
- A copy of the release build or the repository source tree
- TCP port `54321` and UDP port `54322` open on the server machine

## Fastest way to start

### 1. Start the server

If you downloaded a release build, launch the server JAR directly:

```bash title="Server terminal"
java -jar Flaggi-server.jar
```

If you are running from source, the helper script builds the server automatically and launches it for you:

```bash title="Server terminal"
./scripts/run.sh server
```

> [!IMPORTANT] Start the server first. The client will not connect until the TCP listener is available.

### 2. Start the client

```bash title="Client terminal"
java -jar Flaggi-client.jar
```

Or, from source:

```bash title="Client terminal"
./scripts/run.sh client
```

### 3. Connect

In the client menu, enter the server address and your username.

| Situation         | Example address              |
| ----------------- | ---------------------------- |
| Same machine      | `localhost:54321`            |
| Same Wi-Fi / LAN  | `192.168.1.50:54321`         |
| Different network | Use a VPN or port forwarding |

## Controls

- **WASD** or **arrow keys** to move
- **Space**, **F**, or **mouse click** to shoot
- Capture the enemy flag and bring it back to your base

## What happens after you connect

1. The client opens a local UDP listener.
2. It sends a TCP hello with your username and UDP port.
3. The server assigns a UUID and replies with the server UDP port.
4. The client switches to the lobby and then into the match.

> [!NOTE] The game uses TCP for setup and gameplay events, and UDP for fast per-tick state updates.

## Troubleshooting

- **Can't connect**: make sure the server machine address is correct and the port is `54321`.
- **Nothing happens after launch**: check the server terminal for logs.
- **The client closes immediately**: make sure you are using a full JDK/JRE release, not an incomplete install.
- **Two clients on one machine**: this is supported; each client keeps its own UDP listener.
