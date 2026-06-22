import fs from "node:fs";

const outputPath = "D:/RAG-Java-MultiAgent/output/playwright/smartcloud-web-home.png";
const pageUrl = "http://127.0.0.1:5173/";
const cdpBase = "http://127.0.0.1:9223";

async function json(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}: ${await response.text()}`);
  }
  return response.json();
}

let target;
try {
  target = await json(`${cdpBase}/json/new?${pageUrl}`, { method: "PUT" });
} catch {
  const tabs = await json(`${cdpBase}/json/list`);
  target = tabs.find((tab) => tab.webSocketDebuggerUrl) || tabs[0];
}

if (!target?.webSocketDebuggerUrl) {
  throw new Error("No Chrome DevTools Protocol websocket target found.");
}

const ws = new WebSocket(target.webSocketDebuggerUrl);
let nextId = 1;
const pending = new Map();

ws.addEventListener("message", (event) => {
  const message = JSON.parse(event.data);
  if (!message.id || !pending.has(message.id)) return;
  const { resolve, reject } = pending.get(message.id);
  pending.delete(message.id);
  if (message.error) {
    reject(new Error(JSON.stringify(message.error)));
  } else {
    resolve(message.result);
  }
});

await new Promise((resolve, reject) => {
  ws.addEventListener("open", resolve, { once: true });
  ws.addEventListener("error", reject, { once: true });
});

function send(method, params = {}) {
  const id = nextId++;
  ws.send(JSON.stringify({ id, method, params }));
  return new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
  });
}

await send("Page.enable");
await send("Runtime.enable");
await send("Emulation.setDeviceMetricsOverride", {
  width: 1440,
  height: 900,
  deviceScaleFactor: 1,
  mobile: false,
});
await send("Page.navigate", { url: pageUrl });
await new Promise((resolve) => setTimeout(resolve, 7000));
await send("Runtime.evaluate", {
  expression: "document.fonts && document.fonts.ready ? document.fonts.ready.then(() => true) : true",
  awaitPromise: true,
});

const screenshot = await send("Page.captureScreenshot", {
  format: "png",
  fromSurface: true,
});

fs.writeFileSync(outputPath, Buffer.from(screenshot.data, "base64"));
ws.close();
console.log(outputPath);
