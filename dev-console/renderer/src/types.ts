export type Status =
    | "stopped"
    | "stopping"
    | "building"
    | "starting-server"
    | "starting-clients"
    | "running"
    | "error"
    | "server-crashed"

export type ButtonState = "idle" | "building"
export type LogChannel = "server-log" | "client1-log" | "client2-log"
export type PanelName = "server" | "client1" | "client2"

export interface PanelStampUpdate {
    panel: PanelName
    stamp: string | null
}

export interface DebugPlayer {
    uuid: string
    username?: string
    x: number
    y: number
    hp?: number
    flagCount?: number
    skin?: string
    animation?: string
    keys?: string[]
    mouse?: { x: number; y: number } | null
    others?: DebugObject[]
}

export interface DebugObject {
    type?: "PLAYER" | "FLAG" | "BULLET" | string
    x: number
    y: number
    skin?: string
    animation?: string
    username?: string
    hp?: number
}

export interface DebugState {
    tick?: number
    players: DebugPlayer[]
}
