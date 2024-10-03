package org.unifiedpush.android.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.google.crypto.tink.apps.webpush.WebPushHybridDecrypt
import java.security.GeneralSecurityException
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

/**
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
 *     override fun onMessage(context: Context, message: ByteArray, instance: String) {
 *         // TODO: handle message, eg. to sync remote data or show a notification to the user
 *     }
 *
 *     override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
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
 *     public void onMessage(@NotNull Context context, @NotNull byte[] message, @NotNull String instance) {
 *         // TODO: handle message, eg. to sync remote data or show a notification to the user
 *     }
 *
 *     @Override
 *     public void onNewEndpoint(@NotNull Context context, @NotNull String endpoint, @NotNull String instance) {
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
 *         <action android:name="org.unifiedpush.android.connector.LINKED"/>
 *         <action android:name="org.unifiedpush.android.connector.MESSAGE"/>
 *         <action android:name="org.unifiedpush.android.connector.UNREGISTERED"/>
 *         <action android:name="org.unifiedpush.android.connector.NEW_ENDPOINT"/>
 *         <action android:name="org.unifiedpush.android.connector.REGISTRATION_FAILED"/>
 *     </intent-filter>
 * </receiver>
 * ```
 */
open class MessagingReceiver : BroadcastReceiver() {

    /**
     * A new endpoint is to be used for sending push messages. The new endpoint
     * should be send to the application server, and the app should sync for
     * missing notifications.
     */
    open fun onNewEndpoint(context: Context, endpoint: String, instance: String) {}
    /**
     * The registration is not possible, eg. no network, depending on the reason,
     * you can try to register again directly.
     */
    open fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {}
    /**
     * This application is unregistered by the distributor from receiving push messages
     */
    open fun onUnregistered(context: Context, instance: String) {}
    /**
     * A new message is received. The message contains the decrypted content of the push message
     * for the instance
     */
    open fun onMessage(context: Context, message: ByteArray, instance: String) {}

    /**
     * Handle UnifiedPush messages, should not be overriden
     */
    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.getStringExtra(EXTRA_TOKEN)
        val store = Store(context)
        // For the linked request, we use a temporary token,
        // which is different from connection tokens
        if (intent.action == ACTION_LINKED
            && intent.getStringExtra(EXTRA_LINK_TOKEN) == store.linkToken) {
            store.authToken = intent.getStringExtra(EXTRA_AUTH_TOKEN) ?: return
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
                store.registrationSet.tryGetWebPushKeys(instance)?.let { wp ->
                    try {
                        val hybridDecrypt =
                            WebPushHybridDecrypt.Builder()
                                .withAuthSecret(wp.auth)
                                .withRecipientPublicKey(wp.keyPair.public as ECPublicKey)
                                .withRecipientPrivateKey(wp.keyPair.private as ECPrivateKey)
                                .build()
                        val clearMessage = hybridDecrypt.decrypt(message, null)
                        onMessage(context, clearMessage, instance)
                    } catch (e: GeneralSecurityException) {
                        Log.w(TAG, "Could not decrypt message, trying with plain text. Cause: ${e.message}")
                        onMessage(context, message, instance)
                    }
                } ?: onMessage(context, message, instance)
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
