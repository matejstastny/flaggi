const { contextBridge, ipcRenderer } = require("electron")

contextBridge.exposeInMainWorld("api", {
    rebuild: () => ipcRenderer.send("rebuild"),
    killAll: () => ipcRenderer.send("kill-all"),
    openDevtools: () => ipcRenderer.send("open-devtools"),

    onLog: (channel, cb) => {
        const handler = (_, data) => cb(data)
        ipcRenderer.on(channel, handler)
        return () => ipcRenderer.removeListener(channel, handler)
    },
    onStatus: (cb) => {
        const handler = (_, s) => cb(s)
        ipcRenderer.on("status", handler)
        return () => ipcRenderer.removeListener("status", handler)
    },
    onButtonState: (cb) => {
        const handler = (_, s) => cb(s)
        ipcRenderer.on("button-state", handler)
        return () => ipcRenderer.removeListener("button-state", handler)
    },
    onPanelStamp: (cb) => {
        const handler = (_, d) => cb(d)
        ipcRenderer.on("panel-stamp", handler)
        return () => ipcRenderer.removeListener("panel-stamp", handler)
    }
})
