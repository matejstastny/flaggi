import { useEffect, useMemo, useRef, useState } from "react"
import { colorizeAnsi } from "./ansi"
import type { ButtonState, DebugObject, DebugPlayer, DebugState, LogChannel, PanelName, Status } from "./types"

const DEBUG_URL = "http://127.0.0.1:54323/state"
const POLL_MS = 250

const STATUS_LABELS: Record<Status, string> = {
    stopped: "stopped",
    stopping: "stopping…",
    building: "building…",
    "starting-server": "starting server…",
    "starting-clients": "starting clients…",
    running: "running",
    error: "build failed",
    "server-crashed": "server crashed"
}

const LOG_CHANNELS: LogChannel[] = ["server-log", "client1-log", "client2-log"]

const CHANNEL_TO_STAMP: Record<LogChannel, PanelName> = {
    "server-log": "server",
    "client1-log": "client1",
    "client2-log": "client2"
}

const SKIN_COLOR: Record<string, string> = {
    SKIN_RED: "#e03040",
    SKIN_BLUE: "#4080f0",
    SKIN_JESTER: "#9060d0",
    SKIN_VENOM: "#22b45a"
}

const KEY_LABEL: Record<string, string> = {
    KEY_UP: "↑",
    KEY_DOWN: "↓",
    KEY_LEFT: "←",
    KEY_RIGHT: "→",
    KEY_SHOOT: "✦"
}

const ALL_KEYS = ["KEY_UP", "KEY_DOWN", "KEY_LEFT", "KEY_RIGHT", "KEY_SHOOT"]
const GAME_VIEWPORT_CENTER_X = 400
const GAME_VIEWPORT_CENTER_Y = 300
const AIM_PROJECTION_SCALE = 0.3
const WORLD_BOUNDS_PADDING = 100

const initialLogState = () => ({
    "server-log": { chunks: [] as string[], lines: 0 },
    "client1-log": { chunks: [] as string[], lines: 0 },
    "client2-log": { chunks: [] as string[], lines: 0 }
})

interface WorldBounds {
    minX: number
    maxX: number
    minY: number
    maxY: number
}

