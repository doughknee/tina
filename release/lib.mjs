// The parts of publishing that have no network in them, so they can be checked.

/** versionName and versionCode from build.gradle.kts text. */
export function parseVersion(gradle) {
  return {
    versionName: gradle.match(/versionName = "([^"]+)"/)[1],
    versionCode: Number(gradle.match(/versionCode = (\d+)/)[1]),
  };
}

/** Bumps the code by one and sets the name. */
export function bumpVersion(gradle, newName) {
  const { versionName, versionCode } = parseVersion(gradle);
  return gradle
    .replace(`versionCode = ${versionCode}`, `versionCode = ${versionCode + 1}`)
    .replace(`versionName = "${versionName}"`, `versionName = "${newName}"`);
}

/**
 * The CHANGELOG section for one version as the plain text Play shows under "What's new":
 * bullets kept, sub-headings dropped, bold stripped, cut to Play's 500 characters.
 */
export function releaseNotes(changelog, versionName) {
  const lines = changelog.split(/\r?\n/);
  const start = lines.findIndex((l) => l.startsWith(`## v${versionName}`));
  if (start < 0) return "";
  const body = [];
  for (const line of lines.slice(start + 1)) {
    if (line.startsWith("## ")) break;
    const t = line.trim();
    if (!t || t.startsWith("#")) continue;
    body.push(t.replace(/^- /, "• ").replace(/\*\*/g, ""));
  }
  const text = body.join("\n");
  return text.length > 500 ? text.slice(0, 497) + "…" : text;
}

/** "## Unreleased" becomes the heading for this release; null when there is nothing to cut. */
export function cutChangelog(changelog, versionName, trackLabel, date) {
  if (!changelog.includes("## Unreleased")) return null;
  return changelog.replace("## Unreleased", `## v${versionName} (${trackLabel}, ${date})`);
}
