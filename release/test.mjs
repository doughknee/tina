// `node release/test.mjs`: the publish script's text handling, with the CHANGELOG's CRLF endings.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { bumpVersion, cutChangelog, parseVersion, releaseNotes } from "./lib.mjs";

const gradle = 'foo\n        versionCode = 20\n        versionName = "1.9.3"\nbar';
assert.deepEqual(parseVersion(gradle), { versionName: "1.9.3", versionCode: 20 });
assert.deepEqual(parseVersion(bumpVersion(gradle, "1.9.4")), { versionName: "1.9.4", versionCode: 21 });

const log = [
  "# Changelog", "", "## Unreleased", "", "### Peggy Pro", "- App icons: **eight** colours.", "",
  "## v1.9.3 (internal testing, 2026-09-05)", "", "- Ask keeps the end in view.", "",
  "## v1.9.2 (internal testing, 2026-09-05)", "- older",
].join("\r\n");
assert.equal(releaseNotes(log, "1.9.3"), "• Ask keeps the end in view.");
assert.equal(releaseNotes(log, "1.9.2"), "• older");
assert.equal(releaseNotes(log, "9.9.9"), "");
const cutLog = cutChangelog(log, "1.9.4", "internal testing", "2026-09-06");
assert.ok(cutLog.includes("## v1.9.4 (internal testing, 2026-09-06)"));
assert.equal(releaseNotes(cutLog, "1.9.4"), "• App icons: eight colours.");
assert.equal(cutChangelog("## v1.0.0\n- x", "1.0.1", "internal testing", "d"), null);
assert.ok(releaseNotes("## v1\n- " + "x".repeat(600), "1").length <= 500);

// the real files parse too
const real = readFileSync(new URL("../CHANGELOG.md", import.meta.url), "utf8");
assert.ok(releaseNotes(real, "1.9.3").startsWith("• Ask keeps"), "real CHANGELOG");
assert.ok(parseVersion(readFileSync(new URL("../composeApp/build.gradle.kts", import.meta.url), "utf8")).versionCode >= 20);
console.log("release self-check ok");
