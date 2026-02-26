/**
 * ipc.js
 * Single entry point that wires window.api IPC events to the
 * appropriate module functions. Import this last in index.html.
 */

window.api.onLog("server-log", (d) => appendLog("server-log", d))
window.api.onLog("client1-log", (d) => appendLog("client1-log", d))
window.api.onLog("client2-log", (d) => appendLog("client2-log", d))

// Status -----------------------------------------------------------------------

const STATUS_LABELS = {
    stopped: "stopped",
    stopping: "stopping…",
    building: "building…",
    "starting-server": "starting server…",
    "starting-clients": "starting clients…",
    running: "running",
    error: "build failed",
    "server-crashed": "server crashed"
}

const pill = document.getElementById("status-pill")
const statusText = document.getElementById("status-text")

window.api.onStatus((s) => {
    const cls =
        s === "running"
            ? "s-running"
            : s === "building" || s.startsWith("starting")
              ? "s-building"
              : s === "error" || s === "server-crashed"
                ? "s-error"
                : "s-stopped"
    pill.className = cls
    statusText.textContent = STATUS_LABELS[s] ?? s

    if (s === "running") startPolling()
    if (s === "stopped" || s === "error" || s === "stopping") stopPolling()
})

// Button state -----------------------------------------------------------------

const btnRebuild = document.getElementById("btn-rebuild")

window.api.onButtonState((state) => {
    if (state === "building") {
        btnRebuild.textContent = "Building…"
        btnRebuild.classList.add("building")
        btnRebuild.disabled = true
    } else {
        btnRebuild.textContent = "▶ Rebuild"
        btnRebuild.classList.remove("building")
        btnRebuild.disabled = false
    }
})

window.api.onPanelStamp(({ panel, stamp }) => setPanelStamp(panel, stamp))

btnRebuild.addEventListener("click", () => window.api.rebuild())

document.getElementById("btn-stop").addEventListener("click", () => window.api.killAll())

document.getElementById("btn-clear").addEventListener("click", () => clearLogs())
