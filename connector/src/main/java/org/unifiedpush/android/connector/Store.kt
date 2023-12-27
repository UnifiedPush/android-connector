package org.unifiedpush.android.connector

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

internal class Store(context: Context) {

    init {
        preferences = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
    }

    internal fun tryGetToken(instance: String): String? {
        return synchronized(tokenLock) {
            preferences.getString("$instance/$PREF_MASTER_TOKEN", null)
        }
    }

    @SuppressLint("MutatingSharedPrefs", "ApplySharedPref")
    internal fun getTokenOrNew(instance: String): String {
        return synchronized(tokenLock) {
            preferences.getString("$instance/$PREF_MASTER_TOKEN", null) ?: run {
                val token = UUID.randomUUID().toString()
                synchronized(instancesLock) {
                    val instances = preferences.getStringSet(PREF_MASTER_INSTANCE, null)
                        ?: emptySet<String>().toMutableSet()
                    if (!instances.contains(instance)) {
                        instances.add(instance)
                    }
                    preferences.edit().putStringSet(PREF_MASTER_INSTANCE, instances).commit()
                }
                preferences.edit().putString("$instance/$PREF_MASTER_TOKEN", token).commit()
                token
            }
        }
    }

    internal fun tryGetInstance(token: String): String? {
        getInstances().forEach {
            if (tryGetToken(it).equals(token)) {
                return it
            }
        }
        return null
    }

    internal fun getInstances(): Set<String> {
        return synchronized(instancesLock) {
            preferences.getStringSet(PREF_MASTER_INSTANCE, null)
                ?: emptySet()
        }
    }

    @SuppressLint("MutatingSharedPrefs", "ApplySharedPref")
    internal fun removeInstance(instance: String, removeDistributor: Boolean = false) {
        synchronized(instancesLock) {
            val instances = preferences.getStringSet(PREF_MASTER_INSTANCE, null)
                ?: emptySet<String>().toMutableSet()
            instances.remove(instance)
            preferences.edit().putStringSet(PREF_MASTER_INSTANCE, instances).commit()
            synchronized(tokenLock) {
                preferences.edit().remove("$instance/$PREF_MASTER_TOKEN").commit()
            }
            if (removeDistributor && instances.isEmpty()) {
                removeDistributor()
            }
        }
    }

    internal fun removeInstances() {
        synchronized(instancesLock) {
            preferences.edit().remove(PREF_MASTER_INSTANCE).commit()
        }
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
            preferences.edit().apply {
                remove(PREF_MASTER_DISTRIBUTOR)
                remove(PREF_MASTER_DISTRIBUTOR_ACK)
            }.commit()
        }
    }

    internal var distributorAck: Boolean
        get() = synchronized(distributorLock) {
            preferences.getBoolean(PREF_MASTER_DISTRIBUTOR_ACK, false)
        }
        @SuppressLint("ApplySharedPref")
        set(value) = synchronized(distributorLock) {
            preferences.edit().putBoolean(PREF_MASTER_DISTRIBUTOR_ACK, value).commit()
        }


    internal fun saveNoDistributorAck() {
        preferences.edit().putBoolean(PREF_MASTER_NO_DISTRIB_DIALOG_ACK, true).apply()
    }

    internal fun getNoDistributorAck(): Boolean {
        return preferences.getBoolean(PREF_MASTER_NO_DISTRIB_DIALOG_ACK, false)
    }

    internal fun removeNoDistributorAck() {
        preferences.edit().remove(PREF_MASTER_NO_DISTRIB_DIALOG_ACK).apply()
    }

    companion object {
        private val tokenLock = Object()
        private val instancesLock = Object()
        private val distributorLock = Object()
        private lateinit var preferences: SharedPreferences
    }
}
