import express from "express";
import crypto from "node:crypto";

const app = express();
app.use(express.json({ limit: "16kb" }));
const tokens = new Map();
const requests = new Map();
const PORT = Number(process.env.PORT || 3000);
const ADMIN_EMAIL = process.env.ADMIN_EMAIL || "blackspammerbd@gmail.com";

function cleanExpired() {
  const now = Date.now();
  for (const [key, value] of tokens) if (value.expiresAt < now || value.used) tokens.delete(key);
  for (const [key, value] of requests) if (value.createdAt + 10 * 60_000 < now) requests.delete(key);
}
setInterval(cleanExpired, 30_000).unref();

app.get("/health", (_req, res) => res.json({ ok: true, service: "BSB-X-HUB", admin: ADMIN_EMAIL }));
app.post("/api/tokens", (req, res) => {
  const allowed = ["File transfer", "Screen sharing (system consent required)", "Microphone (runtime consent required)", "Location (runtime consent required)"];
  const selected = String(req.body?.permissions || "").split(",").filter(x => allowed.includes(x));
  if (!selected.length) return res.status(400).json({ error: "Select at least one supported permission." });
  const token = crypto.randomBytes(16).toString("hex").toUpperCase();
  const hash = crypto.createHash("sha256").update(token).digest("hex");
  tokens.set(hash, { permissions: selected, expiresAt: Date.now() + 5 * 60_000, used: false });
  res.status(201).json({ token, expiresInSeconds: 300, singleUse: true });
});
app.post("/api/pair", (req, res) => {
  const token = String(req.body?.token || "").trim().toUpperCase();
  if (!/^[A-F0-9]{32}$/.test(token)) return res.status(400).json({ error: "Invalid token format." });
  const hash = crypto.createHash("sha256").update(token).digest("hex");
  const record = tokens.get(hash);
  if (!record || record.used || record.expiresAt < Date.now()) return res.status(410).json({ error: "Token invalid, expired, or already used." });
  record.used = true;
  const requestId = crypto.randomBytes(16).toString("hex");
  requests.set(requestId, { createdAt: Date.now(), permissions: record.permissions, state: "AWAITING_OWNER_APPROVAL" });
  res.status(202).json({ requestId, state: "AWAITING_OWNER_APPROVAL", permissions: record.permissions });
});
app.listen(PORT, "0.0.0.0", () => console.log(`BSB-X-HUB backend listening on ${PORT}`));
