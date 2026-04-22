const ANSI_FG: Record<number, string> = {
    30: "#2a3050",
    31: "#e03040",
    32: "#22b45a",
    33: "#d4900a",
    34: "#4080f0",
    35: "#9060d0",
    36: "#00b0c8",
    37: "#dde3f0",
    90: "#4a5680",
    91: "#ff5060",
    92: "#30d870",
    93: "#f0b030",
    94: "#60a0ff",
    95: "#c080ff",
    96: "#30d8f0",
    97: "#ffffff"
}

const ANSI_BG: Record<number, string> = {
    40: "#141820",
    41: "#5a0810",
    42: "#0a3018",
    43: "#403000",
    44: "#0a2050",
    45: "#300850",
    46: "#003840",
    47: "#303848"
}

export interface AnsiState {
    openSpans: number
}

export function colorizeAnsi(raw: string, state: AnsiState): string {
    const text = raw.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    const parts = text.split(/\x1b\[/)
    let result = parts[0] ?? ""

    for (let i = 1; i < parts.length; i++) {
        const part = parts[i] ?? ""
        const mIdx = part.indexOf("m")
        if (mIdx === -1) {
            result += part
            continue
        }

        const codes = part
            .slice(0, mIdx)
            .split(";")
            .map((n) => Number(n))
        const rest = part.slice(mIdx + 1)
        let style = ""

        for (const code of codes) {
            if (code === 0 || Number.isNaN(code)) {
                while (state.openSpans > 0) {
                    result += "</span>"
                    state.openSpans--
                }
                continue
            }
            if (code === 1) {
                style += "font-weight:bold;"
                continue
            }
            if (code === 2) {
                style += "opacity:0.55;"
                continue
            }
            if (ANSI_FG[code]) {
                style += `color:${ANSI_FG[code]};`
                continue
            }
            if (ANSI_BG[code]) style += `background:${ANSI_BG[code]};`
        }

        if (style) {
            result += `<span style="${style}">`
            state.openSpans++
        }

        result += rest
    }

    return result
}
