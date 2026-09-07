package com.tina.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.tina.app.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * What's new is CHANGELOG.md, shipped as a resource at build time (build.gradle.kts
 * `copyChangelog`), so release notes are written once. The page shows the running feature
 * release: every `## vX.Y.z` section for the build's major.minor, newest first, plus
 * `## Unreleased` when the build carries one (dev builds; a cut release never does).
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberWhatsNew(): List<Pair<String, String>>? {
    val entries by produceState<List<Pair<String, String>>?>(null) {
        value = whatsNewEntries(Res.readBytes("files/CHANGELOG.md").decodeToString(), appVersionName())
    }
    return entries
}

/**
 * The (heading, text) entries the page renders: each `### Heading` inside a matching section
 * becomes "version · Heading" over its bullets; bullets before any heading sit under the version.
 * Mirrors the section cut in release/lib.mjs: bullets kept, bold stripped.
 */
fun whatsNewEntries(changelog: String, versionName: String): List<Pair<String, String>> {
    val feature = featureVersion(versionName)
    val entries = mutableListOf<Pair<String, String>>()
    var version: String? = null
    var heading: String? = null
    val bullets = mutableListOf<String>()
    fun flush() {
        val v = version ?: return
        if (bullets.isEmpty()) return
        entries += (heading?.let { "$v · $it" } ?: v) to bullets.joinToString("\n")
        bullets.clear()
    }
    for (raw in changelog.lineSequence()) {
        val line = raw.trim()
        when {
            line.startsWith("## ") -> {
                flush()
                heading = null
                val title = line.removePrefix("## ").substringBefore(" (").trim()
                version = when {
                    title == "Unreleased" -> title
                    title.startsWith("v") && featureVersion(title.drop(1)) == feature -> title.drop(1)
                    else -> null
                }
            }
            line.startsWith("### ") -> { flush(); heading = line.removePrefix("### ").trim() }
            line.startsWith("- ") && version != null -> bullets += "• " + line.removePrefix("- ").replace("**", "")
        }
    }
    flush()
    return entries
}

/** "1.8.2-dev" → "1.8": the feature release a build belongs to. */
fun featureVersion(versionName: String): String =
    versionName.substringBefore('-').split('.').take(2).joinToString(".")
