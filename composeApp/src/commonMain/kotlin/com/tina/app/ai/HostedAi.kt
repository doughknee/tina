package com.tina.app.ai

import com.tina.app.pro.Entitlement
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Hosted AI: the Anthropic client pointed at the Peggy relay, with the Play purchase token
 * where the API key goes. The relay (relay/README.md) checks the purchase, pins the model per
 * route and keeps the real key. No model or key is ever configured on the phone for this.
 */
const val HOSTED_RELAY_URL = "https://relay.doughknee.com"

/** The model field is required by the wire format; the relay replaces it per route. */
const val HOSTED_MODEL_PLACEHOLDER = "relay"

/** What the relay needs to know who is asking and for what. Null when the caller is not Pro. */
fun relayHeaders(entitlement: Entitlement, route: String): Map<String, String>? {
    val pro = entitlement as? Entitlement.Pro ?: return null
    if (pro.token.isBlank()) return null
    return mapOf(
        "x-api-key" to pro.token,
        "x-peggy-product" to pro.plan.productId,
        "x-peggy-route" to route,
    )
}

/** This month's use of hosted AI, as the relay counts it: Ask turns, and parse + improve calls. */
data class HostedQuota(val askUsed: Int, val askLimit: Int, val lightUsed: Int, val lightLimit: Int)

private val quotaJson = Json { ignoreUnknownKeys = true }

/** Null when not Pro, offline, or the relay is unhappy; the meter simply stays hidden then. */
suspend fun fetchHostedQuota(http: HttpClient, entitlement: Entitlement): HostedQuota? {
    val headers = relayHeaders(entitlement, "ask") ?: return null
    return runCatching {
        val response = http.get("$HOSTED_RELAY_URL/v1/entitlement") {
            headers.forEach { (k, v) -> header(k, v) }
        }
        if (!response.status.isSuccess()) return null
        val quota = quotaJson.parseToJsonElement(response.bodyAsText()).jsonObject["quota"]!!.jsonObject
        fun n(bucket: String, field: String) = quota[bucket]!!.jsonObject[field]!!.jsonPrimitive.int
        HostedQuota(n("ask", "used"), n("ask", "limit"), n("light", "used"), n("light", "limit"))
    }.getOrNull()
}
