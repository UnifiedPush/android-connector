package org.unifiedpush.android.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

open class MessagingReceiver : BroadcastReceiver() {

    open fun onNewEndpoint(context: Context, endpoint: String, instance: String) {}
    open fun onRegistrationFailed(context: Context, instance: String) {}
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
                onNewEndpoint(context, endpoint, instance)
            }
            // keep REFUSED for old distributors supporting AND_1
            ACTION_REGISTRATION_FAILED, ACTION_REGISTRATION_REFUSED -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "No reason supplied"
                Log.i(LOG_TAG, "Failed: $message")
                onRegistrationFailed(context, instance)
                store.removeInstance(instance)
            }
            ACTION_UNREGISTERED -> {
                onUnregistered(context, instance)
                store.removeInstance(instance, removeDistributor = true)
            }
            ACTION_MESSAGE -> {
                // keep EXTRA_MESSAGEv1 for AND_1
                val message = intent.getByteArrayExtra(EXTRA_BYTES_MESSAGE)
                    ?: intent.getStringExtra(EXTRA_MESSAGE)?.toByteArray() ?: return
                val id = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: ""
                onMessage(context, message, instance)
                store.tryGetDistributor()?.let {
                    acknowledgeMessage(context, it, id, token)
                }
            }
        }
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }

    private fun acknowledgeMessage(
        context: Context,
        distributor: String,
        id: String,
        token: String
    ) {
        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_MESSAGE_ACK
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_MESSAGE_ID, id)
        context.sendBroadcast(broadcastIntent)
    }
}
