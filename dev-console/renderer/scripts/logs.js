/**
 * logs.js
 * Manages the three log panels: appending colorized output, line counts,
 * auto-scroll, and panel header timestamp badges.
 */


const logEls = {
  "server-log":  document.getElementById("log-server"),
  "client1-log": document.getElementById("log-c1"),
  "client2-log": document.getElementById("log-c2"),
};

const lineCountEls = {
  "server-log":  document.getElementById("server-lines"),
  "client1-log": document.getElementById("c1-lines"),
  "client2-log": document.getElementById("c2-lines"),
};

const stampEls = {
  server:  document.getElementById("server-stamp"),
  client1: document.getElementById("client1-stamp"),
  client2: document.getElementById("client2-stamp"),
};

const lineCounts = { "server-log": 0, "client1-log": 0, "client2-log": 0 };

function appendLog(channel, text) {
  const el = logEls[channel];
  if (!el) return;

  // Only auto-scroll if already near the bottom
  const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 80;

  const span = document.createElement("span");
  span.innerHTML = colorize(text);
  el.appendChild(span);

  const newLines = (text.match(/\n/g) || []).length;
  lineCounts[channel] += newLines;
  if (lineCountEls[channel]) {
    lineCountEls[channel].textContent = lineCounts[channel] + " lines";
  }

  if (atBottom) el.scrollTop = el.scrollHeight;
}

function clearLogs() {
  for (const [ch, el] of Object.entries(logEls)) {
    if (el) el.innerHTML = "";
    lineCounts[ch] = 0;
  }
  for (const el of Object.values(lineCountEls)) {
    if (el) el.textContent = "0 lines";
  }
}

function setPanelStamp(panel, stamp) {
  const el = stampEls[panel];
  if (!el) return;
  if (stamp) {
    el.textContent = stamp;
    el.style.display = "";
  } else {
    el.textContent = "";
    el.style.display = "none";
  }
}
