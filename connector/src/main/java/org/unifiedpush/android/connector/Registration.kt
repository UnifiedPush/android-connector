package org.unifiedpush.android.connector

import android.content.SharedPreferences
import org.unifiedpush.android.connector.keys.KeyManager
import java.util.UUID

internal class Registration(
    val instance: String,
    val token: String,
    var messageForDistributor: String?,
    var vapid: String?,
    ) {

    companion object {
        internal fun newOrUpdate(
            preferences: SharedPreferences,
            instance: String,
            messageForDistributor: String?,
            vapid: String?,
            keyManager: KeyManager
        ): Registration {
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
                }
                messageForDistributor?.let {
                    putString(PREF_CONNECTOR_MESSAGE.format(instance), it)
                }
                vapid?.let {
                    putString(PREF_CONNECTOR_VAPID.format(instance), it)
                }
                apply()
            }
            keyManager.run {
                if (!exists(instance)) generate(instance)
            }
            return Registration(instance, token!!, messageForDistributor, vapid)
        }
    }
}
