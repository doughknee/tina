/**
 * The Peggy relay: hosted AI for Pro users, with no key on the phone.
 *
 * It speaks the Anthropic Messages API, so the app points its Anthropic client here and sends
 * its Play purchase token where the API key would go. The relay checks the token with Play
 * (cached), counts it against a monthly quota, pins the model per route, and forwards the
 * request to Anthropic with the real key. Responses stream straight through.
 *
 *   POST /v1/messages        headers: x-api-key (purchase token), x-peggy-product, x-peggy-route
 *   GET  /v1/entitlement     same headers; what the app shows in its quota meter
 *   POST /rtdn?secret=...    Play's Pub/Sub push; forgets the cached entitlement for that token
 *
 * See relay/README.md for deploying and the secrets it needs.
 */
import { verifyWithPlay } from "./play.js";

const ANTHROPIC = "https://api.anthropic.com/v1/messages";
const MAX_BODY = 512 * 1024;
// Which model each route gets, whatever the client asked for. Ask deserves the better model;
// parse and improve are structured extraction where the fast one is as good and ten times cheaper.
const MODELS = { ask: "claude-sonnet-5", parse: "claude-haiku-4-5", improve: "claude-haiku-4-5" };
// Monthly turns. Ask is the expensive one, so it is counted on its own; parse and improve share
// a bucket. Lifetime buyers get a taste of Ask; unlimited is what the subscription is for.
const QUOTA = {
  ask: { monthly: 400, yearly: 400, lifetime: 50 },
  light: { monthly: 3000, yearly: 3000, lifetime: 3000 },
};
const ENT_TTL_S = 6 * 3600;

export default {
  fetch: (request, env) => handle(request, env, fetch),
};

/** `doFetch` is injectable so the self-check can run without Play or Anthropic. */
export async function handle(request, env, doFetch) {
  const url = new URL(request.url);
  try {
    if (url.pathname === "/rtdn" && request.method === "POST") return await rtdn(request, env, url);
    if (url.pathname === "/v1/entitlement" && request.method === "GET") return await entitlementInfo(request, env, doFetch);
    if (url.pathname === "/v1/messages" && request.method === "POST") return await messages(request, env, doFetch);
    return json(404, { error: { type: "not_found" } });
  } catch (e) {
    if (e instanceof Reply) return e.response;
    console.error(e);
    return json(502, { error: { type: "relay_error", message: String(e?.message ?? e) } });
  }
}

class Reply extends Error {
  constructor(status, body, headers) {
    super(body?.error?.type ?? "reply");
    this.response = json(status, body, headers);
  }
}

function json(status, body, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json", ...headers },
  });
}

// --------------------------------------------------------------------------------------
// entitlement

/** Resolves who is calling: the plan and when it lapses. Cached in KV, keyed by the token's hash. */
async function entitle(request, env, doFetch) {
  const token = request.headers.get("x-api-key")?.trim();
  const product = request.headers.get("x-peggy-product")?.trim();
  if (!token || !product) throw new Reply(401, { error: { type: "authentication_error", message: "purchase token and product required" } });

  const key = await sha256(token);
  const cached = await env.KV.get(`ent:${key}`, "json");
  if (cached && cached.until > Date.now()) return { ...cached, key };

  let ent;
  if (env.DEV_TOKEN && token === env.DEV_TOKEN) {
    ent = { plan: planOf(product), until: Date.now() + 86_400_000 };
  } else {
    ent = await verifyWithPlay({ token, product, env, doFetch });
  }
  if (!ent) throw new Reply(403, { error: { type: "permission_error", message: "no active Peggy Pro purchase" } });

  const ttl = Math.min(ENT_TTL_S, Math.max(60, Math.floor((ent.until - Date.now()) / 1000)));
  await env.KV.put(`ent:${key}`, JSON.stringify(ent), { expirationTtl: ttl });
  return { ...ent, key };
}

export function planOf(productId) {
  return productId.replace(/^peggy_pro_/, "");
}

function bucketOf(route) {
  return route === "ask" ? "ask" : "light";
}

function month() {
  return new Date().toISOString().slice(0, 7);
}

async function quotaState(env, ent, bucket) {
  const used = Number((await env.KV.get(`q:${ent.key}:${bucket}:${month()}`)) ?? 0);
  const limit = QUOTA[bucket][ent.plan] ?? 0;
  return { used, limit };
}

// --------------------------------------------------------------------------------------
// routes

async function messages(request, env, doFetch) {
  const ent = await entitle(request, env, doFetch);
  const route = request.headers.get("x-peggy-route")?.trim() || "ask";
  if (!MODELS[route]) throw new Reply(400, { error: { type: "invalid_request_error", message: "unknown route" } });

  const raw = await request.text();
  if (raw.length > MAX_BODY) throw new Reply(413, { error: { type: "invalid_request_error", message: "request too large" } });

  const bucket = bucketOf(route);
  const quota = await quotaState(env, ent, bucket);
  if (quota.used >= quota.limit) {
    throw new Reply(429, { error: { type: "quota_exceeded", message: `monthly ${bucket} quota reached`, ...quota } },
      { "x-peggy-quota-used": String(quota.used), "x-peggy-quota-limit": String(quota.limit) });
  }
  // ponytail: read-then-write, not atomic; two racing requests can both pass the check.
  // Fine for one phone. A Durable Object counter if that ever matters.
  const ttl = 40 * 86_400;
  await env.KV.put(`q:${ent.key}:${bucket}:${month()}`, String(quota.used + 1), { expirationTtl: ttl });

  const body = JSON.parse(raw);
  body.model = MODELS[route];
  const upstream = await doFetch(ANTHROPIC, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-api-key": env.ANTHROPIC_API_KEY,
      "anthropic-version": request.headers.get("anthropic-version") || "2023-06-01",
    },
    body: JSON.stringify(body),
  });
  const headers = new Headers(upstream.headers);
  headers.set("x-peggy-quota-used", String(quota.used + 1));
  headers.set("x-peggy-quota-limit", String(quota.limit));
  headers.delete("content-length"); // the body is streamed through untouched; keep it that way
  return new Response(upstream.body, { status: upstream.status, headers });
}

async function entitlementInfo(request, env, doFetch) {
  const ent = await entitle(request, env, doFetch);
  return json(200, {
    plan: ent.plan,
    until: ent.until,
    quota: { ask: await quotaState(env, ent, "ask"), light: await quotaState(env, ent, "light") },
  });
}

/**
 * Play's real-time developer notification, pushed by Pub/Sub. Whatever happened to the
 * purchase, the cached answer is stale, so drop it; the next request asks Play again.
 */
async function rtdn(request, env, url) {
  if (!env.RTDN_SECRET || url.searchParams.get("secret") !== env.RTDN_SECRET) return json(403, { error: { type: "forbidden" } });
  const envelope = await request.json();
  const data = JSON.parse(atob(envelope?.message?.data ?? "e30="));
  const token = data.subscriptionNotification?.purchaseToken
    ?? data.oneTimeProductNotification?.purchaseToken
    ?? data.voidedPurchaseNotification?.purchaseToken;
  if (token) await env.KV.delete(`ent:${await sha256(token)}`);
  return new Response(null, { status: 204 });
}

async function sha256(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
