/**
 * gamestate.js
 * Polls the debug HTTP endpoint and renders:
 *   - Minimap canvas (players, flags, bullets, grid)
 *   - Player cards (HP, position, keys held, mouse aim)
 */

const DEBUG_URL  = "http://127.0.0.1:54323/state";
const POLL_MS    = 250;

const canvas     = document.getElementById("minimap-canvas");
const ctx        = canvas.getContext("2d");
const tickBadge  = document.getElementById("tick-badge");
const cardsEl    = document.getElementById("cards-inner");
const noGame     = document.getElementById("no-game");

// Skin colors — red for SKIN_RED, blue for SKIN_BLUE, etc.
const SKIN_COLOR = {
  SKIN_RED:    "#e03040",
  SKIN_BLUE:   "#4080f0",
  SKIN_JESTER: "#9060d0",
  SKIN_VENOM:  "#22b45a",
};

const KEY_LABEL = {
  KEY_UP: "↑", KEY_DOWN: "↓", KEY_LEFT: "←", KEY_RIGHT: "→", KEY_SHOOT: "✦",
};
const ALL_KEYS = ["KEY_UP", "KEY_DOWN", "KEY_LEFT", "KEY_RIGHT", "KEY_SHOOT"];

let gameState   = null;
let pollTimer   = null;
let worldBounds = { minX: 0, maxX: 1000, minY: 0, maxY: 1000 };

// ── Polling ───────────────────────────────────────────────────────────────────

function startPolling() {
  stopPolling();
  pollOnce();
  pollTimer = setInterval(pollOnce, POLL_MS);
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
  gameState = null;
  clearCards();
  renderEmpty();
}

async function pollOnce() {
  try {
    const res = await fetch(DEBUG_URL, { signal: AbortSignal.timeout(400) });
    if (!res.ok) return;
    const data = await res.json();
    if (data?.players?.length > 0) {
      gameState = data;
      recalcBounds(data);
      renderMinimap();
      renderCards();
    } else {
      gameState = null;
      clearCards();
      renderEmpty();
    }
  } catch (_) {
    // Server not running — silent fail, keep showing last state or empty
  }
}

// ── Canvas helpers ────────────────────────────────────────────────────────────

function resizeCanvas() {
  const headerH = canvas.parentElement.querySelector(".panel-header")?.offsetHeight ?? 32;
  const rect    = canvas.parentElement.getBoundingClientRect();
  canvas.width  = Math.max(rect.width,          10);
  canvas.height = Math.max(rect.height - headerH, 10);
}

function worldToCanvas(wx, wy) {
  const { minX, maxX, minY, maxY } = worldBounds;
  const sx = (wx - minX) / (maxX - minX);
  const sy = (wy - minY) / (maxY - minY);
  return { x: sx * canvas.width, y: sy * canvas.height };
}

function recalcBounds(data) {
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
  for (const p of data.players) {
    minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
    minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
    for (const o of (p.others ?? [])) {
      minX = Math.min(minX, o.x); maxX = Math.max(maxX, o.x);
      minY = Math.min(minY, o.y); maxY = Math.max(maxY, o.y);
    }
  }
  const pad = 100;
  worldBounds = { minX: minX - pad, maxX: maxX + pad, minY: minY - pad, maxY: maxY + pad };
}

// ── Minimap rendering ─────────────────────────────────────────────────────────

function renderEmpty() {
  resizeCanvas();
  ctx.fillStyle = "#0a0d14";
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.fillStyle = "#4a5680";
  ctx.font = "11px sans-serif";
  ctx.textAlign = "center";
  ctx.fillText("No active game", canvas.width / 2, canvas.height / 2);
  if (tickBadge) tickBadge.textContent = "no game";
}

function renderMinimap() {
  resizeCanvas();

  // Background
  ctx.fillStyle = "#0a0d14";
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  // Subtle grid
  ctx.strokeStyle = "#1e2438";
  ctx.lineWidth = 0.5;
  const step = 100;
  for (let wx = Math.ceil(worldBounds.minX / step) * step; wx < worldBounds.maxX; wx += step) {
    const { x } = worldToCanvas(wx, 0);
    ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
  }
  for (let wy = Math.ceil(worldBounds.minY / step) * step; wy < worldBounds.maxY; wy += step) {
    const { y } = worldToCanvas(0, wy);
    ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
  }

  if (tickBadge) tickBadge.textContent = `tick ${gameState.tick ?? "?"}`;

  // Collect all unique objects (same obj visible to all players, dedup by rounded pos)
  const drawn = new Set();

  for (const player of gameState.players) {
    const mk = `${Math.round(player.x)},${Math.round(player.y)}`;
    if (!drawn.has(mk)) {
      drawn.add(mk);
      drawPlayer(player, true);
    }
    for (const obj of (player.others ?? [])) {
      const ok = `${Math.round(obj.x)},${Math.round(obj.y)}`;
      if (!drawn.has(ok)) {
        drawn.add(ok);
        if      (obj.type === "PLAYER") drawOtherPlayer(obj);
        else if (obj.type === "FLAG")   drawFlag(obj.x, obj.y);
        else if (obj.type === "BULLET") drawBullet(obj.x, obj.y);
      }
    }
  }
}

