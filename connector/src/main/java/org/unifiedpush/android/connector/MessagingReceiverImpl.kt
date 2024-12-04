package org.unifiedpush.android.connector

import android.content.Context
import android.content.Intent
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/**
 * @hide
 *
 * Implementation of [MessagingReceiver] to receive UnifiedPush events and
 * forward to [PushReceiver]. Runs only if there isn't another implementation
 * of [MessagingReceiver] (with priority>-500) declared
 */
class MessagingReceiverImpl : MessagingReceiver() {
    override fun onUnregistered(context: Context, instance: String) {
        sendToService(context, instance, PushReceiver.PushEventType.UNREGISTERED)
    }

    override fun onMessage(context: Context, message: PushMessage, instance: String) {
        sendToService(context, instance, PushReceiver.PushEventType.MESSAGE) { intent ->
            intent.putExtra("message", message)
        }
    }

    override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
        sendToService(context, instance, PushReceiver.PushEventType.NEW_ENDPOINT) { intent ->
            intent.putExtra("endpoint", endpoint)
        }
    }

    override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
        sendToService(context, instance, PushReceiver.PushEventType.NEW_ENDPOINT) { intent ->
            intent.putExtra("reason", reason)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (shouldRun(context)) {
            super.onReceive(context, intent)
        }
    }

    private fun sendToService(context: Context, instance: String, type: PushReceiver.PushEventType, processIntent: (Intent) -> Any = {}) {
        Intent().apply {
            `package` = context.packageName
            action = PushReceiver.ACTION_PUSH_EVENT
            // TODO add a secret
            putExtra("instance", instance)
            putExtra("type", type)
            processIntent(this)
        }.also {
            context.sendBroadcast(it)
        }
    }

    /**
     * Run if the application doesn't implement [MessagingReceiver]
     *
     * Cache the result
     *
     * @return `true` if there isn't any implementation of [MessagingReceiver] with priority >-500
     */
    private fun shouldRun(context: Context): Boolean {
        return shouldRun ?: UnifiedPush.getResolveInfo(context, ACTION_MESSAGE, context.packageName).none {
            it.priority > -500
        }.also {
            shouldRun = it
        }
    }

    companion object {
        private var shouldRun: Boolean? = null
    }
}