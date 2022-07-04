package org.unifiedpush.android.connector

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

internal class Store (context: Context) {

    init {
        preferences = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
    }

    internal fun newToken(instance: String): String {
        val token = UUID.randomUUID().toString()
        saveToken(token, instance)
        return token
    }

    @SuppressLint("MutatingSharedPrefs")
    private fun saveToken(token: String, instance: String) {
        val instances = preferences.getStringSet(PREF_MASTER_INSTANCE, null)
            ?: emptySet<String>().toMutableSet()
        if (!instances.contains(instance)) {
            instances.add(instance)
        }
        preferences.edit().putStringSet(PREF_MASTER_INSTANCE, instances).apply()
        preferences.edit().putString("$instance/$PREF_MASTER_TOKEN", token).apply()
    }

    internal fun getToken(instance: String): String? {
        return preferences.getString("$instance/$PREF_MASTER_TOKEN", null)
    }

    internal fun getInstance(token: String): String? {
        getInstances().forEach {
            if (getToken(it).equals(token)) {
                return it
            }
        }
        return null
    }

    internal fun getInstances(): Set<String> {
        return preferences.getStringSet(PREF_MASTER_INSTANCE, null)
            ?: emptySet()
    }

    @SuppressLint("MutatingSharedPrefs")
    internal fun removeInstance(instance: String) {
        val instances = preferences.getStringSet(PREF_MASTER_INSTANCE, null)
            ?: emptySet<String>().toMutableSet()
        instances.remove(instance)
        preferences.edit().putStringSet(PREF_MASTER_INSTANCE, instances).apply()
        preferences.edit().remove("$instance/$PREF_MASTER_TOKEN").apply()
    }

    internal fun removeInstances() {
        preferences.edit().remove(PREF_MASTER_INSTANCE).apply()
    }

    internal fun saveDistributor(distributor: String) {
        preferences.edit().putString(PREF_MASTER_DISTRIBUTOR, distributor).apply()
    }

    internal fun getDistributor(): String? {
        return preferences.getString(PREF_MASTER_DISTRIBUTOR, null)
    }

    internal fun removeDistributor() {
        preferences.edit().remove(PREF_MASTER_DISTRIBUTOR).apply()
    }

    companion object {
        private lateinit var preferences: SharedPreferences
    }
}