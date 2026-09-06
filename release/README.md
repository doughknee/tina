# Releasing Peggy

One command builds, uploads and publishes through the Play Developer API. No browser.

```bash
node release/publish.mjs internal --cut 1.9.4
```

That bumps the version code, renames the CHANGELOG's `## Unreleased` section to the release,
commits and tags, builds the bundle, uploads it, sets the track's release notes from that
CHANGELOG section, and publishes. Then `git push --follow-tags`.

| Command | What it does |
|---|---|
| `node release/publish.mjs internal` | ship the version already in `build.gradle.kts` to internal testing |
| `… --cut 1.9.4` | bump to 1.9.4 first |
| `… alpha --no-build` | promote the version already on Play to closed testing (Play sends it to review on its own) |
| `… --no-build` | reuse the bundle already in `composeApp/build/outputs` |
| `… --status draft` | upload and set the track without publishing |
| `… --check` | only confirm the service account can see the app and list the tracks |

Release notes are whatever is under the version's heading in `CHANGELOG.md`, bullets and
all, cut to Play's 500 characters. Keep writing them there and nothing else is needed.

## One-time setup

1. Put the service-account JSON key (the same account the relay uses) at
   `release/service-account.json`. It is gitignored; never commit it.
2. In Play Console → Users and permissions → that account → App permissions on Peggy, add
   **Release apps to testing tracks** (and **Release to production** when the time comes).
   The relay's own permission, View financial data, is not enough to publish.
3. `node release/publish.mjs internal --check` should print the account email and the tracks.

## Why

The console's upload page needs a hand-dragged file and a click it sometimes ignores, and a
release meant five of those. The API takes the bundle straight from Gradle's output.
