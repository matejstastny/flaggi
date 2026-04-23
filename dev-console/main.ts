import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process"
import { app, BrowserWindow, ipcMain } from "electron"
import path from "node:path"

// Config ----------------------------------------------------------------------

const DEV_CONSOLE_ROOT = path.resolve(__dirname, "..")
const FLAGGI_ROOT = path.resolve(DEV_CONSOLE_ROOT, "..")
const RUN_SCRIPT = path.join(FLAGGI_ROOT, "scripts", "run.sh")

const SERVER_READY_PATTERN = /Application start/
const SERVER_READY_TIMEOUT_MS = 60_000

// -----------------------------------------------------------------------------

let win: BrowserWindow | null = null
let procs: Record<string, ChildProcessWithoutNullStreams | null> = {
    server: null,
    client1: null,
    client2: null
}
let isBuilding = false
const logWatchers: Record<string, Array<(data: string) => void>> = {}

// Window --------------------------------------------------------------------

function createWindow() {
    win = new BrowserWindow({
        width: 1200,
        height: 860,
        minWidth: 900,
        minHeight: 640,
        title: "Flaggi Dev Console",
        icon: path.join(FLAGGI_ROOT, "assets", "icons", "console_icon.png"),
        backgroundColor: "#0a0d14",
        titleBarStyle: "hidden",
        trafficLightPosition: { x: 14, y: 14 },
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            preload: path.join(__dirname, "preload.js")
        }
    })

    void win.loadFile(path.join(DEV_CONSOLE_ROOT, "dist", "renderer", "index.html"))
}

void app.whenReady().then(() => {
    createWindow()
})
app.setName("Flaggi Dev Console")
app.on("before-quit", () => killAll())
app.on("window-all-closed", () => {
    killAll()
    app.quit()
})

// Helpers -------------------------------------------------------------------

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms))

function send(channel: string, data: unknown) {
    const watchers = logWatchers[channel]
    if (watchers) {
        for (const fn of [...watchers]) fn(String(data))
    }
    if (win && !win.isDestroyed()) win.webContents.send(channel, data)
}

const setStatus = (status: string) => send("status", status)
const setButtonState = (state: string) => send("button-state", state)
const setPanelStamp = (panel: string, stamp: string | null) => send("panel-stamp", { panel, stamp })

function timestamp() {
    return new Date().toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    })
}

// Process management --------------------------------------------------------

function killAll() {
    for (const [key, proc] of Object.entries(procs)) {
        if (proc) {
            try {
                if (proc.pid !== undefined) process.kill(-proc.pid, "SIGKILL")
            } catch {
                // ignore
            }
            try {
                proc.kill("SIGKILL")
            } catch {
                // ignore
            }
            procs[key] = null
        }
    }
}

function exitMessage(code: number | null) {
    if (code === null || code === undefined) return "[process stopped]\n"
    if (code === 0) return "[process exited cleanly]\n"
    return `[process exited with code ${code}]\n`
}

function spawnProc(
    args: string[],
    logChannel: string,
    onClose?: (code: number | null) => void,
    extraEnv: Record<string, string> = {}
) {
    type ProcWithEvents = ChildProcessWithoutNullStreams & {
        on(event: "close", listener: (code: number | null) => void): ProcWithEvents
        on(event: "error", listener: (err: Error) => void): ProcWithEvents
    }

    const proc = spawn(args[0], args.slice(1), {
        cwd: FLAGGI_ROOT,
        detached: true,
        env: { ...process.env, ...extraEnv }
    }) as ProcWithEvents

    proc.stdout.on("data", (data) => send(logChannel, data.toString()))
    proc.stderr.on("data", (data) => send(logChannel, data.toString()))
    proc.on("close", (code: number | null) => {
        send(logChannel, `\n${exitMessage(code)}`)
        onClose?.(code)
    })
    proc.on("error", (err: Error) => {
        send(logChannel, `\n[spawn error: ${err.message}]\n`)
        onClose?.(-1)
    })

    return proc
}

// Pattern watcher -----------------------------------------------------------

function waitForPattern(logChannel: string, pattern: RegExp, timeoutMs: number) {
    return new Promise<boolean>((resolve) => {
        if (!logWatchers[logChannel]) logWatchers[logChannel] = []

        const cleanup = () => {
            logWatchers[logChannel] = logWatchers[logChannel].filter((fn) => fn !== watcher)
            clearTimeout(timer)
        }

        const watcher = (data: string) => {
            if (pattern.test(data)) {
                cleanup()
                resolve(true)
            }
        }
        logWatchers[logChannel].push(watcher)
        const timer = setTimeout(() => {
            cleanup()
            resolve(false)
        }, timeoutMs)
    })
}

// Build + launch flow -------------------------------------------------------

async function rebuild() {
    if (isBuilding) return
    isBuilding = true
    setButtonState("building")
    setStatus("stopping")

    setPanelStamp("server", null)
    setPanelStamp("client1", null)
    setPanelStamp("client2", null)

    killAll()
    await sleep(600)

    // Step 1: build + start server
    setStatus("building")
    setPanelStamp("server", timestamp())

    let serverReady = false
    let serverExitedEarly = false

    procs.server = spawnProc(["bash", RUN_SCRIPT, "server"], "server-log", () => {
        if (!serverReady) serverExitedEarly = true
    })

    const matched = await Promise.race([
        waitForPattern("server-log", SERVER_READY_PATTERN, SERVER_READY_TIMEOUT_MS),
        new Promise<boolean>((resolve) => {
            const timer = setInterval(() => {
                if (serverExitedEarly) {
                    clearInterval(timer)
                    resolve(false)
                }
            }, 100)
        })
    ])

    if (!matched || serverExitedEarly) {
        send("server-log", "\n[Server failed to start - not launching clients]\n")
        setStatus("error")
        setButtonState("idle")
        isBuilding = false
        return
    }

    serverReady = true

    // Step 2: launch clients (jar already built, skip rebuild)
    setStatus("starting-clients")
    const clientStamp = timestamp()
    setPanelStamp("client1", clientStamp)
    setPanelStamp("client2", clientStamp)

    procs.client1 = spawnProc(["bash", RUN_SCRIPT, "client", "--skip-build"], "client1-log", undefined, {
        FLAGGI_DEV: "true"
    })
    await sleep(500)
    procs.client2 = spawnProc(["bash", RUN_SCRIPT, "client", "--skip-build"], "client2-log", undefined, {
        FLAGGI_DEV: "true"
    })

    setStatus("running")
    setButtonState("idle")
    isBuilding = false
}

// IPC -----------------------------------------------------------------------

ipcMain.on("rebuild", () => {
    void rebuild()
})
ipcMain.on("kill-all", () => {
    killAll()
    setStatus("stopped")
    setButtonState("idle")
    setPanelStamp("server", null)
    setPanelStamp("client1", null)
    setPanelStamp("client2", null)
})
ipcMain.on("open-devtools", () => win?.webContents.openDevTools())
