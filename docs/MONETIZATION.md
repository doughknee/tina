# tina — monetization plan

Written 2026-09-02. Read `ROADMAP.md` first; this document decides what is sold, at what price, why, and how the code and the store are wired for it. Numbers are estimates to be replaced with Play Console data after the first month.

## 1. The shape of the business

tina is a local-first utility with no accounts and no servers. That is its main selling point and its main constraint: there is nothing to charge for "in the cloud" unless we build one thing that costs money to run. The plan builds exactly one: a hosted AI relay. Everything else that is paid is either cosmetic, convenience, or a power feature that costs nothing per user to operate.

Three rules:

1. **The free app is complete.** Capture, parse, Plan/Sort/Ideas, reminders, widgets, tiles, backup and export, and bring-your-own-key AI stay free forever. A free user must never hit a wall while doing the thing the app is for. Free is the funnel, the reviews, and the word of mouth.
2. **Pro is what costs us money or what a power user wants.** Hosted AI (costs per call), sync (costs engineering and support), and the "make it mine" layer (themes, icons, widget packs).
3. **No ads, no data sale, no dark patterns.** The privacy story is the brand. A single paywall screen that shows what changes, one "maybe later" tap, no repeated nags, no fake urgency.

## 2. Products

| SKU | Type | Price (USD, Play localizes) | What it unlocks |
|---|---|---|---|
| `tina_pro_monthly` | Subscription, 7-day free trial | $3.99 / month | Everything below |
| `tina_pro_yearly` | Subscription, 7-day free trial | $29.99 / year (37% off) | Everything below |
| `tina_pro_lifetime` | One-time in-app product | $49.99 | Everything below **except hosted AI**, which is capped at a small lifetime quota (see §4) |

**Pro includes**
- **Hosted Ask, parse, and improve.** No API key. Streams. Best model for Ask, fast model for parse and improve. A visible monthly quota (default: 400 Ask turns, unlimited captures with AI refinement up to 3,000/month).
- **Sync** between the user's own devices when it ships (v2.x): encrypted, via the user's Google Drive app folder or WebDAV. Until then, the yearly price is justified by AI alone; sync is announced, not sold.
- **Make it yours**: five extra app icons, custom seed colour and named themes, larger widget pack (week strip widget, single-task widget, Idea capture 2×1).
- **Power**: unlimited Ask history (free keeps 30 days), per-occurrence statistics on habits ("12 of 14 done this month"), export to ICS, encrypted backups to a folder of your choice.
- **Supporter**: a badge in About and a place in the credits. People pay for this more than we think.

Why a subscription *and* a lifetime: the AI relay has real monthly cost, so unlimited AI must be a subscription; a meaningful share of this app's audience (local-first, privacy-minded) refuses subscriptions on principle, and the lifetime SKU converts them without exposing us to unbounded AI cost. The lifetime SKU is "Pro without ongoing AI" with a taste of AI included.

Why these prices: $3.99/month is under the "coffee" threshold used by Todoist ($4-5), TickTick ($2.99 but yearly-only), and Things (one-time $10 per platform). Yearly at $29.99 pushes the annual choice (better retention, cash up front). Lifetime at $49.99 is ~16 months of monthly, the common anchor. Play takes 15% on the first $1M/year once enrolled in the reduced-fee program; enrol before launch.

## 3. What stays free, explicitly

Capture with the local parser · voice capture · Plan (Day/Week/Month/All) · Sort with swipes · Ideas with the rich editor · reminders and digests · home widgets (Today, Capture) and both tiles · share sheet · search · tags · trash with retention · JSON backup/export/import and auto-backup · app lock · Material You · BYOK AI (Ollama, Anthropic, OpenAI, custom) including Ask with write actions · desktop app.

BYOK stays free on purpose. It costs us nothing, it is the privacy-preserving option, and Ollama users are the app's most vocal advocates. Pro is the *convenient* AI, not the *only* AI.

## 4. Hosted AI economics

From the AI audit (`docs/audits/ai.md`), a typical user is ~0.33 MTok in / 0.03 MTok out per month if the context is rebuilt every turn and Opus is used for everything. Two changes fix the economics before the relay exists:

- Route capture-parse and improve to `claude-haiku-4-5` (structured extraction; Opus is wasted there).
- Build the Ask context once per conversation and cache it (prompt caching), cap it by tokens at the relay, and set explicit `effort`.

