package org.unifiedpush.android.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import org.unifiedpush.android.connector.keys.DefaultKeyManager
import org.unifiedpush.android.connector.keys.KeyManager
import java.security.GeneralSecurityException

/**
 * **Deprecated**, please use [PushService] instead.
 *
 * Receive UnifiedPush messages (new endpoints, unregistrations, push messages, errors) from the distributors
 *
 * ## Deprecation note
 * The library already embed a receiver implementing this receiver and forward the events to a service. This
 * allow us to maintain the declaration of the exposed service, which can make maintenance easier if any change
 * is required in the future.
 *
 * It is still possible to use this receiver directly: if a receiver with this intent filter is declared
 * in your manifest, the one declared in the library won't run.
 *
 * ## Expose this receiver
 *
 * The receiver has to be exposed in the `AndroidManifest.xml` in order to receive the UnifiedPush messages.
 *
 * ```xml
 * <receiver android:exported="true"  android:enabled="true"  android:name=".CustomReceiver">
 *     <intent-filter>
 *         <action android:name="org.unifiedpush.android.connector.MESSAGE"/>
 *         <action android:name="org.unifiedpush.android.connector.UNREGISTERED"/>
 *         <action android:name="org.unifiedpush.android.connector.NEW_ENDPOINT"/>
 *         <action android:name="org.unifiedpush.android.connector.REGISTRATION_FAILED"/>
 *     </intent-filter>
 * </receiver>
 * ```
 */
abstract class MessagingReceiver : BroadcastReceiver() {
    /**
     * Define the [KeyManager] to use. [DefaultKeyManager] by default.
     *
     * If you wish to change the [KeyManager], you need to call [UnifiedPush.register],
     * [UnifiedPush.unregister] and [UnifiedPush.removeDistributor] with the same
     * KeyManager.
     *
     * @return a [KeyManager]
     */
    open fun getKeyManager(context: Context): KeyManager {
        return DefaultKeyManager(context)
    }

    /**
     * A new endpoint is to be used for sending push messages. The new endpoint
     * should be send to the application server, and the app should sync for
     * missing notifications.
     */
    abstract fun onNewEndpoint(
        context: Context,
        endpoint: PushEndpoint,
        instance: String,
    )

    /**
     * The registration is not possible, eg. no network, depending on the reason,
     * you can try to register again directly.
     */
    abstract fun onRegistrationFailed(
        context: Context,
        reason: FailedReason,
        instance: String,
    )

    /**
     * This application is unregistered by the distributor from receiving push messages
     */
    abstract fun onUnregistered(
        context: Context,
        instance: String,
    )

    /**
     * A new message is received. The message contains the decrypted content of the push message
     * for the instance
     */
    abstract fun onMessage(
        context: Context,
        message: PushMessage,
        instance: String,
    )

    /**
     * Handle UnifiedPush messages, should not be override
     */
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val token = intent.getStringExtra(EXTRA_TOKEN)
        val store = Store(context)
        val keyManager = getKeyManager(context)
        val instance =
            token?.let {
                store.registrationSet.tryGetInstance(it)
            } ?: return
        val wakeLock = WakeLock(context)
        when (intent.action) {
            ACTION_NEW_ENDPOINT -> {
                val endpoint = intent.getStringExtra(EXTRA_ENDPOINT) ?: return
                val id = intent.getStringExtra(EXTRA_MESSAGE_ID)
                val pubKeys = keyManager.getPublicKeySet(instance)
                store.distributorAck = true
                onNewEndpoint(context, PushEndpoint(endpoint, pubKeys), instance)
                store.tryGetDistributor()?.let {
                    mayAcknowledgeMessage(context, it, id, token)
                }
            }
            ACTION_REGISTRATION_FAILED -> {
                val reason = intent.getStringExtra(EXTRA_REASON).toFailedReason()
                Log.i(TAG, "Failed: $reason")
                store.registrationSet.removeInstance(instance, keyManager)
                onRegistrationFailed(context, reason, instance)
            }
            ACTION_UNREGISTERED -> {
                onUnregistered(context, instance)
                store.registrationSet.removeInstance(instance, keyManager).ifEmpty {
                    store.removeDistributor()
                }
            }
            ACTION_MESSAGE -> {
                val message = intent.getByteArrayExtra(EXTRA_BYTES_MESSAGE) ?: return
                val id = intent.getStringExtra(EXTRA_MESSAGE_ID)
                val pushMessage =
                    try {
                        keyManager.decrypt(instance, message)?.let {
                            PushMessage(it, true)
                        } ?: PushMessage(message, false)
                    } catch (e: GeneralSecurityException) {
                        Log.w(TAG, "Could not decrypt message, trying with plain text. Cause: ${e.message}")
                        PushMessage(message, false)
                    }
                onMessage(context, pushMessage, instance)
                store.tryGetDistributor()?.let {
                    mayAcknowledgeMessage(context, it, id, token)
                }
            }
        }
        wakeLock.release()
    }

    private fun mayAcknowledgeMessage(
        context: Context,
        distributor: String,
        id: String?,
        token: String,
    ) {
        id?.let {
            val broadcastIntent =
                Intent().apply {
                    `package` = distributor
                    action = ACTION_MESSAGE_ACK
                    putExtra(EXTRA_TOKEN, token)
                    putExtra(EXTRA_MESSAGE_ID, it)
                }
            context.sendBroadcast(broadcastIntent)
        }
    }
}
