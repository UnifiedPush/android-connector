package org.unifiedpush.android.connector.internal

import android.content.Context
import android.content.Intent
import org.unifiedpush.android.connector.ACTION_MESSAGE
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.UnifiedPush
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

/**
 * @hide
 *
 * Implementation of [MessagingReceiver] to receive UnifiedPush events and
 * forward to [PushService]. Runs only if there isn't another implementation
 * of [MessagingReceiver] (with priority>-500) declared
 */
class MessagingReceiverImpl : MessagingReceiver() {
    override fun onUnregistered(context: Context, instance: String) {
        InternalPushServiceConnection.sendEvent(
            context,
            InternalPushServiceConnection.Event.Unregistered(instance)
        )
    }

    override fun onMessage(context: Context, message: PushMessage, instance: String) {
        InternalPushServiceConnection.sendEvent(
            context,
            InternalPushServiceConnection.Event.Message(message, instance)
        )
    }

    override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
        InternalPushServiceConnection.sendEvent(
            context,
            InternalPushServiceConnection.Event.NewEndpoint(endpoint, instance)
        )
    }

    override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
        InternalPushServiceConnection.sendEvent(
            context,
            InternalPushServiceConnection.Event.RegistrationFailed(reason, instance)
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (shouldRun(context)) {
            super.onReceive(context, intent)
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
        return shouldRun ?: UnifiedPush.getResolveInfo(context, ACTION_MESSAGE, context.packageName)
            .none {
            it.priority > -500
        }.also {
            shouldRun = it
        }
    }

    companion object {
        private var shouldRun: Boolean? = null
    }
}