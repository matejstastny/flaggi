const { app, BrowserWindow, ipcMain } = require("electron");
const { spawn } = require("child_process");
const path = require("path");

// ── CONFIG ──────────────────────────────────────────────────────────────────
const FLAGGI_ROOT  = path.resolve(__dirname, "..");
const WRAPPER_SH   = path.join(FLAGGI_ROOT, "scripts", "run-wrapper.sh");

const SERVER_READY_PATTERN    = /Application start/;
const SERVER_READY_TIMEOUT_MS = 60_000;
// ────────────────────────────────────────────────────────────────────────────

let win;
let procs      = { server: null, client1: null, client2: null };
let isBuilding = false;
const logWatchers = {};

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
app.on("before-quit", () => killAll());
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
// Send a timestamp to display in a panel header badge
function setPanelStamp(panel, stamp) { send("panel-stamp", { panel, stamp }); }

function timestamp() {
  return new Date().toLocaleTimeString([], {
    hour: "2-digit", minute: "2-digit", second: "2-digit",
  });
}

// ── Process management ────────────────────────────────────────────────────────

function killAll() {
  for (const [key, proc] of Object.entries(procs)) {
    if (proc) {
      try { process.kill(-proc.pid, "SIGKILL"); } catch (_) {}
      try { proc.kill("SIGKILL"); }               catch (_) {}
      procs[key] = null;
    }
  }
}

function exitMessage(code, signal) {
  if (code === null || code === undefined) return "[process stopped]\n";
  if (code === 0) return "[process exited cleanly]\n";
  return `[process exited with code ${code}]\n`;
}

function spawnProc(args, logChannel, onClose) {
  const proc = spawn(args[0], args.slice(1), {
    cwd: FLAGGI_ROOT,
    detached: true,
    env: { ...process.env },
  });

  proc.stdout.on("data", (d) => send(logChannel, d.toString()));
  proc.stderr.on("data", (d) => send(logChannel, d.toString()));
  proc.on("close", (code, signal) => {
    send(logChannel, "\n" + exitMessage(code, signal));
    onClose?.(code);
  });
  proc.on("error", (err) => {
    send(logChannel, `\n[spawn error: ${err.message}]\n`);
    onClose?.(-1);
  });

  return proc;
}

// ── Pattern watcher ───────────────────────────────────────────────────────────

function waitForPattern(logChannel, pattern, timeoutMs) {
  return new Promise((resolve) => {
    if (!logWatchers[logChannel]) logWatchers[logChannel] = [];

    const cleanup = () => {
      logWatchers[logChannel] = logWatchers[logChannel].filter((f) => f !== watcher);
      clearTimeout(timer);
    };

    const watcher = (data) => {
      if (pattern.test(data)) { cleanup(); resolve(true); }
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

  // Clear panel stamps on rebuild
  setPanelStamp("server",  null);
  setPanelStamp("client1", null);
  setPanelStamp("client2", null);

  killAll();
  await sleep(600);

  // ── Step 1: build + start server ─────────────────────────────────────────
  setStatus("building");
  const serverStamp = timestamp();
  setPanelStamp("server", serverStamp);

  let serverReady       = false;
  let serverExitedEarly = false;

  procs.server = spawnProc(
    ["bash", WRAPPER_SH, "server"],
    "server-log",
    (code) => { if (!serverReady) serverExitedEarly = true; }
  );

  const matched = await Promise.race([
    waitForPattern("server-log", SERVER_READY_PATTERN, SERVER_READY_TIMEOUT_MS),
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

  // ── Step 2: launch clients ────────────────────────────────────────────────
  setStatus("starting-clients");
  const clientStamp = timestamp();
  setPanelStamp("client1", clientStamp);
  setPanelStamp("client2", clientStamp);

  procs.client1 = spawnProc(["bash", WRAPPER_SH, "client", "--skip-build"], "client1-log");
  await sleep(500);
  procs.client2 = spawnProc(["bash", WRAPPER_SH, "client", "--skip-build"], "client2-log");

  setStatus("running");
  setButtonState("idle");
  isBuilding = false;
}

// ── IPC from renderer ─────────────────────────────────────────────────────────

ipcMain.on("rebuild",       () => rebuild());
ipcMain.on("kill-all",      () => {
  killAll();
  setStatus("stopped");
  setButtonState("idle");
  setPanelStamp("server",  null);
  setPanelStamp("client1", null);
  setPanelStamp("client2", null);
});
ipcMain.on("open-devtools", () => win?.webContents.openDevTools());
