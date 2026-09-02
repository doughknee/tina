package com.tina.app.data

/**
 * Wraps secrets (the AI API key) before they hit DataStore. Values carry a version prefix
 * so a plaintext value written by an older build still reads; it is re-wrapped on next save.
 */
interface SecretCipher {
    fun encrypt(plain: String): String
    fun decrypt(stored: String): String
}

/** Desktop and tests: no keystore, so the value is stored as-is. */
object PlainSecretCipher : SecretCipher {
    override fun encrypt(plain: String): String = plain
    override fun decrypt(stored: String): String = stored
}
