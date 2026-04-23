import { contextBridge, ipcRenderer, type IpcRendererEvent } from "electron"

type LogChannel = "server-log" | "client1-log" | "client2-log"
type Status =
    | "stopped"
    | "stopping"
    | "building"
    | "starting-server"
    | "starting-clients"
    | "running"
    | "error"
    | "server-crashed"
type ButtonState = "idle" | "building"
type PanelName = "server" | "client1" | "client2"

interface PanelStampUpdate {
    panel: PanelName
    stamp: string | null
}

contextBridge.exposeInMainWorld("api", {
    rebuild: () => ipcRenderer.send("rebuild"),
    killAll: () => ipcRenderer.send("kill-all"),
    openDevtools: () => ipcRenderer.send("open-devtools"),

    onLog: (channel: LogChannel, cb: (data: string) => void) => {
        const handler = (_event: IpcRendererEvent, data: string) => cb(data)
        ipcRenderer.on(channel, handler)
        return () => ipcRenderer.removeListener(channel, handler)
    },
    onStatus: (cb: (status: Status) => void) => {
        const handler = (_event: IpcRendererEvent, status: Status) => cb(status)
        ipcRenderer.on("status", handler)
        return () => ipcRenderer.removeListener("status", handler)
    },
    onButtonState: (cb: (state: ButtonState) => void) => {
        const handler = (_event: IpcRendererEvent, state: ButtonState) => cb(state)
        ipcRenderer.on("button-state", handler)
        return () => ipcRenderer.removeListener("button-state", handler)
    },
    onPanelStamp: (cb: (update: PanelStampUpdate) => void) => {
        const handler = (_event: IpcRendererEvent, update: PanelStampUpdate) => cb(update)
        ipcRenderer.on("panel-stamp", handler)
        return () => ipcRenderer.removeListener("panel-stamp", handler)
    }
})
