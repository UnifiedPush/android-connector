package org.unifiedpush.android.connector

import android.content.SharedPreferences
import java.util.UUID

internal class Registration(
    val instance: String,
    val token: String,
    var messageForDistributor: String?,
    var vapid: String?,
    val webPushKeys: WebPushKeys?,
    val ack: Boolean,
    val eventCount: Int,
    ) {

    companion object {
        internal fun tryGetFromInstance(preferences: SharedPreferences, instance: String): Registration? {
            val instances = preferences.getStringSet(PREF_MASTER_INSTANCES, null)
                ?: emptySet<String>().toMutableSet()
            if (!instances.contains(instance)) {
                return null
            }
            val token = preferences.getString(PREF_CONNECTOR_TOKEN.format(instance), null) ?: return null
            val message = preferences.getString(PREF_CONNECTOR_MESSAGE.format(instance), null)
            val vapid = preferences.getString(PREF_CONNECTOR_VAPID.format(instance), null)
            val eventCount = preferences.getInt(PREF_CONNECTOR_EVENT_COUNT.format(instance), -1)
            val webPushKeys = tryGetWebPushKeys(preferences, instance)
            val ack = preferences.getBoolean(PREF_CONNECTOR_ACK.format(instance), false)
            return Registration(instance, token, message, vapid, webPushKeys, ack, eventCount)
        }

        internal fun tryGetWebPushKeys(preferences: SharedPreferences, instance: String): WebPushKeys? {
            val publicKey = preferences.getString(PREF_CONNECTOR_PUBKEY.format(instance), null)
            val privateKey = preferences.getString(PREF_CONNECTOR_PRIVKEY.format(instance), null)
            val auth = preferences.getString(PREF_CONNECTOR_AUTH.format(instance), null)?.b64decode()
            return if (publicKey.isNullOrBlank() || privateKey.isNullOrBlank() || auth == null) {
                null
            } else {
                WebPushKeys (
                    SerializedKeyPair(privateKey, publicKey).deserialize(),
                    auth
                )
            }
        }

        private fun newWebPushKeys(preferences: SharedPreferences, instance: String): WebPushKeys {
            val keyPair = WebPush.generateKeyPair()
            val encodedKeyPair = keyPair.serialize()
            val auth = WebPush.generateAuthSecret()
            preferences.edit()
                .putString(PREF_CONNECTOR_PUBKEY.format(instance), encodedKeyPair.public)
                .putString(PREF_CONNECTOR_PRIVKEY.format(instance), encodedKeyPair.private)
                .putString(PREF_CONNECTOR_AUTH.format(instance), auth.b64encode())
                .apply()
            return WebPushKeys(
                keyPair,
                auth
            )
        }

        internal fun newOrUpdate(preferences: SharedPreferences, instance: String, messageForDistributor: String?, vapid: String?, encrypted: Boolean, eventCount: Int): Registration {
            val instances = preferences.getStringSet(PREF_MASTER_INSTANCES, null)?.toMutableSet()
                ?: emptySet<String>().toMutableSet()
            var token = preferences.getString(PREF_CONNECTOR_TOKEN.format(instance), null)
            if (!instances.contains(instance)) {
                token = null
                instances.add(instance)
            }
            preferences.edit().apply {
                putStringSet(PREF_MASTER_INSTANCES, instances)
                if (token == null) {
                    token = UUID.randomUUID().toString()
                    putString(PREF_CONNECTOR_TOKEN.format(instance), token)
                    putBoolean(PREF_CONNECTOR_ACK.format(instance), false)
                }
                messageForDistributor?.let {
                    putString(PREF_CONNECTOR_MESSAGE.format(instance), it)
                }
                vapid?.let {
                    putString(PREF_CONNECTOR_VAPID.format(instance), it)
                }
                putInt(PREF_CONNECTOR_EVENT_COUNT.format(instance), eventCount)
                apply()
            }
            val webPushKeys = tryGetWebPushKeys(preferences, instance) ?: newWebPushKeys(preferences, instance)
            return Registration(instance, token!!, messageForDistributor, vapid, webPushKeys, false, eventCount)
        }
    }
}
