package org.unifiedpush.android.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

open class MessagingReceiver : BroadcastReceiver() {

    open fun onNewEndpoint(context: Context, endpoint: String, instance: String) {}
    open fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {}
    open fun onUnregistered(context: Context, instance: String) {}
    open fun onMessage(context: Context, message: ByteArray, instance: String) {}

    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.getStringExtra(EXTRA_TOKEN)
        val store = Store(context)
        val instance = token?.let {
            store.tryGetInstance(it)
        } ?: return
        val wakeLock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                acquire(60000L /*1min*/)
            }
        }
        when (intent.action) {
            ACTION_NEW_ENDPOINT -> {
                val endpoint = intent.getStringExtra(EXTRA_ENDPOINT) ?: return
                val id = intent.getStringExtra(EXTRA_MESSAGE_ID)
                store.distributorAck = true
                onNewEndpoint(context, endpoint, instance)
                store.tryGetDistributor()?.let {
                    mayAcknowledgeMessage(context, it, id, token)
                }
            }
            ACTION_REGISTRATION_FAILED -> {
                val reason = intent.getStringExtra(EXTRA_REASON)
                Log.i(LOG_TAG, "Failed: $reason")
                onRegistrationFailed(context, reason.toFailedReason(), instance)
                store.removeInstance(instance)
            }
            ACTION_UNREGISTERED -> {
                onUnregistered(context, instance)
                store.removeInstance(instance, removeDistributor = true)
            }
            ACTION_MESSAGE -> {
                val message = intent.getByteArrayExtra(EXTRA_BYTES_MESSAGE) ?: return
                val id = intent.getStringExtra(EXTRA_MESSAGE_ID)
                onMessage(context, message, instance)
                store.tryGetDistributor()?.let {
                    mayAcknowledgeMessage(context, it, id, token)
                }
            }
        }
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    private fun mayAcknowledgeMessage(
        context: Context,
        distributor: String,
        id: String?,
        token: String
    ) {
        id?.let {
            val broadcastIntent = Intent()
            broadcastIntent.`package` = distributor
            broadcastIntent.action = ACTION_MESSAGE_ACK
            broadcastIntent.putExtra(EXTRA_TOKEN, token)
            broadcastIntent.putExtra(EXTRA_MESSAGE_ID, it)
            context.sendBroadcast(broadcastIntent)
        }
    }
}
