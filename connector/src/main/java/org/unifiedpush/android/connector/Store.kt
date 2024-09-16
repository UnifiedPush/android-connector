package org.unifiedpush.android.connector

import android.annotation.SuppressLint
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
            preferences.edit().putString(PREF_MASTER_DISTRIBUTOR, distributor).commit()
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
        @SuppressLint("ApplySharedPref")
        set(value) = synchronized(distributorLock) {
            preferences.edit().putBoolean(PREF_MASTER_DISTRIBUTOR_AND2, value).commit()
        }

    internal var distributorAck: Boolean
        get() = synchronized(distributorLock) {
            preferences.getBoolean(PREF_MASTER_DISTRIBUTOR_ACK, false)
        }
        @SuppressLint("ApplySharedPref")
        set(value) = synchronized(distributorLock) {
            preferences.edit().putBoolean(PREF_MASTER_DISTRIBUTOR_ACK, value).commit()
        }

    internal var tempToken: String?
        get() = preferences.getString(PREF_MASTER_TEMP_TOKEN, null)
        @SuppressLint("ApplySharedPref")
        set(value) {
            preferences.edit().putString(PREF_MASTER_TEMP_TOKEN, value).commit()
        }

    internal fun newTempToken(): String {
        return UUID.randomUUID().toString().also {
            tempToken = it
        }
    }

    internal var authToken: String?
        get() = preferences.getString(PREF_MASTER_AUTH, null)
        @SuppressLint("ApplySharedPref")
        set(value) {
            value?.let {
                preferences.edit().putString(PREF_MASTER_AUTH, value).commit()
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
            preferences.edit().putInt(PREF_MASTER_EVENT_COUNT, count +1).commit()
            return count
        }
    }

    companion object {
        private val distributorLock = Object()
        private val eventCountLock = Object()
        private lateinit var preferences: SharedPreferences
    }
}
