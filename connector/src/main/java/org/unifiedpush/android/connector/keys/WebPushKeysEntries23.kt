package org.unifiedpush.android.connector.keys

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import org.unifiedpush.android.connector.PREF_CONNECTOR_AUTH
import org.unifiedpush.android.connector.PREF_CONNECTOR_IV
import org.unifiedpush.android.connector.PREF_CONNECTOR_PRIVKEY
import org.unifiedpush.android.connector.PREF_CONNECTOR_PUBKEY
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

@RequiresApi(23)
internal class WebPushKeysEntries23(private val instance: String, private val prefs: SharedPreferences):
    WebPushKeysEntries {

    override fun getWebPushKeys(): WebPushKeys? {
        val auth = prefs.getString(PREF_CONNECTOR_AUTH.format(instance), null)
            ?.b64decode()
            ?: return null
        val iv = prefs.getString(PREF_CONNECTOR_IV.format(instance), null)
            ?.b64decode()
            ?: return null
        val sealedPrivateKey = prefs.getString(PREF_CONNECTOR_PRIVKEY.format(instance), null)
            ?.b64decode()
            ?: return null
        val publicKey = prefs.getString(PREF_CONNECTOR_PUBKEY.format(instance), null)
            ?.deserializePubKey()
            ?: return null

        val cipher = getDecryptionCipher(iv)
        val privateBytes = cipher.doFinal(sealedPrivateKey)
        val privateKey = KeyFactory.getInstance("EC").generatePrivate(
            PKCS8EncodedKeySpec(privateBytes)
        )
        return WebPushKeys(auth, KeyPair(publicKey, privateKey))
    }

    override fun genWebPushKeys(): WebPushKeys {
        val keys = WebPushKeys.new()

        val cipher = getEncryptionCipher()
        val sealedPrivateKey = cipher.doFinal(keys.keyPair.private.encoded)

        prefs.edit()
            .putString(PREF_CONNECTOR_AUTH.format(instance), keys.auth.b64encode())
            .putString(PREF_CONNECTOR_IV.format(instance), cipher.iv.b64encode())
            .putString(PREF_CONNECTOR_PUBKEY.format(instance), (keys.keyPair.public as ECPublicKey).serialize())
            .putString(PREF_CONNECTOR_PRIVKEY.format(instance), sealedPrivateKey.b64encode())
            .apply()
        return keys
    }

    override fun hasWebPushKeys(): Boolean {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
        return prefs.contains(PREF_CONNECTOR_IV.format(instance)) &&
                prefs.contains(PREF_CONNECTOR_AUTH.format(instance)) &&
                prefs.contains(PREF_CONNECTOR_PUBKEY.format(instance)) &&
                prefs.contains(PREF_CONNECTOR_PRIVKEY.format(instance)) &&
                ks.containsAlias(ALIAS) && ks.entryInstanceOf(
                    ALIAS,
                    SecretKeyEntry::class.java
                )
    }

    override fun deleteWebPushKeys() {
        prefs.edit()
            .remove(PREF_CONNECTOR_AUTH.format(instance))
            .remove(PREF_CONNECTOR_IV.format(instance))
            .remove(PREF_CONNECTOR_PUBKEY.format(instance))
            .remove(PREF_CONNECTOR_PRIVKEY.format(instance))
            .apply()
    }

    private fun getEncryptionCipher(): Cipher {
        val aesKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()

        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                Cipher.ENCRYPT_MODE, aesKey
            )
        }
    }

    private fun getDecryptionCipher(iIV: ByteArray): Cipher {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
        val aesKey = ks.getEntry(ALIAS, null) as SecretKeyEntry

        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                Cipher.DECRYPT_MODE, aesKey.secretKey, GCMParameterSpec(128, iIV)
            )
        }
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val ALIAS = "UnifiedPush"
    }
}