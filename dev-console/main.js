const { app, BrowserWindow, ipcMain } = require("electron");
const { spawn } = require("child_process");
const path = require("path");

// ── CONFIG ──────────────────────────────────────────────────────────────────
// flaggi-dev lives inside the repo root, so repo root is one level up
const FLAGGI_ROOT = path.resolve(__dirname, "..");
const RUN_SH     = path.join(FLAGGI_ROOT, "scripts", "run.sh");
const LIB_CONFIG = path.join(FLAGGI_ROOT, "scripts", "lib/config.sh");
const LIB_SHARED = path.join(FLAGGI_ROOT, "scripts", "lib/shared.sh");

// Edit this to match whatever your server prints when it's bound and listening.
const SERVER_READY_PATTERN = /Application start/;
const SERVER_READY_TIMEOUT_MS = 30_000;
// ────────────────────────────────────────────────────────────────────────────

let win;
let procs      = { server: null, client1: null, client2: null };
let isBuilding = false;
const logWatchers = {}; // { "server-log": [fn, ...], ... }

// ── Window ───────────────────────────────────────────────────────────────────

function createWindow() {
  win = new BrowserWindow({
    width: 1100,
    height: 820,
    minWidth: 800,
    minHeight: 600,
    title: "Flaggi Dev",
    backgroundColor: "#0f1117",
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
  return new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
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
    detached: true,
    env: { ...process.env },
  });

  proc.stdout.on("data", (d) => send(logChannel, d.toString()));
  proc.stderr.on("data", (d) => send(logChannel, d.toString()));
  proc.on("close", (code) => {
    send(logChannel, `\n[process exited with code ${code}]\n`);
    onClose?.(code);
  });
  proc.on("error", (err) => send(logChannel, `\n[spawn error: ${err.message}]\n`));

  return proc;
}

// Source the lib scripts and call get_shadowjar_path, same as run.sh does
function getShadowjarPath() {
  return new Promise((resolve) => {
    const script = `source "${LIB_CONFIG}"; source "${LIB_SHARED}"; get_shadowjar_path`;
    const proc = spawn("bash", ["-c", script], { cwd: FLAGGI_ROOT });
    let out = "";
    proc.stdout.on("data", (d) => (out += d));
    proc.on("close", (code) => resolve(code === 0 ? out.trim() : null));
  });
}

// ── Pattern watcher ───────────────────────────────────────────────────────────

function waitForPattern(logChannel, pattern, timeoutMs) {
  return new Promise((resolve) => {
    if (!logWatchers[logChannel]) logWatchers[logChannel] = [];

    const done = () => {
      logWatchers[logChannel] = logWatchers[logChannel].filter((f) => f !== watcher);
      clearTimeout(timer);
      resolve();
    };

    const watcher = (data) => { if (pattern.test(data)) done(); };
    logWatchers[logChannel].push(watcher);
    const timer = setTimeout(done, timeoutMs);
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

  // Step 1: run.sh server — this builds the shadowjar then starts the server.
  // We wait until the server prints its ready string before launching clients.
  setStatus("building");
  send("server-log", `\n─── BUILD + SERVER  [${timestamp()}] ─────────────────\n`);

  let serverReady       = false;
  let serverExitedEarly = false;

  procs.server = spawnProc(["bash", RUN_SH, "server"], "server-log", (code) => {
    if (!serverReady) {
      serverExitedEarly = true;
      setStatus("error");
      setButtonState("idle");
      isBuilding = false;
    }
  });

  await Promise.race([
    waitForPattern("server-log", SERVER_READY_PATTERN, SERVER_READY_TIMEOUT_MS),
    new Promise((resolve) => {
      const t = setInterval(() => {
        if (serverExitedEarly) { clearInterval(t); resolve(); }
      }, 150);
    }),
  ]);

  if (serverExitedEarly) return;
  serverReady = true;

  // Step 2: find the jar that was just built
  const jarPath = await getShadowjarPath();
  if (!jarPath) {
    send("server-log", "\n[ERROR: could not resolve jar path — check lib/shared.sh]\n");
    setStatus("error");
    setButtonState("idle");
    isBuilding = false;
    return;
  }

  // Step 3: launch clients directly from the jar — no rebuild
  setStatus("starting-clients");
  const stamp = timestamp();
  send("client1-log", `─── CLIENT 1  [${stamp}] ──────────────────────────────\n`);
  send("client2-log", `─── CLIENT 2  [${stamp}] ──────────────────────────────\n`);

  procs.client1 = spawnProc(["java", "-jar", jarPath], "client1-log");
  await sleep(500);
  procs.client2 = spawnProc(["java", "-jar", jarPath], "client2-log");

  setStatus("running");
  setButtonState("idle");
  isBuilding = false;
}

// ── IPC from renderer ─────────────────────────────────────────────────────────

ipcMain.on("rebuild",       () => rebuild());
ipcMain.on("kill-all",      () => { killAll(); setStatus("stopped"); setButtonState("idle"); });
ipcMain.on("open-devtools", () => win?.webContents.openDevTools());
