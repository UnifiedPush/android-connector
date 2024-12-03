package org.unifiedpush.android.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import org.unifiedpush.android.connector.keys.DefaultKeyManager
import org.unifiedpush.android.connector.keys.KeyManager
import java.security.GeneralSecurityException

/**
 * @hide
 *
 * Receive UnifiedPush messages (new endpoints, unregistrations, push messages, errors) from the distributors
 *
 * ## Expose a receiver
 *
 * <!-- Note: This must be mirrored in Module.md -->
 *
 * You need to expose a receiver that extend [`MessagingReceiver`][org.unifiedpush.android.connector.MessagingReceiver] and override the following methods:
 * - [onMessage][org.unifiedpush.android.connector.MessagingReceiver.onMessage]
 * - [onNewEndpoint][org.unifiedpush.android.connector.MessagingReceiver.onNewEndpoint]
 * - [onUnregistered][org.unifiedpush.android.connector.MessagingReceiver.onUnregistered]
 * - [onRegistrationFailed][org.unifiedpush.android.connector.MessagingReceiver.onRegistrationFailed]
 *
 * <div class="tabs">
 * <input class="tabs_control hidden" type="radio" id="tabs-0-receiver-0" name="tabs-0" checked>
 * <label class="tabs_label" for="tabs-0-receiver-0">Kotlin</label>
 * <div class="tabs_content">
 * <!-- CONTENT KOTLIN -->
 *
 * ```kotlin
 * class CustomReceiver: MessagingReceiver() {
 *     override fun onMessage(context: Context, message: PushMessage, instance: String) {
 *         // TODO: handle message, eg. to sync remote data or show a notification to the user
 *     }
 *
 *     override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
 *         // TODO: send new endpoint to the app server
 *     }
 *
 *     override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
 *         // TODO: retry depending on the reason
 *     }
 *
 *     override fun onUnregistered(context: Context, instance: String){
 *         // TODO: ask to register to another distributor
 *     }
 * }
 * ```
 *
 * <!-- END KOTLIN -->
 * </div>
 * <input class="tabs_control hidden" type="radio" id="tabs-0-receiver-1" name="tabs-0">
 * <label class="tabs_label" for="tabs-0-receiver-1">Java</label>
 * <div class="tabs_content">
 * <!-- CONTENT JAVA -->
 *
 * ```java
 * class CustomReceiver extends MessagingReceiver {
 *     public CustomReceiver() {
 *         super();
 *     }
 *
 *     @Override
 *     public void onMessage(@NotNull Context context, @NotNull PushMessage message, @NotNull String instance) {
 *         // TODO: handle message, eg. to sync remote data or show a notification to the user
 *     }
 *
 *     @Override
 *     public void onNewEndpoint(@NotNull Context context, @NotNull PushEndpoint endpoint, @NotNull String instance) {
 *         // TODO: send new endpoint to the app server
 *     }
 *
 *     @Override
 *     public void onRegistrationFailed(@NotNull Context context, @NotNull FailedReason reason, @NotNull String instance) {
 *         // TODO: retry depending on the reason
 *     }
 *
 *     @Override
 *     public void onUnregistered(@NotNull Context context, @NotNull String instance) {
 *         // TODO: ask to register to another distributor
 *     }
 * }
 * ```
 *
 * <!-- END JAVA -->
 * </div>
 * </div>
 *
 * ## Edit your manifest
 *
 * <!-- Note: This must be mirrored in Module.md -->
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
        val wakeLock =
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                    acquire(60000L) // 1min
                }
            }
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
