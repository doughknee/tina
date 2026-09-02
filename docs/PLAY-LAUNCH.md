# Peggy — Google Play launch checklist

Everything needed to go from a sideloaded APK to a production listing. Items marked **(owner)** need the Play Console account holder; everything else is engineering and is either done or scheduled in `ROADMAP.md`.

## 1. Account and policy (owner)

- [ ] Developer profile verified, payments profile created, tax info submitted.
- [ ] Enrol in the 15% service-fee tier (Play Console → Monetization setup) before the first sale.
- [ ] Create the app: name **Peggy**, default language en-US, app (not game), free with in-app purchases.
- [ ] Declarations: Ads = none; News app = no; COVID = no; Government app = no; Financial features = none; Health = none.
- [ ] Target audience: 13+ (not designed for children). Content rating questionnaire → Everyone.
- [ ] Data Safety form: use section 4 below verbatim.
- [ ] Privacy policy URL: host `docs/PRIVACY.md` at a public URL (GitHub Pages works) and paste it.
- [ ] Permission declaration for `USE_EXACT_ALARM`: use section 5 below.
- [ ] Play App Signing: accept Google-managed signing on first upload. Rotate the local keystore password first (it is in the README history) and keep the upload key outside the repo.
- [ ] Set up Real-time Developer Notifications (Pub/Sub topic) when subscriptions ship in v1.9.

## 2. Testing tracks and timeline

New personal developer accounts must run a **closed test with at least 12 testers for 14 continuous days** before production access is granted. Start it early.

| Week | Track | What |
|---|---|---|
| 0 | Internal (up to 100) | v1.7 AAB; you and a few friends; verify install, icon, reminders, backup |
| 1-3 | Closed (12+ testers) | v1.7 → v1.8 builds; collect the 14 days; fix crashes from vitals |
| 4 | Apply for production access | Answer the questionnaire about the closed test |
| 5-6 | Open testing (optional) | v1.9 with Pro; license testers verify purchases |
| 7 | Production, staged 10% → 50% → 100% | v2.0.0 |

Recruit testers from the app's own audience: a short post in r/androidapps, r/productivity, and the local-first/Obsidian communities offering the closed-test link. Twelve is a low bar.

## 3. Store listing

**App name**: Peggy
**Short description (≤80)**: Capture anything in two seconds. Plan, sort, and remember, all on your device.
**Full description**:

> Peggy is the fastest way to get a thought out of your head and into the right place.
>
> Type "call sam tomorrow at 3" and it's a task on Thursday at 3 PM. "lunch with jess friday noon for an hour" is an event. "gym every weekday" repeats. Ideas become notes. Nothing needs a form, a date picker, or a decision.
>
> **Plan** shows your day, week, month, or everything, with repeats rolled up so a daily habit is one line, not thirty. **Sort** empties your inbox with a swipe: right for today, left for someday. **Ideas** keeps your notes in a rich editor. **Ask** lets you talk to your own data with your own AI key, or with Peggy Pro.
>
> Everything lives on your phone. No account. No cloud. Export a backup whenever you like. Undo everywhere instead of "are you sure?" dialogs.
>
> - Reminders that ring on time, with Done and Snooze from the notification
> - Home-screen widgets and Quick Settings tiles for capture and today
> - Share text from any app straight into Peggy
> - Material You colours, dark theme, pure black
> - App lock with your device credential
> - Windows desktop app included
>
> Peggy Pro (optional): AI without keys, extra themes and icons, more widgets, unlimited Ask history, and sync between your devices when it ships.

**Category**: Productivity. **Tags**: to-do list, notes, calendar, reminders.
**Contact email**: the developer address on the Play account. **Website**: the GitHub repo or Pages site.

