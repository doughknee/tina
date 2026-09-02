# Audit: AI parsing, Improve, and Ask

Read-only audit performed 2026-09-02 against v1.6.0 (`aaf28e5`). Paths relative to `composeApp/src/`.

## 1. Providers, requests, cost, streaming, errors

`AiProvider` (`Settings.kt:27`): `OFF, OLLAMA, ANTHROPIC, OPENAI, CUSTOM`. Two wire shapes: Anthropic native (`/v1/messages`) and OpenAI-compatible (`/chat/completions`) used for Ollama/OpenAI/Custom. Model list hardcoded, Anthropic only (`AiCaptureParser.kt:41-46`), default `claude-opus-5`.

| Path | Payload | ~tokens in |
|---|---|---|
| Capture parse | instructions + the one captured line | 450-550 |
| Improve one item | instructions + one item + standing `aiInstructions` | 450-600 |
| Ask (read / write) | **the entire non-deleted database**, one line per item, capped at 80,000 chars (~20-25k tokens), rebuilt every turn, plus the full message history | up to 25k |

- No thinking/effort config, no prompt caching, no token counting. `max_tokens` 2048 (chat) / 1024 (parse); none on the OpenAI path.
- **No streaming**: a spinner for the whole wait, up to the 180 s timeout. **No retries** on 429/5xx.
- Errors collapse to one message ("Couldn't reach the model"); capture refinement fails silently; provider OFF, blank model and Wi-Fi-only-on-metered all render as connection failures. Only the Test Connection button reports the real cause.

## 2. Security and privacy

- **API key in plaintext DataStore** (`Settings.kt:104,156,219`), world-readable on desktop, **written into every exported backup** (`Backup.kt:17,63`), restored on import; `BackupTest.kt:33,50` locks this in. Every keystroke of the key field hits disk.
- `usesCleartextTraffic="true"` app-wide (`AndroidManifest.xml:16`) and the base URL is free text with no scheme validation: the whole database can go out over plain HTTP with the bearer token.
- No logging of bodies (good); provider error bodies are echoed into a settings snackbar (`AiCaptureParser.kt:211,241`).
- `RequestAiNetworkPermissions` exists for Android 17's local-network permission but is never called.

**Play Data Safety**: with a cloud provider on, the whole task/calendar/notes database is *shared with a third party*. Must declare Personal info, Calendar, App activity, Other user-generated content; purpose App functionality; optional (provider defaults OFF). "Encrypted in transit" cannot be answered yes until cleartext is restricted. No deletion mechanism or retention statement. The in-app disclosure (`strings.xml:141`, "capture text") is inaccurate for Ask (sends everything) and not prominent. Gen-AI policy needs an in-app way to report offensive output.

## 3. Ask write actions

Protocol in prose in the system prompt (`AiChat.kt:88-99`); the model appends `{"actions":[…]}`. Parser (`AskActions.kt:28-65`) is solid and tested. `applyActions` (`AskViewModel.kt:239-322`): unknown ops dropped, ids validated, dates `runCatching`-parsed.

- **No cap on batch size, no confirmation, executed the instant the reply arrives.**
- A malformed date (`"next friday"`) parses to null and the null branch **clears the item's entire schedule** (`:297-301`).
- Undo: last batch only, replayed **forward** not in reverse (`:164`), 5-second window by default.
- **Prompt injection**: share-sheet content is interpolated unescaped into the DATABASE block; a note body can instruct the model to emit a delete batch. Write mode persists across sessions once enabled.

## 4. What hosted "tina Pro" AI needs

1. A stateless relay (`/v1/ask`, `/v1/parse`, `/v1/improve`) holding the provider key; a `PRO` value on `AiProvider` and one branch in each request builder on the client. Pin models server-side.
2. Auth: Play Billing purchase token → server verification (`purchases.subscriptionsv2.get`) → short-lived bearer token. Nothing in the repo touches Billing today.
3. Quotas at the relay (monthly tokens, per-minute rate limit) with structured 429s the app can show.
4. Abuse controls: device binding, request-size cap in tokens, turn caps, per-account anomaly alerts and kill switch, short token TTL.
5. Compliance: subprocessor disclosure, retention statement, deletion endpoint, Gen-AI content report.

**Cost per typical user per month** (300 items, 5 captures/day, 20 improves, 10 Ask chats × 3 turns): ~0.33 MTok in, ~0.03 MTok out.

| Model | Cost/user/month |
|---|---|
| claude-opus-5 (current default) | ~$2.40, realistically $3-4 with thinking |
| claude-sonnet-5 | ~$0.97 |
| claude-haiku-4-5 | ~$0.48 |

Heavy users (1,500 items, 200 Ask turns) cost ~$25/month on Opus because the context cap is in characters, rebuilt each turn, uncached. **Recommendations**: Haiku for parse/improve, top model for Ask only, build the DB context once per chat and put it behind a cache breakpoint, explicit `effort`, relay-side token cap. Then a $4.99/month tier has ~$0.80-1.20 COGS.

## 5. Bugs and gaps, ranked

**P0**
1. API key plaintext and in backups.
2. Prompt injection into a write-capable agent with no confirmation or batch cap.
3. Cleartext + unvalidated base URL.
4. Wrong Haiku model id (`claude-haiku-4-5-20251001` → `claude-haiku-4-5`).

**P1**
5. All errors collapse to one opaque message.
6. Wi-Fi-only reports as a connection failure.
7. `RequestAiNetworkPermissions` never called.
8. Malformed date silently unschedules an item.
9. No retries on 429/5xx.
10. `send()` can swallow a message when a send is already in flight.
11. Undo replays forward.
12. `max_tokens: 2048` can truncate the actions block after a thorough answer.
13. Share-captured items auto-refined with no undo; `parseCapture` called without `firstDayOfWeek` in `ShareActivity`.

**P2**
14. Chat not restored after process death (`currentChatId` not saved).
15. No model list refresh.
16. No cost/token display.
17. No streaming.
18. Model override Anthropic-only in the UI.
19. `modelOverride` leaks across chats.
20. `SuggestionCache` unbounded.
21. Key field writes on every keystroke.
22. Privacy note inaccurate and hidden.
23. `SUGGEST` refine mode ignores the raw text.
24. No conversation trimming.

## 6. Tests

Present: `AiJsonMappingTest`, `AskActionsTest`, `ImprovePatchTest` (21 pure-function tests). Absent: request construction (a `MockEngine` fixture would catch the model-id class of bug), `applyActions` and its undo, `buildAskContext` cap, the write/read-only prompt branch, `CaptureRefiner`, settings round-trips, any UI test.
