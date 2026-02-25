const { app, BrowserWindow, ipcMain } = require("electron");
const { spawn } = require("child_process");
const path = require("path");

// ── CONFIG ──────────────────────────────────────────────────────────────────

const FLAGGI_ROOT  = path.resolve(__dirname, "..");
const WRAPPER_SH   = path.join(FLAGGI_ROOT, "scripts", "run-wrapper.sh");

// Must match something your server logs when it's ready to accept connections
const SERVER_READY_PATTERN    = /Application start/;
const SERVER_READY_TIMEOUT_MS = 60_000;
// ────────────────────────────────────────────────────────────────────────────

let win;
let procs      = { server: null, client1: null, client2: null };
let isBuilding = false;
const logWatchers = {}; // channel → [fn, ...]

// ── Window ───────────────────────────────────────────────────────────────────

function createWindow() {
  win = new BrowserWindow({
    width: 1100,
    height: 820,
    minWidth: 800,
    minHeight: 600,
    title: "Flaggi Dev",
    backgroundColor: "#0f1117",
    titleBarStyle: "hidden",
    trafficLightPosition: { x: 12, y: 12 },
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, "preload.js"),
    },
  });
  win.loadFile("index.html");
  // win.webContents.openDevTools();
}

app.whenReady().then(createWindow);
app.on("window-all-closed", () => { killAll(); app.quit(); });

// ── Helpers ──────────────────────────────────────────────────────────────────

function sleep(ms) { return new Promise((r) => setTimeout(r, ms)); }

function send(channel, data) {
  const watchers = logWatchers[channel];
  if (watchers) for (const fn of [...watchers]) fn(data);
  if (win && !win.isDestroyed()) win.webContents.send(channel, data);
}

function setStatus(s)      { send("status", s); }
function setButtonState(s) { send("button-state", s); }

function timestamp() {
  return new Date().toLocaleTimeString([], {
    hour: "2-digit", minute: "2-digit", second: "2-digit"
  });
}

// ── Process management ────────────────────────────────────────────────────────

function killAll() {
  for (const [key, proc] of Object.entries(procs)) {
    if (proc) {
      try { process.kill(-proc.pid, "SIGTERM"); } catch (_) {}
      try { proc.kill("SIGKILL"); }               catch (_) {}
      procs[key] = null;
    }
  }
}

function spawnProc(args, logChannel, onClose) {
  const proc = spawn(args[0], args.slice(1), {
    cwd: FLAGGI_ROOT,
    detached: true, // needed for negative-pid group kill
    env: { ...process.env },
  });

  proc.stdout.on("data", (d) => send(logChannel, d.toString()));
  proc.stderr.on("data", (d) => send(logChannel, d.toString()));
  proc.on("close", (code) => {
    send(logChannel, `\n[process exited with code ${code}]\n`);
    onClose?.(code);
  });
  proc.on("error", (err) => {
    send(logChannel, `\n[spawn error: ${err.message}]\n`);
    onClose?.(-1);
  });

  return proc;
}

// ── Pattern watcher ───────────────────────────────────────────────────────────

// Resolves true if pattern matched, false if timed out or failed
function waitForPattern(logChannel, pattern, timeoutMs) {
  return new Promise((resolve) => {
    if (!logWatchers[logChannel]) logWatchers[logChannel] = [];

    const cleanup = () => {
      logWatchers[logChannel] = logWatchers[logChannel].filter((f) => f !== watcher);
      clearTimeout(timer);
    };

    const watcher = (data) => {
      if (pattern.test(data)) {
        cleanup();
        resolve(true);
      }
    };

    logWatchers[logChannel].push(watcher);
    const timer = setTimeout(() => { cleanup(); resolve(false); }, timeoutMs);
  });
}

// ── Build + launch flow ───────────────────────────────────────────────────────

async function rebuild() {
  if (isBuilding) return;
  isBuilding = true;
  setButtonState("building");
  setStatus("stopping");

  killAll();
  await sleep(600);

  // ── Step 1: build + start server ─────────────────────────────────────────
  setStatus("building");
  send("server-log", `\n─── BUILD + SERVER  [${timestamp()}] ─────────────────\n`);

  let serverReady       = false;
  let serverExitedEarly = false;

  procs.server = spawnProc(
    ["bash", WRAPPER_SH, "server"],
    "server-log",
    (code) => {
      if (!serverReady) {
        serverExitedEarly = true;
      }
    }
  );

  // Wait for server ready signal, but also watch for early exit
  const matched = await Promise.race([
    waitForPattern("server-log", SERVER_READY_PATTERN, SERVER_READY_TIMEOUT_MS),
    // Poll for early exit
    new Promise((resolve) => {
      const t = setInterval(() => {
        if (serverExitedEarly) { clearInterval(t); resolve(false); }
      }, 100);
    }),
  ]);

  if (!matched || serverExitedEarly) {
    send("server-log", "\n[Server failed to start — not launching clients]\n");
    setStatus("error");
    setButtonState("idle");
    isBuilding = false;
    return;
  }

  serverReady = true;

  // ── Step 2: launch clients with --skip-build (jar already built) ──────────
  setStatus("starting-clients");
  const stamp = timestamp();
  send("client1-log", `─── CLIENT 1  [${stamp}] ──────────────────────────────\n`);
  send("client2-log", `─── CLIENT 2  [${stamp}] ──────────────────────────────\n`);

  procs.client1 = spawnProc(["bash", WRAPPER_SH, "client", "--skip-build"], "client1-log");
  await sleep(500); // small stagger
  procs.client2 = spawnProc(["bash", WRAPPER_SH, "client", "--skip-build"], "client2-log");

  setStatus("running");
  setButtonState("idle");
  isBuilding = false;
}

// ── IPC from renderer ─────────────────────────────────────────────────────────

ipcMain.on("rebuild",       () => rebuild());
ipcMain.on("kill-all",      () => { killAll(); setStatus("stopped"); setButtonState("idle"); });
ipcMain.on("open-devtools", () => win?.webContents.openDevTools());
