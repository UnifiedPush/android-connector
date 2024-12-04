package org.unifiedpush.android.connector

import WakeLock
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import java.io.Serializable

/**
 * Service to receive UnifiedPush messages (new endpoints, unregistrations, push messages, errors) from the distributors
 *
 * You need to declare a service that extend [PushService]. This service must not be exported and
 * must handle the action `org.unifiedpush.android.connector.PUSH_EVENT`:
 *
 * ```xml
 * <service android:name=".PushServiceImpl"
 *     android:exported="false">
 *     <intent-filter>
 *         <action android:name="org.unifiedpush.android.connector.PUSH_EVENT"/>
 *     </intent-filter>
 * </service>
 * ```
 * You need to use [UnifiedPush] to register for push notifications.
 */
abstract class PushReceiver: BroadcastReceiver() {
    /** Type of Push Event */
    internal enum class PushEventType {
        REGISTRATION_FAILED,
        UNREGISTERED,
        NEW_ENDPOINT,
        MESSAGE;
    }

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getSerializableExtraForT("type", PushEventType::class.java)
            ?: return
        val instance = intent.getStringExtra("instance")
            ?: return
        val wakeLock = WakeLock(context)
        when (type) {
            PushEventType.REGISTRATION_FAILED -> {
                val reason = intent.getSerializableExtraForT("reason", FailedReason::class.java)
                    ?: return
                onRegistrationFailed(context, reason, instance)
            }
            PushEventType.UNREGISTERED -> onUnregistered(context, instance)
            PushEventType.NEW_ENDPOINT -> {
                val endpoint = intent.getParcelableExtraForT("endpoint", PushEndpoint::class.java)
                    ?: return
                onNewEndpoint(context, endpoint, instance)
            }
            PushEventType.MESSAGE -> {
                val message = intent.getParcelableExtraForT("message", PushMessage::class.java)
                    ?: return
                onMessage(context, message, instance)
            }
        }
        wakeLock.release()
    }

    private fun <T : Serializable>Intent.getSerializableExtraForT(name: String, tClass: Class<T>): T? {
        return if (Build.VERSION.SDK_INT > 32) {
            this.getSerializableExtra(name, tClass)
        } else {
            this.getSerializableExtra(name) as T?
        }
    }

    private fun <T : Parcelable>Intent.getParcelableExtraForT(name: String, tClass: Class<T>): T? {
        return if (Build.VERSION.SDK_INT > 32) {
            this.getParcelableExtra(name, tClass)
        } else {
            this.getParcelableExtra(name) as T?
        }
    }

    /**
     * A new endpoint is to be used for sending push messages. The new endpoint
     * should be send to the application server, and the app should sync for
     * missing notifications.
     */
    abstract fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String)

    /**
     * A new message is received. The message contains the decrypted content of the push message
     * for the instance
     */
    abstract fun onMessage(context: Context, message: PushMessage, instance: String)

    /**
     * The registration is not possible, eg. no network, depending on the reason,
     * you can try to register again directly.
     */
    abstract fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String)

    /**
     * This application is unregistered by the distributor from receiving push messages
     */
    abstract fun onUnregistered(context: Context, instance: String)

    internal companion object {
        const val ACTION_PUSH_EVENT = "org.unifiedpush.android.connector.PUSH_EVENT"
    }
}