package com.tina.app.ai

import com.tina.app.pro.Entitlement

/**
 * Hosted AI: the Anthropic client pointed at the Peggy relay, with the Play purchase token
 * where the API key goes. The relay (relay/README.md) checks the purchase, pins the model per
 * route and keeps the real key. No model or key is ever configured on the phone for this.
 */
const val HOSTED_RELAY_URL = "https://peggy-relay.doughknee.workers.dev"

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
