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
            // Every registration that hasn't been acknowledge has been sent before LINKED was received
            store.registrationSet.forEachRegistration {
                if (!it.ack) {
                    UnifiedPush.registerApp(
                        context,
                        store,
                        it
                    )
                }
            }
            return
        }
        val instance = token?.let {
            store.registrationSet.tryGetInstance(it)
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
                store.registrationSet.ack(instance, true)
                onNewEndpoint(context, endpoint, instance)
                store.tryGetDistributor()?.let {
                    mayAcknowledgeMessage(context, it, id, token)
                }
            }
            ACTION_REGISTRATION_FAILED -> {
                val reason = intent.getStringExtra(EXTRA_REASON).toFailedReason()
                Log.i(TAG, "Failed: $reason")
                if (reason == FailedReason.UNAUTH) {
                    // What if REGISTER was send after LINK is sent and before LINKED is received ?
                    // -> REGISTER is never send if Ack = false. `registerApp` sends LINK first
                    // and save the registration. Once LINKED is received, it sends the REGISTER
                    // intent to the distributor.
                    // -> The only reason why we could receive an UNAUTH is
                    // because we had Ack = true, but the distributor did not
                    // know the token anymore (eg. its data has been wiped)
                    // -> but if there were many REGISTER intent, we had Ack = true, and we receive
                    // UNAUTH, so in reality this is Ack = false
                    // => therefore, we NEED to know last LINK event and register event
                    // It must be monolithically increased, we use an event count
                    store.registrationSet.ack(instance, false)
                    if (store.registrationSet.getEventCount(instance) > store.lastLinkRequest) {
                        // This registration request is more recent than the last LINK request,
                        // the distributor has probably been reinstalled
                        // we send a new LINK request
                        store.distributorAck = false
                        store.authToken = null
                        store.tryGetDistributor()?.let { distributor ->
                            UnifiedPush.broadcastLink(context, store, distributor)
                        }
                    } else if (store.distributorAck) {
                        // We have received the new LINKED response, we can register directly
                        store.registrationSet.tryGetRegistration(instance)?.let { reg ->
                            store.tryGetDistributor()?.let {
                                UnifiedPush.registerApp(context, store, reg)
                            }
                        }
                    }
                    // else, `registerApp` will be send during next LINKED
                } else {
                    store.registrationSet.removeInstance(instance)
                }
                onRegistrationFailed(context, reason, instance)
            }
            ACTION_UNREGISTERED -> {
                onUnregistered(context, instance)
                store.registrationSet.removeInstance(instance).ifEmpty {
                    store.removeDistributor()
                }
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
            val broadcastIntent = Intent().apply {
                `package` = distributor
                action = ACTION_MESSAGE_ACK
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_MESSAGE_ID, it)
            }
            context.sendBroadcast(broadcastIntent)
        }
    }
}