function drawPlayer(p, isLocal) {
  const { x, y } = worldToCanvas(p.x, p.y);
  const r = isLocal ? 7 : 5;
  const color = SKIN_COLOR[p.skin] ?? "#dde3f0";

  // Glow
  if (isLocal) {
    ctx.shadowColor = color;
    ctx.shadowBlur  = 12;
  }

  ctx.beginPath();
  ctx.arc(x, y, r, 0, Math.PI * 2);
  ctx.fillStyle   = color;
  ctx.fill();
  ctx.strokeStyle = isLocal ? "#fff" : "#1e2438";
  ctx.lineWidth   = isLocal ? 1.5 : 1;
  ctx.stroke();
  ctx.shadowBlur  = 0;

  // HP bar
  const barW = 22, barH = 3;
  const hpFrac = Math.max(0, Math.min(1, (p.hp ?? 100) / 100));
  ctx.fillStyle = "#1e2438";
  ctx.fillRect(x - barW / 2, y - r - 7, barW, barH);
  ctx.fillStyle = hpFrac > 0.5 ? "#22b45a" : hpFrac > 0.25 ? "#d4900a" : "#e03040";
  ctx.fillRect(x - barW / 2, y - r - 7, barW * hpFrac, barH);

  // Aim line from player toward mouse (local player only)
  if (isLocal && p.mouse) {
    const aimX = p.x + (p.mouse.x - 400) * 0.3;
    const aimY = p.y + (p.mouse.y - 300) * 0.3;
    const { x: ax, y: ay } = worldToCanvas(aimX, aimY);
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.lineTo(ax, ay);
    ctx.strokeStyle = "rgba(224,48,64,0.25)";
    ctx.lineWidth = 1;
    ctx.stroke();
  }

  // Username
  if (p.username) {
    ctx.fillStyle   = "#4a5680";
    ctx.font        = "9px sans-serif";
    ctx.textAlign   = "center";
    ctx.fillText(p.username, x, y + r + 11);
  }
}

function drawOtherPlayer(obj) {
  drawPlayer({ ...obj, mouse: null }, false);
}

function drawFlag(wx, wy) {
  const { x, y } = worldToCanvas(wx, wy);
  // Pole
  ctx.fillStyle = "#4a5680";
  ctx.fillRect(x - 0.5, y - 10, 1.5, 14);
  // Flag triangle — alternates red/blue using position hash
  ctx.fillStyle = (Math.round(wx) + Math.round(wy)) % 2 === 0 ? "#e03040" : "#4080f0";
  ctx.beginPath();
  ctx.moveTo(x + 1, y - 10);
  ctx.lineTo(x + 8, y - 6);
  ctx.lineTo(x + 1, y - 2);
  ctx.closePath();
  ctx.fill();
}

function drawBullet(wx, wy) {
  const { x, y } = worldToCanvas(wx, wy);
  ctx.shadowColor = "#f07030";
  ctx.shadowBlur  = 6;
  ctx.beginPath();
  ctx.arc(x, y, 2.5, 0, Math.PI * 2);
  ctx.fillStyle = "#f09050";
  ctx.fill();
  ctx.shadowBlur = 0;
}

// ── Player cards ──────────────────────────────────────────────────────────────

function clearCards() {
  cardsEl.innerHTML = "";
  cardsEl.appendChild(noGame);
  noGame.style.display = "flex";
}

function renderCards() {
  noGame.style.display = "none";

  // Reuse existing DOM nodes to avoid flicker
  const existing = new Map(
    [...cardsEl.querySelectorAll(".player-card")].map((el) => [el.dataset.uuid, el])
  );
  const seen = new Set();

  for (const p of gameState.players) {
    seen.add(p.uuid);
    let card = existing.get(p.uuid);
    if (!card) {
      card = document.createElement("div");
      card.className  = "player-card";
      card.dataset.uuid = p.uuid;
      cardsEl.appendChild(card);
    }
    updateCard(card, p);
  }

  // Remove cards for players who left
  for (const [uuid, el] of existing) {
    if (!seen.has(uuid)) el.remove();
  }
}

function updateCard(card, p) {
  const hp        = Math.max(0, Math.min(100, p.hp ?? 100));
  const hpColor   = hp > 50 ? "#22b45a" : hp > 25 ? "#d4900a" : "#e03040";
  const skinColor = SKIN_COLOR[p.skin] ?? "#dde3f0";
  const heldKeys  = new Set(p.keys ?? []);
  const name      = esc(p.username || p.uuid.slice(0, 8));

  card.innerHTML = `
    <div class="card-name" style="color:${skinColor}">${name}</div>
    <div class="card-row">
      <span class="card-label">HP</span>
      <div class="hp-bar-track">
        <div class="hp-bar-fill" style="width:${hp}%;background:${hpColor}"></div>
      </div>
      <span class="card-val">${Math.round(hp)}</span>
    </div>
    <div class="card-row">
      <span class="card-label">XY</span>
      <span class="card-val">${Math.round(p.x)}, ${Math.round(p.y)}</span>
      <span class="card-label" style="margin-left:6px">🚩</span>
      <span class="card-val">${p.flagCount ?? 0}</span>
    </div>
    <div class="card-row">
      <span class="card-label">🖱</span>
      <span class="card-val">${p.mouse?.x ?? 0}, ${p.mouse?.y ?? 0}</span>
    </div>
    <div class="card-row">
      <div class="keys-wrap">
        ${ALL_KEYS.map((k) => `<span class="key-chip ${heldKeys.has(k) ? "active" : ""}">${KEY_LABEL[k]}</span>`).join("")}
      </div>
    </div>
    <div class="card-row" style="font-size:9px;color:var(--muted)">
      ${esc(p.skin?.replace("SKIN_", "") ?? "?")} · ${esc(p.animation?.replace("ANIM_", "") ?? "?")}
    </div>
  `;
}

function esc(s) {
  return String(s)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}
