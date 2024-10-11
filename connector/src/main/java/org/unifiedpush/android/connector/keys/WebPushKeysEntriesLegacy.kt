package org.unifiedpush.android.connector.keys

import android.content.SharedPreferences
import org.unifiedpush.android.connector.PREF_CONNECTOR_AUTH
import org.unifiedpush.android.connector.PREF_CONNECTOR_PRIVKEY
import org.unifiedpush.android.connector.PREF_CONNECTOR_PUBKEY
import java.security.KeyFactory
import java.security.KeyPair
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec

internal class WebPushKeysEntriesLegacy(private val instance: String, private val prefs: SharedPreferences):
    WebPushKeysEntries {
    override fun getWebPushKeys(): WebPushKeys? {
        val auth = prefs.getString(PREF_CONNECTOR_AUTH.format(instance), null)
            ?.b64decode()
            ?: return null
        val privateBytes = prefs.getString(PREF_CONNECTOR_PRIVKEY.format(instance), null)
            ?.b64decode()
            ?: return null
        val publicKey = prefs.getString(PREF_CONNECTOR_PUBKEY.format(instance), null)
            ?.deserializePubKey()
            ?: return null

        val privateKey = KeyFactory.getInstance("EC").generatePrivate(
            PKCS8EncodedKeySpec(privateBytes)
        )
        return WebPushKeys(auth, KeyPair(publicKey, privateKey))
    }

    override fun genWebPushKeys(): WebPushKeys {
        val keys = WebPushKeys.new()

        prefs.edit()
            .putString(PREF_CONNECTOR_AUTH.format(instance), keys.auth.b64encode())
            .putString(PREF_CONNECTOR_PUBKEY.format(instance), (keys.keyPair.public as ECPublicKey).serialize())
            .putString(PREF_CONNECTOR_PRIVKEY.format(instance), keys.keyPair.private.encoded.b64encode())
            .apply()
        return keys
    }

    override fun hasWebPushKeys(): Boolean {
        return prefs.contains(PREF_CONNECTOR_AUTH.format(instance)) &&
                prefs.contains(PREF_CONNECTOR_PUBKEY.format(instance)) &&
                prefs.contains(PREF_CONNECTOR_PRIVKEY.format(instance))
    }

    override fun deleteWebPushKeys() {
        prefs.edit()
            .remove(PREF_CONNECTOR_AUTH.format(instance))
            .remove(PREF_CONNECTOR_PUBKEY.format(instance))
            .remove(PREF_CONNECTOR_PRIVKEY.format(instance))
            .apply()
    }
}