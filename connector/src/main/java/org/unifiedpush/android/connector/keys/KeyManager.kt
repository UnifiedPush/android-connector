package org.unifiedpush.android.connector.keys

import org.unifiedpush.android.connector.data.PublicKeySet

/**
 * Defines functions the key manager must expose.
 */
interface KeyManager {
    /**
     * Decrypt [sealed] with [instance]'s keys.
     *
     * @return clear text or `null` if decryption failed.
     */
    fun decrypt(instance: String, sealed: ByteArray): ByteArray?

    /**
     * Generate a new key pair and auth secret for [instance].
     */
    fun generate(instance: String)

    /**
     * Get [PublicKeySet] for [instance]. Encoded to be shared to the application server.
     *
     * @return [PublicKeySet] if possible, `null` if the key set doesn't exist or isn't valid.
     */
    fun getPublicKeySet(instance: String): PublicKeySet?

    /**
     * Check if the key set for [instance] exists and is valid.
     */
    fun exists(instance: String): Boolean

    /**
     * Delete key set for [instance].
     */
    fun delete(instance: String)
}