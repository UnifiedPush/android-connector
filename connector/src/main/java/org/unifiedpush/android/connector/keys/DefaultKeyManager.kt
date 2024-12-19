package org.unifiedpush.android.connector.keys

import android.content.Context
import android.os.Build
import com.google.crypto.tink.apps.webpush.WebPushHybridDecrypt
import org.unifiedpush.android.connector.PREF_MASTER
import org.unifiedpush.android.connector.data.PublicKeySet
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

/**
 * Default [KeyManager].
 *
 * For SDK >= 23, private keys are stored encrypted in shared preferences using AES-GCM, and a random
 * key in the Android Key Store.
 *
 * For SDK < 23, private keys are stored in plain text in shared preferences.
 */
class DefaultKeyManager(context: Context) : KeyManager {
    private val preferences = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)

    override fun decrypt(
        instance: String,
        sealed: ByteArray,
    ): ByteArray? {
        val keys = getKeyStoreEntries(instance).getOrNullWebPushKeys() ?: return null
        val hybridDecrypt =
            WebPushHybridDecrypt.Builder()
                .withAuthSecret(keys.auth)
                .withRecipientPublicKey(keys.keyPair.public as ECPublicKey)
                .withRecipientPrivateKey(keys.keyPair.private as ECPrivateKey)
                .build()
        return hybridDecrypt.decrypt(sealed, null)
    }

    override fun generate(instance: String) {
        getKeyStoreEntries(instance).genWebPushKeys()
    }

    override fun getPublicKeySet(instance: String): PublicKeySet? {
        return getKeyStoreEntries(instance).getOrNullWebPushKeys()?.publicKeySet
    }

    override fun exists(instance: String): Boolean {
        return getKeyStoreEntries(instance).getOrNullWebPushKeys() != null
    }

    override fun delete(instance: String) {
        getKeyStoreEntries(instance).deleteWebPushKeys()
    }

    private fun getKeyStoreEntries(instance: String): WebPushKeysEntries {
        return if (Build.VERSION.SDK_INT >= 23) {
            WebPushKeysEntries23(instance, preferences)
        } else {
            WebPushKeysEntriesLegacy(instance, preferences)
        }
    }
}
