package org.unifiedpush.android.connector

import android.content.SharedPreferences

class RegistrationSet(private val preferences: SharedPreferences) {

    internal fun tryGetToken(instance: String): String? {
        return synchronized(registrationLock) {
            preferences.getString(PREF_CONNECTOR_TOKEN.format(instance), null)
        }
    }

    internal fun tryGetRegistration(instance: String): Registration? {
        return synchronized(registrationLock) {
            Registration.tryGetFromInstance(preferences, instance)
        }
    }

    internal fun tryGetWebPushKeys(instance: String): WebPushKeys? {
        return synchronized(registrationLock) {
            Registration.tryGetWebPushKeys(preferences, instance)
        }
    }

    internal fun tryGetPublicKeySet(instance: String): PublicKeySet? {
        return synchronized(registrationLock) {
            Registration.tryGetWebPushKeys(preferences, instance)?.publicKeySet
        }
    }

    internal fun getEventCount(instance: String): Int {
        return synchronized(registrationLock) {
            preferences.getInt(PREF_CONNECTOR_EVENT_COUNT.format(instance), -1)
        }
    }

    internal fun ack(instance: String, ack: Boolean) {
        return synchronized(registrationLock) {
            preferences.edit().putBoolean(PREF_CONNECTOR_ACK.format(instance), ack).apply()
        }
    }

    internal fun setEventCount(instance: String, count: Int) {
        return synchronized(registrationLock) {
            preferences.edit().putInt(PREF_CONNECTOR_EVENT_COUNT.format(instance), count).apply()
        }
    }
    
    internal fun newOrUpdate(
        instance: String,
        messageForDistributor: String?,
        vapid: String?,
        encrypted: Boolean,
        eventCount: Int
    ): Registration {
        return synchronized(registrationLock) {
            Registration.newOrUpdate(preferences, instance, messageForDistributor, vapid, encrypted, eventCount)
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

    internal fun isAnyInstance(): Boolean {
        return synchronized(registrationLock) {
            preferences.getStringSet(PREF_MASTER_INSTANCES, null) != null
        }
    }

    /**
     * Remove instance and return the updated set of instances
     */
    internal fun removeInstance(instance: String): Set<String> {
        synchronized(registrationLock) {
            val instances = preferences.getStringSet(PREF_MASTER_INSTANCES, null)?.toMutableSet()
                ?: emptySet<String>().toMutableSet()
            instances.remove(instance)
            preferences.edit()
                .putStringSet(PREF_MASTER_INSTANCES, instances)
                .remove(PREF_CONNECTOR_TOKEN.format(instance))
                .remove(PREF_CONNECTOR_VAPID.format(instance))
                .remove(PREF_CONNECTOR_MESSAGE.format(instance))
                .remove(PREF_CONNECTOR_ACK.format(instance))
                .remove(PREF_CONNECTOR_PUBKEY.format(instance))
                .remove(PREF_CONNECTOR_PRIVKEY.format(instance))
                .remove(PREF_CONNECTOR_AUTH.format(instance))
                .remove(PREF_CONNECTOR_EVENT_COUNT.format(instance))
                .apply()
            return instances
        }
    }

    internal fun removeInstances() {
        synchronized(registrationLock) {
            preferences.getStringSet(PREF_MASTER_INSTANCES, null)?.forEach { instance ->
                preferences.edit()
                    .remove(PREF_CONNECTOR_TOKEN.format(instance))
                    .remove(PREF_CONNECTOR_VAPID.format(instance))
                    .remove(PREF_CONNECTOR_MESSAGE.format(instance))
                    .remove(PREF_CONNECTOR_PUBKEY.format(instance))
                    .remove(PREF_CONNECTOR_PRIVKEY.format(instance))
                    .remove(PREF_CONNECTOR_AUTH.format(instance))
                    .remove(PREF_CONNECTOR_ACK.format(instance))
                    .remove(PREF_CONNECTOR_EVENT_COUNT.format(instance))
                    .apply()
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

    internal fun forEachRegistration(block: (registration: Registration) -> Unit) {
        forEachInstance { instance ->
            Registration.tryGetFromInstance(preferences, instance)?.let {
                block(it)
            }
        }
    }

    companion object {
        private val registrationLock = Object()
    }
}