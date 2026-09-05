/**
 * Asks Google Play whether a purchase token is good, using a service account.
 *
 * Subscriptions go through purchases.subscriptionsv2 (token alone identifies them); one-time
 * products need the product id too. The service-account access token is cached in KV for
 * fifty minutes; Google issues them for sixty.
 */

const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/androidpublisher";
const API = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications";

// A cancelled subscription is still paid for until it expires, so it stays Pro; grace period
// is Play still trying the card. On hold, paused and expired are the ones that lose access.
const LIVE_STATES = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_CANCELED",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
]);

/** Returns `{ plan, until }` for a live purchase, or null. */
export async function verifyWithPlay({ token, product, env, doFetch }) {
  const access = await accessToken(env, doFetch);
  const pkg = env.PACKAGE_NAME;
  const auth = { authorization: `Bearer ${access}` };

  if (product === "peggy_pro_lifetime") {
    const r = await doFetch(`${API}/${pkg}/purchases/products/${product}/tokens/${encodeURIComponent(token)}`, { headers: auth });
    if (!r.ok) return null;
    const p = await r.json();
    if (p.purchaseState !== 0) return null; // 0 purchased, 1 cancelled (refunded), 2 pending
    return { plan: "lifetime", until: Date.now() + 365 * 86_400_000 };
  }

  const r = await doFetch(`${API}/${pkg}/purchases/subscriptionsv2/tokens/${encodeURIComponent(token)}`, { headers: auth });
  if (!r.ok) return null;
  const s = await r.json();
  if (!LIVE_STATES.has(s.subscriptionState)) return null;
  const line = (s.lineItems ?? []).find((l) => l.productId === product) ?? s.lineItems?.[0];
  if (!line) return null;
  const until = Date.parse(line.expiryTime);
  if (!(until > Date.now())) return null;
  return { plan: line.productId.replace(/^peggy_pro_/, ""), until };
}

async function accessToken(env, doFetch) {
  const cached = await env.KV.get("gauth");
  if (cached) return cached;
  const now = Math.floor(Date.now() / 1000);
  const jwt = await signJwt(
    { iss: env.PLAY_SA_EMAIL, scope: SCOPE, aud: TOKEN_URL, iat: now, exp: now + 3600 },
    env.PLAY_SA_KEY,
  );
  const r = await doFetch(TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: jwt }),
  });
  if (!r.ok) throw new Error(`google token: ${r.status} ${await r.text()}`);
  const { access_token } = await r.json();
  await env.KV.put("gauth", access_token, { expirationTtl: 50 * 60 });
  return access_token;
}

/** RS256 over a PKCS#8 PEM, which is what a service-account JSON's private_key is. */
async function signJwt(claims, pem) {
  const der = Uint8Array.from(atob(pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "")), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const head = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const body = b64url(JSON.stringify(claims));
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${head}.${body}`));
  return `${head}.${body}.${b64url(sig)}`;
}

function b64url(input) {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : new Uint8Array(input);
  let s = "";
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
