# Peggy privacy policy

Effective 2026-09-02. Applies to the Peggy Android and desktop apps published by Brandon Harris.

## The short version

Peggy keeps your data on your device. It has no account system, no analytics, no advertising, and no server of its own. The only time your data leaves your device is when **you** turn on an AI feature and choose where it goes.

## What Peggy stores

Tasks, events, notes, tags, settings, and (if you use Ask) your conversations. All of it is stored in a database on your device. Uninstalling Peggy deletes it. You can export a copy at any time from Settings → Data, and delete everything from the same place.

## Android backup

Android may include Peggy's database in the device backup to your own Google account if you have device backup enabled. Peggy excludes its settings file, including any AI key, from that backup. You control device backup in your Android settings.

## AI features (optional, off by default)

If you enable an AI provider in Settings → Parsing & AI, Peggy sends text to that provider:

- **Capture parsing and Improve** send the text of the item being parsed or improved.
- **Ask** sends your question, the conversation so far, and a summary of your tasks, events, and notes so the model can answer about them.

Where it goes depends on your choice:

- **Ollama** on your own computer or network: nothing leaves your network.
- **Anthropic** or **OpenAI**: their API, under their privacy policies, using your own API key.
- **A custom endpoint** you configure.
- **Peggy Pro** (when available): Peggy's relay service forwards requests to a model provider with Peggy's own key. The relay does not store the content of your requests; it keeps a count of requests per subscription for quota purposes.

Your API key is stored on your device only, encrypted with the device keystore on Android, and is never included in exports or backups.

You can report an AI response you find inappropriate from the Ask screen; the report contains only the text you choose to include.

## Purchases

Peggy Pro is sold through Google Play. Google processes the payment; Peggy receives a purchase token to unlock features and never sees your payment details.

## Permissions

- Notifications and exact alarms: to ring reminders at the time you set.
- Boot completed: to re-arm reminders after a restart.
- Local network (Android 17+): only to reach an Ollama server you configured.
- Microphone is not requested; voice capture uses your device's speech recognizer through its own interface.

## Children

Peggy is not directed at children under 13.

## Changes

Changes to this policy are published with the app release notes and at this address.

## Contact

Questions or deletion requests: bcharris713@gmail.com
