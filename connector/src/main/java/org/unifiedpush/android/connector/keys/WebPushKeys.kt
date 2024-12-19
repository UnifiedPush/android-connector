package org.unifiedpush.android.connector.keys

import org.unifiedpush.android.connector.data.PublicKeySet
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

/**
 * Contains Web Push keys information necessary to decrypt the messages.
 *
 * @param [auth] Shared secret with the application server, 16 random bytes.
 * @param [keyPair] P-256 Key Pair for use in ECDH.
 */
internal class WebPushKeys(
    val auth: ByteArray,
    val keyPair: KeyPair,
) {
    /** @return a [PublicKeySet] from the keys. */
    val publicKeySet: PublicKeySet
        get() =
            PublicKeySet(
                (keyPair.public as ECPublicKey).serialize(),
                auth.b64encode(),
            )

    companion object {
        /**
         * Generate a new key set.
         */
        fun new(): WebPushKeys {
            return WebPushKeys(
                keyPair = generateKeyPair(),
                auth = generateAuthSecret(),
            )
        }

        private fun generateKeyPair(): KeyPair {
            return KeyPairGenerator.getInstance("EC").apply {
                initialize(
                    ECGenParameterSpec("secp256r1"),
                )
            }.generateKeyPair()
        }

        private fun generateAuthSecret(): ByteArray {
            return ByteArray(16).apply {
                SecureRandom().nextBytes(this)
            }
        }
    }
}
