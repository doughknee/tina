// Cut and publish a Peggy release from the command line, through the Play Developer API.
//
//   node release/publish.mjs internal            build, upload and publish the current version
//   node release/publish.mjs internal --cut 1.9.4  bump to 1.9.4 first (version code +1, CHANGELOG
//                                                 "Unreleased" becomes the release, commit + tag)
//   node release/publish.mjs alpha --no-build    reuse the bundle already in build/outputs
//   node release/publish.mjs internal --check    only prove the service account can see the app
//
// Needs release/service-account.json (gitignored): the same service account the relay uses,
// with "Release apps to testing tracks" on Peggy in Play Console. Release notes are the
// CHANGELOG section for the version being shipped. Closed tracks go to review on their own.
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { execSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { signJwt } from "../relay/src/play.js";
import { bumpVersion, cutChangelog, parseVersion, releaseNotes as notesFor } from "./lib.mjs";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const GRADLE = path.join(ROOT, "composeApp", "build.gradle.kts");
const CHANGELOG = path.join(ROOT, "CHANGELOG.md");
const AAB = path.join(ROOT, "composeApp", "build", "outputs", "bundle", "release", "composeApp-release.aab");
const PACKAGE = "com.peggy.app";
const API = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE}`;
const UPLOAD = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${PACKAGE}`;

const args = process.argv.slice(2);
const track = args.find((a) => !a.startsWith("--"));
const flag = (name) => args.includes(`--${name}`);
const value = (name) => { const i = args.indexOf(`--${name}`); return i >= 0 ? args[i + 1] : undefined; };
if (!track && !flag("check")) die("which track? internal, alpha or production");

const saPath = value("sa") ?? path.join(ROOT, "release", "service-account.json");
if (!existsSync(saPath)) die(`no service account at ${saPath}; drop the JSON key there (it is gitignored)`);
const sa = JSON.parse(readFileSync(saPath, "utf8"));

// --- 1. optionally cut the version
if (value("cut")) cut(value("cut"), track);
const { versionName, versionCode } = readVersion();

// --- 2. token
const access = await accessToken(sa);
const auth = { authorization: `Bearer ${access}` };

if (flag("check")) {
  const edit = await api("POST", `${API}/edits`, auth);
  const tracks = await api("GET", `${API}/edits/${edit.id}/tracks`, auth);
  await api("DELETE", `${API}/edits/${edit.id}`, auth, undefined, true);
  console.log(`ok: ${sa.client_email} can see ${PACKAGE}; tracks: ${(tracks.tracks ?? []).map((t) => t.track).join(", ")}`);
  process.exit(0);
}

// --- 3. bundle
if (!flag("no-build")) {
  console.log(`building ${versionName} (${versionCode})…`);
  execSync(`${process.platform === "win32" ? "gradlew.bat" : "./gradlew"} :composeApp:bundleRelease -q`, { cwd: ROOT, stdio: "inherit" });
}
const aab = readFileSync(value("aab") ?? AAB);

// --- 4. upload and publish, all inside one edit
const edit = await api("POST", `${API}/edits`, auth);
console.log(`uploading ${(aab.length / 1e6).toFixed(1)} MB…`);
const bundle = await api("POST", `${UPLOAD}/edits/${edit.id}/bundles?uploadType=media`,
  { ...auth, "content-type": "application/octet-stream" }, aab);
if (bundle.versionCode !== versionCode) die(`bundle is version code ${bundle.versionCode}, gradle says ${versionCode}; rebuild`);

const notes = releaseNotes(versionName);
await api("PUT", `${API}/edits/${edit.id}/tracks/${track}`, { ...auth, "content-type": "application/json" }, JSON.stringify({
  track,
  releases: [{
    name: `${versionCode} (${versionName})`,
    versionCodes: [String(versionCode)],
    status: value("status") ?? "completed",
    releaseNotes: notes ? [{ language: "en-US", text: notes }] : [],
  }],
}));
await api("POST", `${API}/edits/${edit.id}:commit`, auth);
console.log(`published ${versionCode} (${versionName}) to ${track}${track === "internal" ? "" : " (closed tracks go to review)"}`);
if (notes) console.log(`notes:\n${notes}`);

// --------------------------------------------------------------------------------------

function die(msg) { console.error(msg); process.exit(1); }

function readVersion() { return parseVersion(readFileSync(GRADLE, "utf8")); }

/** Bumps gradle, retitles the CHANGELOG's Unreleased section, commits and tags. */
function cut(newName, track) {
  const { versionName, versionCode } = readVersion();
  if (newName === versionName) die(`already at ${versionName}`);
  writeFileSync(GRADLE, bumpVersion(readFileSync(GRADLE, "utf8"), newName));
  const label = { internal: "internal testing", alpha: "closed testing", production: "production" }[track] ?? track;
  const cutLog = cutChangelog(readFileSync(CHANGELOG, "utf8"), newName, label, new Date().toISOString().slice(0, 10));
  if (!cutLog) die("CHANGELOG.md has no Unreleased section to cut");
  writeFileSync(CHANGELOG, cutLog);
  execSync(`git add "${GRADLE}" "${CHANGELOG}" && git commit -q -m "Peggy ${newName}" && git tag -a v${newName} -m "v${newName}"`, { cwd: ROOT, stdio: "inherit" });
  console.log(`cut v${newName} (${versionCode + 1}); push with: git push --follow-tags`);
}

function releaseNotes(versionName) { return notesFor(readFileSync(CHANGELOG, "utf8"), versionName); }

async function accessToken(sa) {
  const now = Math.floor(Date.now() / 1000);
  const jwt = await signJwt(
    { iss: sa.client_email, scope: "https://www.googleapis.com/auth/androidpublisher", aud: sa.token_uri, iat: now, exp: now + 3600 },
    sa.private_key,
  );
  const r = await fetch(sa.token_uri, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: jwt }),
  });
  if (!r.ok) die(`google token: ${r.status} ${await r.text()}`);
  return (await r.json()).access_token;
}

async function api(method, url, headers, body, allowEmpty = false) {
  const r = await fetch(url, { method, headers, body });
  const text = await r.text();
  if (!r.ok) die(`${method} ${url.replace(API, "").replace(UPLOAD, "")}: ${r.status} ${text.slice(0, 400)}`);
  return text && !allowEmpty ? JSON.parse(text) : {};
}
