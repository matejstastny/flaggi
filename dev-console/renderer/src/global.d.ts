import type { ButtonState, LogChannel, PanelStampUpdate, Status } from "./types"

interface WindowApi {
    rebuild: () => void
    killAll: () => void
    openDevtools: () => void
    onLog: (channel: LogChannel, cb: (data: string) => void) => () => void
    onStatus: (cb: (status: Status) => void) => () => void
    onButtonState: (cb: (state: ButtonState) => void) => () => void
    onPanelStamp: (cb: (update: PanelStampUpdate) => void) => () => void
}

declare global {
    interface Window {
        api: WindowApi
    }
}

export {}