Resulting COGS per active Pro user per month: **~$0.80-1.20** with Sonnet 5 for Ask, ~$0.50 if Haiku is enough. Against $3.99 (net ~$3.39 after Play's 15%), gross margin is ~65-75%. The yearly SKU nets ~$2.12/month equivalent, still fine.

Quotas protect the tail: 400 Ask turns/month is ~10× a typical user and caps the worst case at ~$6 of cost. Over-quota users see a friendly meter and can wait or use their own key (BYOK stays available to Pro users too). Lifetime users get 50 Ask turns/month of hosted AI as a taste; unlimited requires the subscription.

The relay: a stateless service (Cloudflare Workers or Fly.io, ~150 lines) that (1) verifies a Play purchase token against the Play Developer API and issues a short-lived bearer token, (2) proxies `/ask`, `/parse`, `/improve` to the model provider with server-side keys and pinned models, (3) enforces per-account quotas in a KV store, (4) receives Play Real-time Developer Notifications to revoke on cancel/refund. Cost at 10k Pro users: under $50/month plus model spend.

## 5. Funnel and projections

Assumptions (conservative for a well-reviewed utility with no marketing budget):

| Metric | Value |
|---|---|
| Installs per month, organic | 1,000 → 5,000 over the first year |
| 30-day retention | 25% |
| Free → trial start | 3% of installs |
| Trial → paid | 40% |
| Monthly : yearly : lifetime split | 40 : 45 : 15 |
| Monthly churn (subscriptions) | 6% |

| Month | Cumulative installs | Paying users | Net revenue / month |
|---|---|---|---|
| 3 | 3,000 | ~40 | ~$110 |
| 6 | 9,000 | ~120 | ~$330 |
| 12 | 30,000 | ~400 | ~$1,100 |
| 24 | 90,000 | ~1,200 | ~$3,300 |

These are floor numbers. Reviews and a single feature in a newsletter or subreddit typically double them. The point is that costs stay near zero until revenue exists: the relay and model spend scale with paying users only.

## 6. Play Billing implementation

Engineering scope for v1.9 (roadmap), all Android; desktop honours the same entitlement via a signed token exported from the phone (later).

1. **Dependency**: `com.android.billingclient:billing-ktx:7.1.1`.
2. **`ProEntitlement`** (commonMain): `StateFlow<Entitlement>` with `Free`, `Trial(until)`, `Pro(plan, until?)`. Backed by a local cache (DataStore, 7-day lease) so the app works offline; refreshed from `BillingClient.queryPurchasesAsync` on launch and after purchase.
3. **`BillingRepository`** (androidMain): connect, query products, launch the flow, acknowledge, handle `PENDING`, restore. Server-side verification through the relay's `/entitlement` endpoint; until the relay exists, client-side acknowledgement is enough for the testing tracks.
4. **Developer override**: a build-time flag (`BuildConfig.PRO_OVERRIDE` set from a gitignored property) so the maintainer's device is Pro. Never a runtime toggle.
5. **Gating points**: a `Gate.pro { … }` composable that shows the paywall sheet instead of the feature: hosted provider option in Parsing & AI, themes/icons, extra widgets, Ask history beyond 30 days, ICS export, encrypted folder backups.
6. **Paywall**: one sheet, three prices, the trial called out, "Restore purchases", "Not now". Shown at most once per week unprompted; always available from Settings → tina Pro.
7. **Lifecycle**: grace period (Play default 3 days) keeps Pro; account hold shows a one-line banner in Settings; cancellation keeps Pro until period end; refunds revoke on the next launch.
8. **Testing**: license testers on the internal track buy for free; test cards; verify purchase, restore after reinstall, cancel, and expiry with Play's test subscriptions (5-minute periods).

## 7. Where the paywall appears, and where it never does

Appears: Settings → tina Pro (always); Parsing & AI when choosing "tina (no key needed)"; Appearance when tapping a locked theme or icon; the widget picker for Pro widgets; Ask history past 30 days.

Never: during capture, on first launch, on the empty states, in notifications, or as an interstitial after any action. The trial is offered exactly once, inside the paywall, not as a pop-up.

## 8. Risks and answers

- **Play policy on exact alarms and AI**: covered in `PLAY-LAUNCH.md`. The AI relay needs the Gen-AI content-report mechanism (a "Report" action on any Ask reply).
- **Model price changes**: quotas and the ability to switch the Ask model server-side without an app update.
- **Refund abuse of lifetime**: Play's standard window; nothing to do.
- **Subscription fatigue**: the lifetime SKU and free BYOK are the answers, not more nagging.
- **Support load**: a "Send diagnostics" action (exists, disabled) and a public issue tracker; Pro support is email with a 48-hour target.
- **Taxes and payouts**: Play handles VAT/GST as merchant of record in most countries; set up the payments profile before the first sale.

## 9. Decisions still open for the owner

- Confirm prices and whether to launch the lifetime SKU at day one or after 3 months of data.
- Choose the relay host (Cloudflare Workers is cheapest; Fly.io is simplest to debug).
- Decide if sync is promised in the Pro copy at launch (recommendation: no; "coming to Pro" in the roadmap only).
