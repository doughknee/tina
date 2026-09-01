package com.tina.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Backup(
    val version: Int = 1,
    val exportedAt: Long,
    val items: List<Item>,
)

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}

fun encodeBackup(items: List<Item>, exportedAt: Long): String =
    json.encodeToString(Backup(exportedAt = exportedAt, items = items))

/** Returns null if the file isn't a tina backup. */
fun decodeBackup(text: String): Backup? = runCatching { json.decodeFromString<Backup>(text) }.getOrNull()
