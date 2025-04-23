package org.unifiedpush.android.connector

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

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
abstract class PushService: Service() {
    /**
     * A new endpoint is to be used for sending push messages. The new endpoint
     * should be send to the application server, and the app should sync for
     * missing notifications.
     */
    abstract fun onNewEndpoint(endpoint: PushEndpoint, instance: String)

    /**
     * A new message is received. The message contains the decrypted content of the push message
     * for the instance
     */
    abstract fun onMessage(message: PushMessage, instance: String)

    /**
     * The registration is not possible, eg. no network, depending on the reason,
     * you can try to register again directly.
     */
    abstract fun onRegistrationFailed(reason: FailedReason, instance: String)

    /**
     * This application is unregistered by the distributor from receiving push messages
     */
    abstract fun onUnregistered(instance: String)

    /**
     * @hide
     * Return [PushBinder] onBind
     */
    override fun onBind(intent: Intent?): IBinder? {
        return PushBinder()
    }

    /**
     * @hide
     * Binder to expose the [PushService].
     *
     * Works only within the app
     */
    inner class PushBinder: Binder() {
        /**
         * Get [PushService]
         *
         * @throws SecurityException if not called within the app
         */
        fun getService(): PushService {
            if (getCallingUid() != android.os.Process.myUid()) {
                throw SecurityException("Binding allowed only within the app")
            }
            return this@PushService
        }
    }

    internal companion object {
        const val ACTION_PUSH_EVENT = "org.unifiedpush.android.connector.PUSH_EVENT"
    }
}
