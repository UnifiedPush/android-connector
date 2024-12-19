package org.unifiedpush.android.connector.internal

import android.content.Context
import android.content.SharedPreferences
import org.unifiedpush.android.connector.PREF_MASTER
import org.unifiedpush.android.connector.PREF_MASTER_DISTRIBUTOR
import org.unifiedpush.android.connector.PREF_MASTER_DISTRIBUTOR_ACK

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
                .apply()
        }
    }

    internal var distributorAck: Boolean
        get() =
            synchronized(distributorLock) {
                preferences.getBoolean(PREF_MASTER_DISTRIBUTOR_ACK, false)
            }
        set(value) =
            synchronized(distributorLock) {
                preferences.edit().putBoolean(PREF_MASTER_DISTRIBUTOR_ACK, value).apply()
            }

    companion object {
        private val distributorLock = Object()
        private lateinit var preferences: SharedPreferences
    }
}
