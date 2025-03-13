package org.unifiedpush.android.connector.internal

import android.content.SharedPreferences
import org.unifiedpush.android.connector.PREF_CONNECTOR_AUTH
import org.unifiedpush.android.connector.PREF_CONNECTOR_IV
import org.unifiedpush.android.connector.PREF_CONNECTOR_MESSAGE
import org.unifiedpush.android.connector.PREF_CONNECTOR_PRIVKEY
import org.unifiedpush.android.connector.PREF_CONNECTOR_PUBKEY
import org.unifiedpush.android.connector.PREF_CONNECTOR_TOKEN
import org.unifiedpush.android.connector.PREF_CONNECTOR_VAPID
import org.unifiedpush.android.connector.PREF_MASTER_INSTANCES
import org.unifiedpush.android.connector.keys.KeyManager

/**
 * Internal Set of [Registration]
 */
internal class RegistrationSet(private val preferences: SharedPreferences) {
    internal fun tryGetToken(instance: String): String? {
        return synchronized(registrationLock) {
            preferences.getString(PREF_CONNECTOR_TOKEN.format(instance), null)
        }
    }

    internal fun newOrUpdate(
        instance: String,
        messageForDistributor: String?,
        vapid: String?,
        keyManager: KeyManager,
    ): Registration {
        return synchronized(registrationLock) {
            Registration.newOrUpdate(
                preferences,
                instance,
                messageForDistributor,
                vapid,
                keyManager
            )
        }
    }

    internal fun tryGetInstance(token: String): String? {
        synchronized(registrationLock) {
            preferences.getStringSet(PREF_MASTER_INSTANCES, null)?.forEach {
                if (tryGetToken(it).equals(token)) {
                    return it
                }
            }
        }
        return null
    }

    /**
     * Remove instance and return the updated set of instances
     */
    internal fun removeInstance(
        instance: String,
        keyManager: KeyManager,
    ): Set<String> {
        synchronized(registrationLock) {
            val instances =
                preferences.getStringSet(PREF_MASTER_INSTANCES, null)?.toMutableSet()
                    ?: emptySet<String>().toMutableSet()
            instances.remove(instance)
            preferences.edit()
                .putStringSet(PREF_MASTER_INSTANCES, instances)
                .remove(PREF_CONNECTOR_TOKEN.format(instance))
                .remove(PREF_CONNECTOR_VAPID.format(instance))
                .remove(PREF_CONNECTOR_MESSAGE.format(instance))
                .apply()
            keyManager.delete(instance)
            return instances
        }
    }

    internal fun removeInstances(keyManager: KeyManager) {
        synchronized(registrationLock) {
            preferences.getStringSet(PREF_MASTER_INSTANCES, null)?.forEach { instance ->
                preferences.edit()
                    .remove(PREF_CONNECTOR_TOKEN.format(instance))
                    .remove(PREF_CONNECTOR_VAPID.format(instance))
                    .remove(PREF_CONNECTOR_MESSAGE.format(instance))
                    .apply()
                keyManager.delete(instance)
            }
            preferences.edit().remove(PREF_MASTER_INSTANCES).apply()
        }
    }

    internal fun forEachInstance(block: (instance: String) -> Unit) {
        synchronized(registrationLock) {
            preferences.getStringSet(PREF_MASTER_INSTANCES, null)?.forEach {
                block(it)
            }
        }
    }

    companion object {
        private val registrationLock = Object()
    }
}
