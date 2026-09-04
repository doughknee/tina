---
name: settings-audit
description: Check that Peggy's Settings pages (rows, subtitles, subpages, What's new, onboarding cards) still describe the app as it is, and fix what drifted. Use after any feature work, rename, or release, or when the user says "settings audit", "are settings up to date", "settings pass".
---

# Settings audit

Settings copy drifts because it is prose about the app, and the app changes without it.
This skill derives the truth from the code every time it runs. It names no rows, no
strings and no features, so it cannot go stale itself; only the file roles below can, and
step 0 handles that.

## Step 0 — locate the sources of truth (never assume paths)

Find each of these by grep, not from memory. If one is missing, look for where the role
moved (grep the symbol), then fix the pattern in this file as part of the run.

| Role | How to find it |
|---|---|
| Settings sections and rows | `grep -rln "SettingsSection(" composeApp/src/commonMain` — rows are `SettingsRow.*` entries with `id`, `title`, `supporting`, `keywords`, `visible`, `checked`/`onClick` |
| Settings subpages with hardcoded lists | `grep -rln "InfoSubpage(\|ChoiceSubpage(" composeApp/src/commonMain` — What's new, Widgets, Shortcuts, Licenses, Open app to live here as literal `entries`/`options` |
| Persisted settings model | `grep -rn "data class Settings(" composeApp/src/commonMain` and its `SettingsRepository` setters |
| UI strings | `composeApp/src/commonMain/composeResources/values/strings.xml` (if that moves: `find composeApp -name strings.xml`) |
| Tab names | `grep -n 'name="tab_' <strings.xml>` and `enum class TinaTab` |
| Version | `versionName` in `composeApp/build.gradle.kts` |
| Release notes | `CHANGELOG.md` top section |
| Desktop key bindings | `grep -rn "KeyBus.emit(KeyCommand" composeApp/src/desktopMain` |
| Widgets | `grep -rln "GlanceAppWidget" composeApp/src/androidMain` |
| Onboarding cards | `grep -n 'name="onb_' <strings.xml>` and the onboarding screen (`grep -rln OnboardingScreen`) |
| Notification digests | `grep -rln "Digest\|Nudge" composeApp/src/commonMain/kotlin/com/tina/app/notifications` |
| Design decisions | `DECISIONS.md` (what a feature is *meant* to do when code and copy disagree) |

## Step 1 — build the inventory (mechanical, no judgement yet)

Produce two lists and keep them in the scratchpad:

1. **Every settings row**: section id, row id, `visible` condition, and the resolved text of
   its title and supporting string (follow `Res.string.x` into strings.xml, including
   format arguments). Include the subpage `entries`/`options` literals.
2. **Every fact the copy could be wrong about**, pulled from code, not from memory:
   - the three tab names and what each page does (its screen file's KDoc and the empty-state strings);
   - each `Settings` field: which row reads it (`grep "settings.<field>"` in the settings screen) and which code honours it (`grep "<field>"` outside settings);
   - each `OpenAppTo`/enum choice and what the shell does with it;
   - the key bindings actually emitted on desktop;
   - the widgets that exist and what their code does (buttons, deep links, refresh);
   - the notifications that exist and when they fire;
   - `versionName` and the CHANGELOG's top section;
   - any strings or KDoc still using a retired name (old app name, old tab names, old page names — read the current names from the tab strings and the rename entry in DECISIONS.md, then grep for what they replaced).

## Step 2 — cross-check

Report a finding for each of these. Quote the current text and the code that contradicts it.

- **Row describes behaviour the code no longer has** (a subtitle naming a page, gesture, count or timing the code does not implement). Read the setter's consumers, not the setter.
- **Setting persisted but not honoured**: a `Settings` field with a row and no consumer outside settings, or a consumer that ignores some enum values. Either wire it or hide the row; never leave a switch that does nothing.
- **Behaviour with no row**: a `Settings` field or a user-facing toggle in code that no row exposes. Decide with DECISIONS.md whether it is deliberate (say so) or missing.
- **Subpage literals out of date**: What's new must have an entry whose version matches the major.minor of `versionName` and summarises the CHANGELOG top section; Widgets must list exactly the widgets in code with what they do; Shortcuts must list exactly the emitted bindings and the row's count must equal the list size; Licenses must cover the dependencies in `gradle/libs.versions.toml`.
- **Names**: any retired name in settings copy, subpages, onboarding strings, or the notification titles. A word that is also ordinary English (for example a day name used as a date label) is only a finding when it names a page.
- **Onboarding cards** still describe the current pages and gestures (they are settings-adjacent copy and drift the same way).
- **Keywords**: each row's `keywords` include the current page name it relates to, so settings search finds it under the name the user sees.
- **Visibility**: rows gated on a platform or a flag are gated on the one the code still uses.

## Step 3 — fix, then prove it

- Fix copy in strings.xml; fix wiring in code. Small, direct edits; do not restructure the settings screen.
- Add a CHANGELOG line under the current dev section when a user-visible row changed.
- Build both targets and run the tests: `gradlew :composeApp:compileKotlinDesktop :composeApp:desktopTest :composeApp:assembleDev` (set `JAVA_HOME` per README / memory).
- Install the dev build on whatever device is connected and screenshot the changed rows and any subpage you touched; for a wiring fix (like a start page), exercise it and screenshot the result.
- Commit with a message that lists what drifted, and per CLAUDE.md log it in Linear.

## Output

A short report: what drifted (quoted), what was changed, what was left alone and why, and
the screenshots. If nothing drifted, say so in one line; do not invent findings.
