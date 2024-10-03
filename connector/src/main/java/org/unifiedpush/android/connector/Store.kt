package org.unifiedpush.android.connector

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

internal class Store(context: Context) {

    var registrationSet: RegistrationSet

    init {
        preferences = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        registrationSet = RegistrationSet(preferences)
    }

    internal fun saveDistributor(distributor: String) {
        synchronized(distributorLock) {
            preferences.edit().putString(PREF_MASTER_DISTRIBUTOR, distributor).apply()
        }
    }

    internal fun tryGetDistributor(): String? {
        return synchronized(distributorLock) {
            preferences.getString(PREF_MASTER_DISTRIBUTOR, null)
        }
    }

    internal fun removeDistributor() {
        synchronized(distributorLock) {
            preferences.edit()
                .remove(PREF_MASTER_DISTRIBUTOR)
                .remove(PREF_MASTER_DISTRIBUTOR_ACK)
                .remove(PREF_MASTER_DISTRIBUTOR_AND2)
                .remove(PREF_MASTER_AUTH)
                .apply()
        }
    }

    // If the distributor doesn't support LINK/ED actions
    // it's still following AND_2
    internal var legacyDistributor: Boolean
        get() = synchronized(distributorLock) {
            preferences.getBoolean(PREF_MASTER_DISTRIBUTOR_AND2, false)
        }
        set(value) = synchronized(distributorLock) {
            preferences.edit().putBoolean(PREF_MASTER_DISTRIBUTOR_AND2, value).apply()
        }

    internal var distributorAck: Boolean
        get() = synchronized(distributorLock) {
            preferences.getBoolean(PREF_MASTER_DISTRIBUTOR_ACK, false)
        }
        set(value) = synchronized(distributorLock) {
            preferences.edit().putBoolean(PREF_MASTER_DISTRIBUTOR_ACK, value).apply()
        }

    internal var linkToken: String?
        get() = preferences.getString(PREF_MASTER_TEMP_TOKEN, null)
        set(value) {
            preferences.edit().putString(PREF_MASTER_TEMP_TOKEN, value).apply()
        }

    internal fun newLinkToken(): String {
        return UUID.randomUUID().toString().also {
            linkToken = it
        }
    }

    internal var authToken: String?
        get() = preferences.getString(PREF_MASTER_AUTH, null)
        set(value) {
            value?.let {
                preferences.edit().putString(PREF_MASTER_AUTH, value).apply()
            } ?: run {
                preferences.edit().remove(PREF_MASTER_AUTH)
            }
        }

    internal var lastLinkRequest: Int
        get() = preferences.getInt(PREF_MASTER_LAST_LINK, 0)
        set(value) {
            preferences.edit().putInt(PREF_MASTER_LAST_LINK, value)
        }

    internal fun getEventCountAndIncrement(): Int {
        synchronized(eventCountLock) {
            val count = preferences.getInt(PREF_MASTER_EVENT_COUNT, 0)
            preferences.edit().putInt(PREF_MASTER_EVENT_COUNT, count +1).apply()
            return count
        }
    }

    companion object {
        private val distributorLock = Object()
        private val eventCountLock = Object()
        private lateinit var preferences: SharedPreferences
    }
}
