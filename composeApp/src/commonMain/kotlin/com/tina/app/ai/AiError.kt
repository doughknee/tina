package com.tina.app.ai

/** Why a chat call failed, in the terms the user can act on. */
enum class AiError { OFF, NO_MODEL, METERED, INSECURE_ENDPOINT, UNAUTHORIZED, NOT_FOUND, RATE_LIMITED, SERVER, NETWORK, BAD_REPLY }

class AiException(val error: AiError, detail: String? = null) : RuntimeException(detail)

fun aiErrorFor(status: Int): AiError = when {
    status == 401 || status == 403 -> AiError.UNAUTHORIZED
    status == 404 -> AiError.NOT_FOUND
    status == 429 -> AiError.RATE_LIMITED
    status >= 500 -> AiError.SERVER
    else -> AiError.BAD_REPLY
}
