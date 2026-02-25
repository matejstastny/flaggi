# flaggi-dev

Electron dev launcher for Flaggi. Three-pane log view (server + 2 clients) with a single Rebuild button.

## Setup

```bash
cd flaggi-dev
npm install
```

## Configure

Edit the top of `main.js`:

```js
const FLAGGI_ROOT = path.resolve(__dirname, "..") // path to your flaggi repo
const SERVER_TASK = ":server:run" // Gradle task for server
const CLIENT_TASK = ":client:run" // Gradle task for client
```

The "server ready" detection uses a regex — edit `waitForReady` if your server prints a specific ready message:

```js
await waitForReady("server-log", /YOUR READY STRING/i, 15000)
```

Or just replace the whole call with `await sleep(5000)` for a fixed wait.

## Run

```bash
npm start
```

## What it does

1. Kills any running server/client processes
2. Runs `./gradlew --parallel :server:classes :client:classes` (compiles both in parallel)
3. Launches server via `./gradlew :server:run`, waits for ready signal
4. Launches client 1, then client 2 (with a small stagger)
5. Streams all stdout/stderr into the three log panels with basic colorization

Click **Rebuild** to restart the whole cycle. Click **Stop all** to kill everything.
