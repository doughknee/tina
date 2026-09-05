# Peggy relay

Hosted AI for Pro users. A Cloudflare Worker that verifies the phone's Play purchase, counts
it against a monthly quota, pins the model per route, and forwards the request to Anthropic
with a key that never leaves the server. `docs/MONETIZATION.md` §4 is the why; this is the how.

```
phone ──x-api-key: <purchase token>──▶ relay ──x-api-key: <real key>──▶ api.anthropic.com
                                        │
                                        ├── Play Developer API   (is this token paid for?)
                                        └── KV                   (cached answers, quotas)
```

The wire format is the Anthropic Messages API, so the app's existing Anthropic client is the
relay client too. Three headers on every request:

| Header | Value |
|---|---|
| `x-api-key` | the Play purchase token |
| `x-peggy-product` | `peggy_pro_monthly`, `peggy_pro_yearly` or `peggy_pro_lifetime` |
| `x-peggy-route` | `ask`, `parse` or `improve` — picks the model and the quota bucket |

Responses carry `x-peggy-quota-used` and `x-peggy-quota-limit`. Over quota is a 429 with
`error.type = "quota_exceeded"`. No purchase is a 403. `GET /v1/entitlement` returns the
plan, expiry and both quota buckets for the meter in Settings.

## Check it

```bash
node relay/test.mjs
```

Fakes KV, Play and Anthropic; covers auth, model pinning, both quota buckets, the lifetime
cap, Play caching, and revocation through `/rtdn`.

## Run it locally

```bash
cd relay && npx wrangler dev
```

With `DEV_TOKEN` set in `.dev.vars`, any request whose `x-api-key` equals it is treated as
Pro without asking Play. `ANTHROPIC_API_KEY` in the same file makes real calls.

## Deploy it

One-time, in this order:

1. **Cloudflare**: `npx wrangler login`, then `npx wrangler kv namespace create KV` and paste
   the id into `wrangler.toml`.
2. **Google Cloud**: a project with the *Google Play Android Developer API* enabled and a
   service account. In Play Console → Users and permissions, invite the service-account
   email with **View financial data** on the Peggy app. Download its JSON key.
3. **Secrets**: `npx wrangler secret put` for `ANTHROPIC_API_KEY`, `PLAY_SA_EMAIL`,
   `PLAY_SA_KEY` (the `private_key` field pasted as-is; escaped `\n` and quotes are fine), `RTDN_SECRET`
   (any long random string).
4. `npx wrangler deploy`. `wrangler.toml` routes it at `relay.doughknee.com`, a custom domain on the
   site's own zone, so deploy creates the DNS record and certificate; no `workers.dev` needed.
5. **Revocation**: Play Console → Monetization setup → Real-time developer notifications.
   Create a Pub/Sub topic in the same Google Cloud project, add a **push** subscription with
   endpoint `https://<relay>/rtdn?secret=<RTDN_SECRET>`, and grant
   `google-play-developer-notifications@system.gserviceaccount.com` Publisher on the topic.
   Until this is wired, a refund or cancellation is honoured within six hours (the cache TTL)
   instead of immediately.

`HOSTED_RELAY_URL` in the app already points there.

## What it deliberately does not do

- **Streaming is a pass-through**, not a feature of the relay. When the app asks for
  `stream: true` it gets SSE back untouched.
- **Quota counting is not atomic** (KV read-then-write). Two requests racing from the same
  phone can both pass. That is one person's phone; a Durable Object counter is the upgrade if
  it ever matters.
- **No per-user identity beyond the token.** A subscription's token changes on renewal, which
  resets its quota month early; the ceiling is generous enough that this is a rounding error.
