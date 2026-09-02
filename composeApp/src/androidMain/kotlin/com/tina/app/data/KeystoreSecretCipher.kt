package com.tina.app.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE = "AndroidKeyStore"
private const val ALIAS = "tina-secrets"
private const val PREFIX = "enc1:"

/**
 * AES-GCM under a key that lives in the hardware-backed keystore, so the stored value is
 * useless off this device (and is excluded from cloud backup anyway; see backup_rules.xml).
 */
class KeystoreSecretCipher : SecretCipher {
    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    override fun encrypt(plain: String): String {
        if (plain.isEmpty()) return ""
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
            val bytes = cipher.doFinal(plain.toByteArray())
            PREFIX + Base64.encodeToString(cipher.iv + bytes, Base64.NO_WRAP)
        }.getOrDefault(plain) // a broken keystore must not lose the key: fall back to plaintext
    }

    override fun decrypt(stored: String): String {
        if (!stored.startsWith(PREFIX)) return stored // written before encryption existed
        return runCatching {
            val all = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, all, 0, 12))
            String(cipher.doFinal(all, 12, all.size - 12))
        }.getOrDefault("")
    }
}
