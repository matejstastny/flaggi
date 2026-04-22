const { contextBridge, ipcRenderer } = require("electron")

contextBridge.exposeInMainWorld("api", {
    rebuild: () => ipcRenderer.send("rebuild"),
    killAll: () => ipcRenderer.send("kill-all"),
    openDevtools: () => ipcRenderer.send("open-devtools"),

    onLog: (channel, cb) => ipcRenderer.on(channel, (_, data) => cb(data)),
    onStatus: (cb) => ipcRenderer.on("status", (_, s) => cb(s)),
    onButtonState: (cb) => ipcRenderer.on("button-state", (_, s) => cb(s)),
    onPanelStamp: (cb) => ipcRenderer.on("panel-stamp", (_, d) => cb(d))
})
