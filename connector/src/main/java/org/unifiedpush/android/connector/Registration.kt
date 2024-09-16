package org.unifiedpush.android.connector

import android.content.SharedPreferences
import java.util.UUID

internal class Registration(
    val instance: String,
    val token: String,
    var messageForDistributor: String?,
    var vapid: String?,
    val eventCount: Int,
    val ack: Boolean
    ) {

    companion object {
        fun tryGetFromInstance(preferences: SharedPreferences, instance: String): Registration? {
            val instances = preferences.getStringSet(PREF_MASTER_INSTANCES, null)
                ?: emptySet<String>().toMutableSet()
            if (!instances.contains(instance)) {
                return null
            }
            val token = preferences.getString(PREF_CONNECTOR_TOKEN.format(instance), null) ?: return null
            val message = preferences.getString(PREF_CONNECTOR_MESSAGE.format(instance), null)
            val vapid = preferences.getString(PREF_CONNECTOR_VAPID.format(instance), null)
            val eventCount = preferences.getInt(PREF_CONNECTOR_EVENT_COUNT.format(instance), -1)
            val ack = preferences.getBoolean(PREF_CONNECTOR_ACK.format(instance), false)
            return Registration(instance, token, message, vapid, eventCount, ack)
        }

        fun newOrUpdate(preferences: SharedPreferences, instance: String, messageForDistributor: String?, vapid: String?, eventCount: Int): Registration {
            val instances = preferences.getStringSet(PREF_MASTER_INSTANCES, null)
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
            return Registration(instance, token!!, messageForDistributor, vapid, eventCount, false)
        }
    }
}
