# Audit: data layer, backup, storage

Read-only audit performed 2026-09-02 against v1.6.0 (`aaf28e5`). Paths relative to `composeApp/src/`.

## 1. Schema and migrations

Tables at v5 (`AppDatabase.kt:15-25`; exported schemas in `composeApp/schemas/.../{2,3,4,5}.json`):

- `items` — id, title, body, type, createdAt, updatedAt, tags (CSV), color, pinned, dueDate (epoch day), dueTime (minute of day), completed, completedAt, priority, reminderOffsetMinutes, sortOrder, startAt/endAt (epoch millis), allDay, recurrence (RRULE), deletedAt. **No indices, no foreign keys.**
- `chats`, `chat_messages` (index on chatId, no FK/cascade).
- `occurrence_completions` — PK (itemId, epochDay), skipped, completedAt. No FK, never garbage-collected.

Migrations 2→3, 3→4, 4→5 are additive and match the exported schemas. **`fallbackToDestructiveMigration(dropAllTables = true)` (`AppDatabase.kt:79`) applies to every missing path**: a forgotten migration or an APK downgrade silently drops every table. Fix: `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)`. No migration tests, no CI.

## 2. Backup

JSON `{version, exportedAt, items, settings?}` (`Backup.kt:100-119`); the wire format is the Room entity. `version` is never read. Includes non-trashed items and settings (including the plaintext **AI API key**).

Excluded silently: trashed items, `occurrence_completions` (all recurring-task history), chats.

Import (`ItemRepository.kt:163-174`) is additive: dedups on `(title, createdAt)`, reassigns ids, no transaction, schedules an alarm per imported item, and **applies settings (including the API key) before items, unconditionally, with no confirmation**.

Auto-backup (Android only): weekly WorkManager job to `filesDir/backups/` (deleted on uninstall), keeps 4, non-atomic `writeText`, silent failures, `KEEP` policy so changes never reach existing installs. Desktop has none.

Android system backup is unconfigured: no `allowBackup`/`dataExtractionRules`, so the WAL-mode DB and the settings file (with the key) go to Google Drive uncheckpointed.

Ways a user loses data, ranked: missing migration/downgrade; uninstall with only app-private auto-backups; restore losing trash/occurrences/chats; import clobbering settings; corrupt auto-backup evicting a good one; duplicate rows after edit+import; inconsistent cloud restore; empty-trash/delete-everything with no export prompt.

## 3. Repository invariants

- Soft delete is applied consistently to reads. `softDelete` and `setSortOrder` do not bump `updatedAt` (`ItemDao.kt:18-19,107-108`).
- `restore` ignores the caller's item in the still-exists branch; `AgendaViewModel.restoreItem` uses `update` (never clears `deletedAt`); `restoreAll` is a no-op for purged rows.
- Trash purge runs only on Android cold start; desktop never purges.
- Occurrence completion rows are never deleted with their item.
- `sortOrder` mixes `createdAt` millis with dense drag indices.
- Note editor runs two independent debounced read-modify-write flows (title 400 ms, body 500 ms) that can clobber each other.
- **All-day events stored as absolute millis at local midnight** shift a day when the timezone changes (`ItemRepository.kt:226-231`, `EventEditorViewModel.kt:47-55`, `AgendaRange.kt:468-475`). Tasks (epoch day + minute) are right.
- `AgendaViewModel` caches the timezone once.
- `observeEventsIntersecting` returns every recurring event regardless of range.

## 4. Distance from sync

Needs: stable ids (UUID), tombstones on occurrence_completions (currently hard delete), `updatedAt` everywhere, a version/dirty column, transactional batching, a conflict strategy. `items` is ~2 additive migrations away (uuid + version); `occurrence_completions` needs a remodel.

## 5. Performance

`observeAll()` has five simultaneous subscribers (agenda state, dots, search, capture, settings stats) all re-materialising the table on any write. No indices. Search is unindexed `LIKE '%q%'` over HTML bodies with no `%`/`_` escaping. `dots` expands every recurring series over 91 days per emission. Tag queries filter CSV in memory. N+1 untransacted writes in rename/remove tag, reorder, clear completed, import.

## 6. Bugs and gaps, ranked

1. Critical — destructive fallback for every missing migration (`AppDatabase.kt:79`).
2. Critical — no migration tests, no CI.
3. High — backups exclude occurrences, chats, trash.
4. High — no backup rules; DB + plaintext key to Google Drive.
5. High — import overwrites settings and the key unconditionally.
6. High — all-day events shift with timezone.
7. High — non-atomic auto-backup write.
8. High — import dedup/id reassignment; untransacted.
9. Med-High — backup `version` never checked.
10. Medium — `updatedAt` gaps; orphan occurrence rows; sortOrder magnitudes; note editor race; digest event filter; search escaping; purge only on Android start; silent auto-backup failures.
11. Low — `restoreAll`/`restoreItem` paths; `KEEP` policy; alarm op per import; cached timezone; CSV tags; null stream reported as bad file; chats not cleared by delete-everything; desktop export extension.

## 7. Tests

Strong: recurrence (22), agenda builder, capture→item, backup codec. `ItemRepositoryTest` (desktop, real Room, 9 tests). Zero: migrations, import, backup handler wiring, what backups omit, auto-backup, `OccurrenceRepository`, `SettingsRepository`, timezone behaviour, `ChatDao`, search semantics.

Three highest-leverage moves: (1) `fallbackToDestructiveMigrationFrom(1)` + migration test + CI; (2) backup v2 with checked version, occurrences, trash, chats, uuid dedup, transaction, settings prompt; (3) add `uuid`/`version` to items in the same migration.
