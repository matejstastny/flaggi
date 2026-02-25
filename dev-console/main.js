const { app, BrowserWindow, ipcMain } = require("electron");
const { spawn } = require("child_process");
const path = require("path");

// ── CONFIG ──────────────────────────────────────────────────────────────────

const FLAGGI_ROOT = path.resolve(__dirname, "..");
const RUN_SH      = path.join(FLAGGI_ROOT, "scripts", "run.sh");

const SERVER_READY_PATTERN    = /Application start/;
const SERVER_READY_TIMEOUT_MS = 30_000;

// Matches the "Launching <jar>..." line in run.sh output, e.g:
//   [INFO] Launching flaggi-server-1.0.0.jar...
// We grab the jar name from there and look for it under DIR_SERVER_TEMP.
const LAUNCHING_PATTERN = /Launching (.+\.jar)/;
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

// ── Pattern watcher ───────────────────────────────────────────────────────────

function waitForPattern(logChannel, pattern, timeoutMs) {
  return new Promise((resolve) => {
    if (!logWatchers[logChannel]) logWatchers[logChannel] = [];

    const done = () => {
      logWatchers[logChannel] = logWatchers[logChannel].filter((f) => f !== watcher);
      clearTimeout(timer);
      resolve(null);
    };

    const watcher = (data) => {
      const m = data.match(pattern);
      if (m) {
        logWatchers[logChannel] = logWatchers[logChannel].filter((f) => f !== watcher);
        clearTimeout(timer);
        resolve(m); // resolve with match object so callers can extract groups
      }
    };

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

  setStatus("building");
  send("server-log", `\n─── BUILD + SERVER  [${timestamp()}] ─────────────────\n`);

  let serverReady       = false;
  let serverExitedEarly = false;
  let capturedJarName   = null;

  procs.server = spawnProc(["bash", RUN_SH, "server"], "server-log", (code) => {
    if (!serverReady) {
      serverExitedEarly = true;
      setStatus("error");
      setButtonState("idle");
      isBuilding = false;
    }
  });

  // Concurrently watch for two patterns in the server log:
  //   1. "Launching <jar>.jar..." — so we know the jar name/location
  //   2. SERVER_READY_PATTERN     — so we know the server is up
  const [launchMatch] = await Promise.all([
    waitForPattern("server-log", LAUNCHING_PATTERN, SERVER_READY_TIMEOUT_MS),
    waitForPattern("server-log", SERVER_READY_PATTERN, SERVER_READY_TIMEOUT_MS),
    // bail-out poller
    new Promise((resolve) => {
      const t = setInterval(() => {
        if (serverExitedEarly) { clearInterval(t); resolve(); }
      }, 150);
    }),
  ]);

  if (serverExitedEarly) return;
  serverReady = true;

  // The jar was moved to DIR_SERVER_TEMP by run.sh.
  // We grab the name from the log line and find it via `find`.
  if (launchMatch && launchMatch[1]) {
    capturedJarName = launchMatch[1]; // e.g. "flaggi-server-1.0.0.jar"
  }

  const jarPath = await resolveClientJar(capturedJarName);
  if (!jarPath) {
    send("server-log", "\n[ERROR: could not find client jar — check build output]\n");
    setStatus("error");
    setButtonState("idle");
    isBuilding = false;
    return;
  }

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

// Find the client jar. The shadowjar is a single fat jar — for server, run.sh
// moves it into DIR_SERVER_TEMP. The original build output dir still has it
// (or we find it via `find`). We locate any flaggi*.jar under the repo root,
// excluding the server temp dir.
function resolveClientJar(capturedJarName) {
  return new Promise((resolve) => {
    // Use `find` to locate the jar under build/libs, excluding server-temp
    const proc = spawn("bash", ["-c",
      `find "${FLAGGI_ROOT}" -name "*.jar" -path "*/build/libs/*" | head -1`
    ], { cwd: FLAGGI_ROOT });

    let out = "";
    proc.stdout.on("data", (d) => (out += d));
    proc.on("close", () => resolve(out.trim() || null));
  });
}

// ── IPC from renderer ─────────────────────────────────────────────────────────

ipcMain.on("rebuild",       () => rebuild());
ipcMain.on("kill-all",      () => { killAll(); setStatus("stopped"); setButtonState("idle"); });
ipcMain.on("open-devtools", () => win?.webContents.openDevTools());
