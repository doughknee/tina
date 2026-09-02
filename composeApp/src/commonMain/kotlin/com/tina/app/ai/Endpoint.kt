package com.tina.app.ai

/**
 * Cleartext is allowed only towards hosts that cannot be on the public internet: a LAN
 * Ollama. Anything else must be https, or the key and the whole Ask context would travel
 * in the clear. Checked where requests are built, so a hand-typed base URL cannot bypass it.
 */
fun isAllowedAiEndpoint(baseUrl: String): Boolean {
    val url = baseUrl.trim().lowercase()
    if (url.startsWith("https://")) return true
    if (!url.startsWith("http://")) return false
    val host = url.removePrefix("http://").substringBefore('/').substringBefore(':').removePrefix("[").removeSuffix("]")
    return isPrivateHost(host)
}

fun isPrivateHost(host: String): Boolean {
    if (host == "localhost" || host.endsWith(".local") || host.endsWith(".lan") || host.endsWith(".home.arpa")) return true
    if (host == "::1" || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80")) return true
    val parts = host.split('.')
    if (parts.size != 4 || parts.any { it.toIntOrNull() !in 0..255 }) return false
    val a = parts[0].toInt()
    val b = parts[1].toInt()
    return a == 10 || a == 127 || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 169 && b == 254)
}
