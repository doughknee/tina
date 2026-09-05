// The relay's self-check: `node relay/test.mjs`. Fakes KV, Play and Anthropic.
import assert from "node:assert/strict";
import { generateKeyPairSync } from "node:crypto";
import { handle } from "./src/index.js";

// a throwaway service-account key, so the JWT signer runs for real
const { privateKey } = generateKeyPairSync("rsa", { modulusLength: 2048 });
const PEM = privateKey.export({ type: "pkcs8", format: "pem" });

const kv = new Map();
const env = {
  KV: {
    get: async (k, type) => { const v = kv.get(k); return v == null ? null : type === "json" ? JSON.parse(v) : v; },
    put: async (k, v) => { kv.set(k, v); },
    delete: async (k) => { kv.delete(k); },
  },
  PACKAGE_NAME: "com.peggy.app",
  ANTHROPIC_API_KEY: "sk-relay",
  PLAY_SA_EMAIL: "sa@example.iam.gserviceaccount.com",
  // as it arrives when the JSON value is pasted into `wrangler secret put`: quoted, newlines escaped
  PLAY_SA_KEY: JSON.stringify(PEM),
  RTDN_SECRET: "s3cret",
  DEV_TOKEN: "dev-token",
};

let upstream = [];
let playState = "SUBSCRIPTION_STATE_ACTIVE";
const fakeFetch = async (url, init) => {
  if (url.startsWith("https://api.anthropic.com")) {
    upstream.push({ headers: init.headers, body: JSON.parse(init.body) });
    return new Response('{"content":[{"type":"text","text":"hi"}]}', { status: 200, headers: { "content-type": "application/json" } });
  }
  if (url.includes("subscriptionsv2")) {
    return Response.json({ subscriptionState: playState, lineItems: [{ productId: "peggy_pro_yearly", expiryTime: new Date(Date.now() + 3600e3).toISOString() }] });
  }
  if (url.includes("/purchases/products/")) return Response.json({ purchaseState: 0 });
  if (url.includes("oauth2")) return Response.json({ access_token: "ya29.fake" });
  throw new Error(`unexpected fetch ${url}`);
};

const req = (path, { token = "dev-token", product = "peggy_pro_yearly", route = "ask", method = "POST", body = { model: "whatever", messages: [] } } = {}) =>
  new Request(`https://relay.test${path}`, {
    method,
    headers: { "x-api-key": token, "x-peggy-product": product, "x-peggy-route": route, "anthropic-version": "2023-06-01" },
    body: method === "POST" ? JSON.stringify(body) : undefined,
  });

// no token -> 401
assert.equal((await handle(new Request("https://relay.test/v1/messages", { method: "POST", body: "{}" }), env, fakeFetch)).status, 401);

// ask: forwarded with the relay's key and the pinned model, client's model ignored
let r = await handle(req("/v1/messages"), env, fakeFetch);
assert.equal(r.status, 200);
assert.equal(upstream[0].headers["x-api-key"], "sk-relay");
assert.equal(upstream[0].body.model, "claude-sonnet-5");
assert.equal(r.headers.get("x-peggy-quota-used"), "1");
assert.equal(r.headers.get("x-peggy-quota-limit"), "400");

// parse gets the fast model and its own bucket
r = await handle(req("/v1/messages", { route: "parse" }), env, fakeFetch);
assert.equal(upstream[1].body.model, "claude-haiku-4-5");
assert.equal(r.headers.get("x-peggy-quota-limit"), "3000");

// entitlement meter reflects both
const info = await (await handle(req("/v1/entitlement", { method: "GET" }), env, fakeFetch)).json();
assert.deepEqual(info.quota, { ask: { used: 1, limit: 400 }, light: { used: 1, limit: 3000 } });
assert.equal(info.plan, "yearly");

// lifetime: 50 Ask turns, then 429
env.DEV_TOKEN = undefined;
kv.clear();
for (let i = 0; i < 50; i++) assert.equal((await handle(req("/v1/messages", { token: "life", product: "peggy_pro_lifetime" }), env, fakeFetch)).status, 200);
r = await handle(req("/v1/messages", { token: "life", product: "peggy_pro_lifetime" }), env, fakeFetch);
assert.equal(r.status, 429);
assert.equal((await r.json()).error.type, "quota_exceeded");

// a real subscription is verified with Play once, then served from cache
const playCalls = () => [...kv.keys()].filter((k) => k.startsWith("ent:")).length;
assert.equal((await handle(req("/v1/messages", { token: "real-sub" }), env, fakeFetch)).status, 200);
assert.equal(playCalls(), 2);

// rtdn drops the cache; an expired subscription is then refused
playState = "SUBSCRIPTION_STATE_EXPIRED";
assert.equal((await handle(req("/v1/messages", { token: "real-sub" }), env, fakeFetch)).status, 200, "still cached");
const push = { message: { data: btoa(JSON.stringify({ subscriptionNotification: { purchaseToken: "real-sub" } })) } };
assert.equal((await handle(new Request("https://relay.test/rtdn?secret=s3cret", { method: "POST", body: JSON.stringify(push) }), env, fakeFetch)).status, 204);
assert.equal((await handle(req("/v1/messages", { token: "real-sub" }), env, fakeFetch)).status, 403);
assert.equal((await handle(new Request("https://relay.test/rtdn?secret=wrong", { method: "POST", body: "{}" }), env, fakeFetch)).status, 403);

// oversized body
assert.equal((await handle(req("/v1/messages", { token: "life", product: "peggy_pro_lifetime", route: "parse", body: { messages: "x".repeat(600 * 1024) } }), env, fakeFetch)).status, 413);

console.log("relay self-check ok");