export function App() {
    const [status, setStatus] = useState<Status>("stopped")
    const [buttonState, setButtonState] = useState<ButtonState>("idle")
    const [panelStamps, setPanelStamps] = useState<Record<PanelName, string | null>>({
        server: null,
        client1: null,
        client2: null
    })
    const [logs, setLogs] = useState(initialLogState)
    const [gameState, setGameState] = useState<DebugState | null>(null)
    const [worldBounds, setWorldBounds] = useState<WorldBounds>({
        minX: 0,
        maxX: 1000,
        minY: 0,
        maxY: 1000
    })

    const logRefs = useRef<Record<LogChannel, HTMLDivElement | null>>({
        "server-log": null,
        "client1-log": null,
        "client2-log": null
    })
    const autoScrollRef = useRef<Record<LogChannel, boolean>>({
        "server-log": false,
        "client1-log": false,
        "client2-log": false
    })
    const ansiStateRef = useRef<Record<LogChannel, { openSpans: number }>>({
        "server-log": { openSpans: 0 },
        "client1-log": { openSpans: 0 },
        "client2-log": { openSpans: 0 }
    })
    const pollTimerRef = useRef<number | null>(null)
    const canvasRef = useRef<HTMLCanvasElement | null>(null)

    useEffect(() => {
        const offLogs = LOG_CHANNELS.map((channel) =>
            window.api.onLog(channel, (data) => {
                const el = logRefs.current[channel]
                autoScrollRef.current[channel] = !el || el.scrollHeight - el.scrollTop - el.clientHeight < 80
                const html = colorizeAnsi(data, ansiStateRef.current[channel])
                const newLines = (data.match(/\n/g) ?? []).length
                setLogs((prev) => ({
                    ...prev,
                    [channel]: {
                        chunks: [...prev[channel].chunks, html],
                        lines: prev[channel].lines + newLines
                    }
                }))
            })
        )

        const offStatus = window.api.onStatus((nextStatus) => {
            setStatus(nextStatus)
            if (nextStatus === "stopping") clearLogs()
            if (nextStatus === "running") startPolling()
            if (nextStatus === "stopped" || nextStatus === "error" || nextStatus === "stopping") stopPolling()
        })

        const offButton = window.api.onButtonState((nextState) => {
            setButtonState(nextState)
        })

        const offStamp = window.api.onPanelStamp(({ panel, stamp }) => {
            setPanelStamps((prev) => ({ ...prev, [panel]: stamp }))
        })

        return () => {
            for (const off of offLogs) off()
            offStatus()
            offButton()
            offStamp()
            stopPolling()
        }
    }, [])

    useEffect(() => {
        for (const channel of LOG_CHANNELS) {
            if (!autoScrollRef.current[channel]) continue
            const el = logRefs.current[channel]
            if (el) el.scrollTop = el.scrollHeight
            autoScrollRef.current[channel] = false
        }
    }, [logs])

    useEffect(() => {
        renderMinimap(canvasRef.current, gameState, worldBounds)
    }, [gameState, worldBounds])

    useEffect(() => {
        const onResize = () => renderMinimap(canvasRef.current, gameState, worldBounds)
        window.addEventListener("resize", onResize)
        return () => window.removeEventListener("resize", onResize)
    }, [gameState, worldBounds])

    const statusClass = useMemo(() => {
        if (status === "running") return "s-running"
        if (status === "building" || status.startsWith("starting")) return "s-building"
        if (status === "error" || status === "server-crashed") return "s-error"
        return "s-stopped"
    }, [status])

    function clearLogs() {
        setLogs(initialLogState())
        for (const channel of LOG_CHANNELS) ansiStateRef.current[channel] = { openSpans: 0 }
    }

    function stopPolling() {
        if (pollTimerRef.current) {
            window.clearInterval(pollTimerRef.current)
            pollTimerRef.current = null
        }
        setGameState(null)
    }

    function startPolling() {
        stopPolling()
        void pollOnce()
        pollTimerRef.current = window.setInterval(() => {
            void pollOnce()
        }, POLL_MS)
    }

    async function pollOnce() {
        try {
            const res = await fetch(DEBUG_URL, { signal: AbortSignal.timeout(400) })
            if (!res.ok) return
            const data = (await res.json()) as DebugState
            if (!data?.players?.length) {
                setGameState(null)
                return
            }
            setGameState(data)
            setWorldBounds(calculateBounds(data))
        } catch {
            // silent fail when server is unavailable
        }
    }

    const players = gameState?.players ?? []

    return (
        <div id="app">
            <div id="toolbar">
                <div id="app-name">
                    <span className="flag-r">FLAG</span>
                    <span className="flag-b">GI</span>
                    <span className="flag-dev">DEV</span>
                </div>

                <div id="status-pill" className={statusClass}>
                    <div id="status-dot" />
                    <span id="status-text">{STATUS_LABELS[status] ?? status}</span>
                </div>

                <div className="spacer" />

                <button id="btn-clear" onClick={clearLogs}>
                    Clear logs
                </button>
                <button id="btn-stop" onClick={() => window.api.killAll()}>
                    Stop all
                </button>
                <button
                    id="btn-rebuild"
                    className={buttonState === "building" ? "building" : ""}
                    disabled={buttonState === "building"}
                    onClick={() => window.api.rebuild()}
                >
                    {buttonState === "building" ? "Building…" : "▶ Rebuild"}
                </button>
            </div>

            <div id="main" style={{ display: "flex", flexDirection: "column", flex: 1, minHeight: 0, gap: "10px" }}>
                <div id="log-row">
                    <LogPanel
                        id="panel-server"
                        label="Server"
                        logRef={(el) => (logRefs.current["server-log"] = el)}
                        chunks={logs["server-log"].chunks}
                        lines={logs["server-log"].lines}
                        stamp={panelStamps.server}
                    />
                    <LogPanel
                        id="panel-client1"
                        label="Client 1"
                        logRef={(el) => (logRefs.current["client1-log"] = el)}
                        chunks={logs["client1-log"].chunks}
                        lines={logs["client1-log"].lines}
                        stamp={panelStamps.client1}
                    />
                    <LogPanel
                        id="panel-client2"
                        label="Client 2"
                        logRef={(el) => (logRefs.current["client2-log"] = el)}
                        chunks={logs["client2-log"].chunks}
                        lines={logs["client2-log"].lines}
                        stamp={panelStamps.client2}
                    />
                </div>

                <div className="panel" id="game-panel">
                    <div id="minimap-wrap">
                        <div className="panel-header">
                            <span className="panel-label">Game State</span>
                            <span className="panel-badge panel-badge-right" id="tick-badge">
                                {gameState ? `tick ${gameState.tick ?? "?"}` : "no game"}
                            </span>
                        </div>
                        <canvas id="minimap-canvas" ref={canvasRef} />
                    </div>

                    <div id="player-cards">
                        <div className="panel-header">
                            <span className="panel-label">Players</span>
                        </div>
                        <div id="cards-inner">
                            {players.length === 0 ? (
                                <div id="no-game">No active game</div>
                            ) : (
                                players.map((player) => <PlayerCard key={player.uuid} player={player} />)
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

function LogPanel({
    id,
    label,
    stamp,
    lines,
    chunks,
    logRef
}: {
    id: string
    label: string
    stamp: string | null
    lines: number
    chunks: string[]
    logRef: (el: HTMLDivElement | null) => void
}) {
    return (
        <div className="panel" id={id}>
            <div className="panel-header">
                <span className="panel-label">{label}</span>
                {stamp ? <span className="panel-badge">{stamp}</span> : null}
                <span className="panel-badge panel-badge-right">{lines} lines</span>
            </div>
            <div className="log" ref={logRef}>
                {chunks.map((chunk, index) => (
                    <span key={index} dangerouslySetInnerHTML={{ __html: chunk }} />
                ))}
            </div>
        </div>
    )
}

function PlayerCard({ player }: { player: DebugPlayer }) {
    const hp = clamp(player.hp ?? 100, 0, 100)
    const hpColor = hp > 50 ? "#22b45a" : hp > 25 ? "#d4900a" : "#e03040"
    const skinColor = SKIN_COLOR[player.skin ?? ""] ?? "#dde3f0"
    const heldKeys = new Set(player.keys ?? [])
    const name = player.username || player.uuid.slice(0, 8)

    return (
        <div className="player-card">
            <div className="card-name" style={{ color: skinColor }}>
                {name}
            </div>
            <div className="card-row">
                <span className="card-label">HP</span>
                <div className="hp-bar-track">
                    <div className="hp-bar-fill" style={{ width: `${hp}%`, background: hpColor }} />
                </div>
                <span className="card-val">{Math.round(hp)}</span>
            </div>
            <div className="card-row">
                <span className="card-label">XY</span>
                <span className="card-val">
                    {Math.round(player.x)}, {Math.round(player.y)}
                </span>
                <span className="card-label" style={{ marginLeft: "6px" }}>
                    🚩
                </span>
                <span className="card-val">{player.flagCount ?? 0}</span>
            </div>
            <div className="card-row">
                <span className="card-label">🖱</span>
                <span className="card-val">
                    {player.mouse?.x ?? 0}, {player.mouse?.y ?? 0}
                </span>
            </div>
            <div className="card-row">
                <div className="keys-wrap">
                    {ALL_KEYS.map((key) => (
                        <span key={key} className={`key-chip ${heldKeys.has(key) ? "active" : ""}`}>
                            {KEY_LABEL[key]}
                        </span>
                    ))}
                </div>
            </div>
            <div className="card-row" style={{ fontSize: "9px", color: "var(--muted)" }}>
                {(player.skin ?? "?").replace("SKIN_", "")} · {(player.animation ?? "?").replace("ANIM_", "")}
            </div>
        </div>
    )
}

function clamp(n: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, n))
}

function calculateBounds(data: DebugState): WorldBounds {
    let minX = Infinity
    let maxX = -Infinity
    let minY = Infinity
    let maxY = -Infinity

    for (const p of data.players) {
        minX = Math.min(minX, p.x)
        maxX = Math.max(maxX, p.x)
        minY = Math.min(minY, p.y)
        maxY = Math.max(maxY, p.y)
        for (const o of p.others ?? []) {
            minX = Math.min(minX, o.x)
            maxX = Math.max(maxX, o.x)
            minY = Math.min(minY, o.y)
            maxY = Math.max(maxY, o.y)
        }
    }

    const pad = WORLD_BOUNDS_PADDING
    return { minX: minX - pad, maxX: maxX + pad, minY: minY - pad, maxY: maxY + pad }
}

function renderMinimap(canvas: HTMLCanvasElement | null, gameState: DebugState | null, worldBounds: WorldBounds) {
    if (!canvas) return
    const ctx = canvas.getContext("2d")
    if (!ctx) return

    resizeCanvas(canvas)

    ctx.fillStyle = "#0a0d14"
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    if (!gameState) {
        ctx.fillStyle = "#4a5680"
        ctx.font = "11px sans-serif"
        ctx.textAlign = "center"
        ctx.fillText("No active game", canvas.width / 2, canvas.height / 2)
        return
    }

    ctx.strokeStyle = "#1e2438"
    ctx.lineWidth = 0.5
    const step = 100
    for (let wx = Math.ceil(worldBounds.minX / step) * step; wx < worldBounds.maxX; wx += step) {
        const { x } = worldToCanvas(wx, 0, worldBounds, canvas)
        ctx.beginPath()
        ctx.moveTo(x, 0)
        ctx.lineTo(x, canvas.height)
        ctx.stroke()
    }
    for (let wy = Math.ceil(worldBounds.minY / step) * step; wy < worldBounds.maxY; wy += step) {
        const { y } = worldToCanvas(0, wy, worldBounds, canvas)
        ctx.beginPath()
        ctx.moveTo(0, y)
        ctx.lineTo(canvas.width, y)
        ctx.stroke()
    }

    const drawn = new Set<string>()

    for (const player of gameState.players) {
        const key = `${Math.round(player.x)},${Math.round(player.y)}`
        if (!drawn.has(key)) {
            drawn.add(key)
            drawPlayer(ctx, canvas, player, true, worldBounds)
        }
        for (const obj of player.others ?? []) {
            const objectKey = `${Math.round(obj.x)},${Math.round(obj.y)}`
            if (drawn.has(objectKey)) continue
            drawn.add(objectKey)
            if (obj.type === "PLAYER") drawOtherPlayer(ctx, canvas, obj, worldBounds)
            if (obj.type === "FLAG") drawFlag(ctx, canvas, obj.x, obj.y, worldBounds)
            if (obj.type === "BULLET") drawBullet(ctx, canvas, obj.x, obj.y, worldBounds)
        }
    }
}

function resizeCanvas(canvas: HTMLCanvasElement) {
    const parent = canvas.parentElement
    if (!parent) return
    const header = parent.querySelector(".panel-header")
    const headerHeight = header instanceof HTMLElement ? header.offsetHeight : 32
    const rect = parent.getBoundingClientRect()
    canvas.width = Math.max(rect.width, 10)
    canvas.height = Math.max(rect.height - headerHeight, 10)
}

function worldToCanvas(wx: number, wy: number, worldBounds: WorldBounds, canvas: HTMLCanvasElement) {
    const sx = (wx - worldBounds.minX) / (worldBounds.maxX - worldBounds.minX)
    const sy = (wy - worldBounds.minY) / (worldBounds.maxY - worldBounds.minY)
    return { x: sx * canvas.width, y: sy * canvas.height }
}

function drawPlayer(
    ctx: CanvasRenderingContext2D,
    canvas: HTMLCanvasElement,
    player: DebugPlayer,
    isLocal: boolean,
    worldBounds: WorldBounds
) {
    const { x, y } = worldToCanvas(player.x, player.y, worldBounds, canvas)
    const radius = isLocal ? 7 : 5
    const color = SKIN_COLOR[player.skin ?? ""] ?? "#dde3f0"

    if (isLocal) {
        ctx.shadowColor = color
        ctx.shadowBlur = 12
    }

    ctx.beginPath()
    ctx.arc(x, y, radius, 0, Math.PI * 2)
    ctx.fillStyle = color
    ctx.fill()
    ctx.strokeStyle = isLocal ? "#fff" : "#1e2438"
    ctx.lineWidth = isLocal ? 1.5 : 1
    ctx.stroke()
    ctx.shadowBlur = 0

    const barW = 22
    const barH = 3
    const hpFraction = clamp((player.hp ?? 100) / 100, 0, 1)
    ctx.fillStyle = "#1e2438"
    ctx.fillRect(x - barW / 2, y - radius - 7, barW, barH)
    ctx.fillStyle = hpFraction > 0.5 ? "#22b45a" : hpFraction > 0.25 ? "#d4900a" : "#e03040"
    ctx.fillRect(x - barW / 2, y - radius - 7, barW * hpFraction, barH)

    if (isLocal && player.mouse) {
        const aimX = player.x + (player.mouse.x - GAME_VIEWPORT_CENTER_X) * AIM_PROJECTION_SCALE
        const aimY = player.y + (player.mouse.y - GAME_VIEWPORT_CENTER_Y) * AIM_PROJECTION_SCALE
        const { x: aimCanvasX, y: aimCanvasY } = worldToCanvas(aimX, aimY, worldBounds, canvas)
        ctx.beginPath()
        ctx.moveTo(x, y)
        ctx.lineTo(aimCanvasX, aimCanvasY)
        ctx.strokeStyle = "rgba(224,48,64,0.25)"
        ctx.lineWidth = 1
        ctx.stroke()
    }

    if (player.username) {
        ctx.fillStyle = "#4a5680"
        ctx.font = "9px sans-serif"
        ctx.textAlign = "center"
        ctx.fillText(player.username, x, y + radius + 11)
    }
}

function drawOtherPlayer(
    ctx: CanvasRenderingContext2D,
    canvas: HTMLCanvasElement,
    obj: DebugObject,
    worldBounds: WorldBounds
) {
    drawPlayer(
        ctx,
        canvas,
        {
            uuid: `other-${obj.x}-${obj.y}`,
            x: obj.x,
            y: obj.y,
            hp: obj.hp,
            skin: obj.skin,
            username: obj.username,
            mouse: null
        },
        false,
        worldBounds
    )
}

function drawFlag(
    ctx: CanvasRenderingContext2D,
    canvas: HTMLCanvasElement,
    worldX: number,
    worldY: number,
    worldBounds: WorldBounds
) {
    const { x, y } = worldToCanvas(worldX, worldY, worldBounds, canvas)
    ctx.fillStyle = "#4a5680"
    ctx.fillRect(x - 0.5, y - 10, 1.5, 14)
    ctx.fillStyle = (Math.round(worldX) + Math.round(worldY)) % 2 === 0 ? "#e03040" : "#4080f0"
    ctx.beginPath()
    ctx.moveTo(x + 1, y - 10)
    ctx.lineTo(x + 8, y - 6)
    ctx.lineTo(x + 1, y - 2)
    ctx.closePath()
    ctx.fill()
}

function drawBullet(
    ctx: CanvasRenderingContext2D,
    canvas: HTMLCanvasElement,
    worldX: number,
    worldY: number,
    worldBounds: WorldBounds
) {
    const { x, y } = worldToCanvas(worldX, worldY, worldBounds, canvas)
    ctx.shadowColor = "#f07030"
    ctx.shadowBlur = 6
    ctx.beginPath()
    ctx.arc(x, y, 2.5, 0, Math.PI * 2)
    ctx.fillStyle = "#f09050"
    ctx.fill()
    ctx.shadowBlur = 0
}
