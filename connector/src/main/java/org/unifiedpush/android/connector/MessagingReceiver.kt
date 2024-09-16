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
        // For the linked request, we use a temporary token,
        // which is different from connection tokens
        if (intent.action == ACTION_LINKED
            && intent.getStringExtra(EXTRA_AUTH) == store.tempToken) {
            store.authToken = intent.getStringExtra(EXTRA_AUTH) ?: return
            store.distributorAck = true
            // TODO send registration for all registrationQueue elements
            return
        }
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
                val reason = intent.getStringExtra(EXTRA_REASON).toFailedReason()
                Log.i(TAG, "Failed: $reason")
                if (reason == FailedReason.UNAUTH) {
                    // What if REGISTER was send before LINKED is receive, but LINKED
                    // has been received since MESSAGE ?
                    // -> REGISTER is never send if Ack = false, it sends LINK first
                    // and add the registration to the queue
                    // -> The only reason why we could receive an UNAUTH is
                    // because we had Ack = true, but the distributor do not
                    // know the token anymore (eg. data has been wiped)
                    // -> but if there were many REGISTER intent, they were sent with Ack = true
                    // but in reality this is Ack = false
                    // => therefore, we NEED to know last LINK event and register event
                    // It must be monolithically increased, we can use an event count
                    // TODO, save add store.event_count
                    // if (store.event_n(token)>store.last_link_event_n) {
                    //     store.distributorAck = false
                    //     // No need to check if the distributor is still here, we just receive
                    //     // an intent with the right token
                    //     val distributor = store.tryGetDistributor(context) ?: return
                    //     broadcastLink(context, store, distributor)
                    // }
                    // TODO, add this registration to the registrationQueue if it exists
                    // or send registerApp()
                }
                onRegistrationFailed(context, reason, instance)
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