**Graphics**
- App icon 512×512 PNG (generated from the adaptive icon in `composeApp/src/androidMain/res`).
- Feature graphic 1024×500: brand colour background, the icon, "Capture in two seconds." A generator script lives in `docs/assets/` once created.
- Phone screenshots, 1080×2400, 6-8, in this order with captions: Capture with chips ("Type it. Peggy files it."), Plan Day ("Your day, in order"), Plan Week with a rolled-up habit ("Habits are one line"), Sort swipe ("Empty the inbox with a swipe"), Ideas grid, Ask, Settings hub, Widgets on a home screen.
- 7-inch and 10-inch tablet screenshots after the v1.8 layout work.

**What's new (per release)**: three to five user-facing lines, copied from `CHANGELOG.md`.

## 4. Data Safety answers

Fill the form exactly like this. It is accurate for v1.7+ (API key no longer in backups, cleartext restricted).

- Does your app collect or share any of the required user data types? **Yes** (only when the user enables an AI provider).
- Is all user data encrypted in transit? **Yes.**
- Do you provide a way for users to request deletion? **Yes** (uninstalling deletes everything on device; for AI providers, link to the provider's policy; email for anything else).

Data types:

| Category | Type | Collected | Shared | Optional | Purpose |
|---|---|---|---|---|---|
| Personal info | Other info (names inside tasks/notes) | No | Yes | Yes | App functionality |
| App activity | Other user-generated content (captures, tasks, events, notes) | No | Yes | Yes | App functionality |
| App info and performance | Crash logs | No | No | — | — (none collected until a crash reporter is added; update then) |

"Shared" means sent to the AI provider the user configured (Anthropic, OpenAI, a custom endpoint, or Peggy's relay for Pro). "Collected" stays No because Peggy operates no server that stores user data; if the Pro relay ever logs content, change this.

Nothing is collected for analytics or advertising. No device or other IDs. No location, contacts, files, or messages.

## 5. Permission declarations

**USE_EXACT_ALARM** (Play requires a declaration): "Peggy is a reminders and calendar app. Its core function is to remind the user of tasks and events at the exact time they set. Exact alarms are used only for user-created reminders and the user's chosen daily digest times."

**RECEIVE_BOOT_COMPLETED**: re-arms reminders after a restart. **POST_NOTIFICATIONS**: reminders and digests. **ACCESS_LOCAL_NETWORK** (Android 17): only to reach a self-hosted Ollama on the user's LAN when they configure one. No other sensitive permissions. No `QUERY_ALL_PACKAGES`, no foreground services, no accessibility service.

## 6. Build and release mechanics

- Artifact: `.\gradlew :composeApp:bundleRelease` → `composeApp/build/outputs/bundle/release/composeApp-release.aab`. The APK remains for GitHub releases and sideloading.
- Version: `versionCode` increments every upload; `versionName` follows the tag. The About screen reads the version from Gradle (`BuildConfig`-equivalent), never from a constant.
- R8 and resource shrinking on for release with `proguard-rules.pro`; test the release build on the emulator before each upload (capture, Ask with Ollama at `10.0.2.2:11434`, notes editor, backup import, widgets).
- Pre-launch report: read it for every track upload; fix crashes and accessibility findings it reports.
- Rollout: 10% for 24 h, 50% for 48 h, then 100%, halting if crash rate > 0.5% or ANR > 0.2%.

## 7. Post-launch

- Watch Android vitals daily for the first two weeks.
- Reply to every review in the first month.
- Keep `CHANGELOG.md` and the What's new in sync; ship monthly.
- Revisit prices after 90 days of data (`MONETIZATION.md` §9).

## 8. Remaining manual steps after the overnight pass

- Host the privacy policy and paste its URL.
- Create the Play app, upload the first AAB to internal testing, add testers.
- Submit the `USE_EXACT_ALARM` declaration with the text above.
- Rotate the upload keystore password; delete it from README history if the repo ever goes public.
- Create the Pro products in Play Console with the SKU ids in `MONETIZATION.md` when v1.9 ships.
