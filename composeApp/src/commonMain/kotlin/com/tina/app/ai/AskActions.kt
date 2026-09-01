package com.tina.app.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val actionsJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
data class AskAction(
    val op: String = "",
    val id: Long? = null,
    val title: String? = null,
    val date: String? = null,
    val time: String? = null,
    val priority: String? = null,
    val tags: List<String>? = null,
    val type: String? = null,
    val durationMinutes: Int? = null,
)

@Serializable
private data class AskActionsBlock(val actions: List<AskAction> = emptyList())

/**
 * Pulls the trailing {"actions":[...]} block out of a model reply.
 * Returns the reply with the block removed, plus the parsed actions.
 */
fun extractAskActions(reply: String): Pair<String, List<AskAction>> {
    val marker = reply.lastIndexOf("\"actions\"")
    if (marker < 0) return reply to emptyList()
    // walk back to the '{' that opens the block
    val start = reply.lastIndexOf('{', marker)
    if (start < 0) return reply to emptyList()
    // bracket-match forward to find its end
    var depth = 0
    var end = -1
    var inString = false
    var escaped = false
    for (i in start until reply.length) {
        val c = reply[i]
        when {
            escaped -> escaped = false
            c == '\\' && inString -> escaped = true
            c == '"' -> inString = !inString
            !inString && c == '{' -> depth++
            !inString && c == '}' -> {
                depth--
                if (depth == 0) {
                    end = i
                    break
                }
            }
        }
    }
    if (end < 0) return reply to emptyList()
    val block = reply.substring(start, end + 1)
    val parsed = runCatching { actionsJson.decodeFromString<AskActionsBlock>(block) }.getOrNull()
        ?: return reply to emptyList()
    // drop the block (and any code fence characters hugging it) from the visible text
    val stripped = (reply.substring(0, start) + reply.substring(end + 1))
        .replace("```json", "")
        .replace("```", "")
        .trim()
    return stripped to parsed.actions.filter { it.op.isNotBlank() }
}
