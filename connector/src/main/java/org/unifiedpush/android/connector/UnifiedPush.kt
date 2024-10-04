package org.unifiedpush.android.connector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log

/**
 * Object containing functions to interact with the distributor
 *
 * <!-- Note: This must be mirrored in Module.md -->
 *
 * ### Request a new registration
 *
 * You first need to pick and save the distributor the user wants to use. If there is only one installed you can directly use that one, else this must be done with a user interaction.
 *
 * If you want, the library `org.unifiedpush.android:connector-ui` offers a customizable dialog that request user's choice and register to this distributor.
 *
 * Once the user has chosen the distributor, you have to save it with [`saveDistributor`][org.unifiedpush.android.connector.UnifiedPush.saveDistributor]. This function must be called before [`registerApp`][org.unifiedpush.android.connector.UnifiedPush.registerApp].
 *
 * When the distributor is saved, you can call [`registerApp`][org.unifiedpush.android.connector.UnifiedPush.registerApp] to request a new registration. It has optional parameters, the following example uses `messageForDistributor` and `vapid`. You can use `instance` to bring multiple-registration support to your application.
 *
 * [`registerApp`][org.unifiedpush.android.connector.UnifiedPush.registerApp] have to be called from time to time, for instance when the application starts, to be sure the distributor is still installed and correctly linked.
 *
 * <div class="tabs">
 * <input class="tabs_control hidden" type="radio" id="tabs-1-receiver-0" name="tabs-1" checked>
 * <label class="tabs_label" for="tabs-1-receiver-0">Kotlin</label>
 * <div class="tabs_content">
 * <!-- CONTENT KOTLIN -->
 *
 * ```kotlin
 * import org.unifiedpush.android.connector.UnifiedPush
 * /* ... */
 *
 * // Check if a distributor is already registered
 * UnifiedPush.getAckDistributor(context)?.let {
 *     // Re-register in case something broke
 *     UnifiedPush.registerApp(context, messageForDistributor, vapid)
 *     return
 * }
 * // Get a list of distributors that are available
 * val distributors = UnifiedPush.getDistributors(context)
 * // select one or ask the user which distributor to use, eg. with a dialog
 * val userDistrib = yourFunc(distributors)
 * // save the distributor
 * UnifiedPush.saveDistributor(context, userDistrib)
 * // register your app to the distributor
 * UnifiedPush.registerApp(context, messageForDistributor, vapid)
 * ```
 *
 * <!-- END KOTLIN -->
 * </div>
 * <input class="tabs_control hidden" type="radio" id="tabs-1-receiver-1" name="tabs-1">
 * <label class="tabs_label" for="tabs-1-receiver-1">Java</label>
 * <div class="tabs_content">
 * <!-- CONTENT JAVA -->
 *
 * ```java
 * import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;
 * import org.unifiedpush.android.connector.UnifiedPush;
 * /* ... */
 *
 * // Check if a distributor is already registered
 * if (UnifiedPush.getAckDistributor(context) != null) {
 *     // Re-register in case something broke
 *     UnifiedPush.registerApp(
 *         context,
 *         INSTANCE_DEFAULT,
 *         messageForDistributor,
 *         vapid,
 *         true
 *     );
 *     return;
 * }
 * // Get a list of distributors that are available
 * List<String> distributors = UnifiedPush.getDistributors(context);
 * // select one or show a dialog or whatever
 * String userDistrib = yourFunc(distributors);
 * // the below line will crash the app if no distributors are available
 * UnifiedPush.saveDistributor(context, userDistrib);
 * UnifiedPush.registerApp(
 *     context,
 *     INSTANCE_DEFAULT,
 *     messageForDistributor,
 *     vapid,
 *     true
 * );
 * ```
 *
 * <!-- END JAVA -->
 * </div>
 * </div>
 *
 * ### Unsubscribe
 *
 * To unsubscribe, simply call [`unregisterApp`][org.unifiedpush.android.connector.UnifiedPush.unregisterApp]. Set the instance you want to unsubscribed to if you used one during registration.
 *
 * It removes the distributor if this is the last instance to unregister.
 */
object UnifiedPush {

    // For compatibility purpose with AND_2
    private const val FEATURE_BYTES_MESSAGE = "org.unifiedpush.android.distributor.feature.BYTES_MESSAGE"

    /**
     * Request a new registration for the [instance] to the saved distributor.
     *
     * [saveDistributor] must be called before this function. Else [MessagingReceiver.onRegistrationFailed] will be called with the reason [FailedReason.DISTRIBUTOR_NOT_SAVED].
     *
     * If there was a distributor but it has been removed, [MessagingReceiver.onUnregistered] will be called for all subscribed instances.
     *
     * @param [instance] Registration instance. Can be used to get multiple registrations, eg. for multi-account support.
     * @param [messageForDistributor] May be shown by the distributor UI to identify this registration
     * @param [vapid] VAPID public key ([RFC8292](https://www.rfc-editor.org/rfc/rfc8292)) base64url encoded of the uncompressed form (87 chars long)
     * @param [encrypted] If push message decryption is to be supported by the library (default=true)
     *
     * @return A [PublicKeySet] if the decryption of the messages is handled by the library (default)
     */
    @JvmStatic
    fun registerApp(
        context: Context,
        instance: String = INSTANCE_DEFAULT,
        messageForDistributor: String? = null,
        vapid: String? = null,
        encrypted: Boolean = true
    ) : PublicKeySet? {
        val store = Store(context)
        return registerApp(
            context,
            store,
            store.registrationSet.newOrUpdate(
                instance,
                messageForDistributor,
                vapid,
                encrypted,
                store.getEventCountAndIncrement(),
            )
        )
    }

    @JvmStatic
    internal fun registerApp(
        context: Context,
        store: Store,
        registration: Registration
    ) : PublicKeySet? {

        val distributor = store.tryGetDistributor() ?: run {
            broadcastLocalRegistrationFailed(context, store, registration.instance, FailedReason.DISTRIBUTOR_NOT_SAVED)
            return null
        }
        val legacy = store.legacyDistributor

        if (legacy) {
            // We check if the distributor has been upgraded fo follow the new specs
            if (!isLegacyDistributor(context, distributor)) {
                // The distributor has been upgraded
                store.legacyDistributor = false
                store.distributorAck = false
                broadcastLink(context, store, distributor)
                registerAppv3(context, store, registration)
            }
            registerAppv2(context, store, registration)
        } else {
            registerAppv3(context, store, registration)
        }
        return registration.webPushKeys?.publicKeySet
    }

    @JvmStatic
    private fun registerAppv2(context: Context, store: Store, registration: Registration) {

        // If it is empty, then the distributor has been uninstalled
        // getDistributor sends UNREGISTERED locally
        val distributor = getDistributor(context, store, false) ?: run {
            broadcastLocalRegistrationFailed(context, store, registration.instance, FailedReason.DISTRIBUTOR_NOT_SAVED)
            return
        }
        val broadcastIntent = Intent().apply {
            `package` = distributor
            action = ACTION_REGISTER
            putExtra(EXTRA_TOKEN, registration.token)
            // For compatibility with AND_2
            putExtra(EXTRA_FEATURES, arrayOf(FEATURE_BYTES_MESSAGE))
            putExtra(EXTRA_APPLICATION, context.packageName)
            registration.messageForDistributor?.let {
                putExtra(EXTRA_MESSAGE_FOR_DISTRIB, it)
            }
        }
        context.sendBroadcast(broadcastIntent)
    }

    @JvmStatic
    private fun registerAppv3(context: Context, store: Store, registration: Registration) {
        //TODO check vapid format

        // Here we want to be sure the distributor is still installed
        // or we return => we use getDistributor and not the store directly
        // It doesn't have to be ack yet, because it is

        // If the distributor is uninstalled, getDistributor sends UNREGISTERED locally
        val distributor = getDistributor(context, store, false) ?: run {
            broadcastLocalRegistrationFailed(context, store, registration.instance, FailedReason.DISTRIBUTOR_NOT_SAVED)
            return
        }
        // If the auth token is empty, it means the LINKED response hasn't been received yet.
        // The registration request is saved already, and the registration will be send when
        // the LINKED is received.
        val auth = store.authToken ?: return

        val broadcastIntent = Intent().apply {
            `package` = distributor
            action = ACTION_REGISTER
            putExtra(EXTRA_TOKEN, registration.token)
            putExtra(EXTRA_AUTH_TOKEN, auth)
            putExtra(EXTRA_APPLICATION, context.packageName)
            registration.messageForDistributor?.let {
                putExtra(EXTRA_MESSAGE_FOR_DISTRIB, it)
            }
            registration.vapid?.let {
                putExtra(EXTRA_VAPID, it)
            }
        }
        context.sendBroadcast(broadcastIntent)
    }

    /**
     * Send registration request for every instances that haven't been ack
     *
     * Used when we receive a LINKED, or the result from the link activity
     */
    @JvmStatic
    internal fun registerEveryUnAckApp(context: Context, store: Store) {
        store.registrationSet.forEachRegistration {
            if (!it.ack) {
                registerApp(
                    context,
                    store,
                    it
                )
            }
        }
    }

    /**
     * Send an unregistration request for the [instance] to the saved distributor and remove the registration. Remove the distributor if this is the last instance registered.
     *
     * [MessagingReceiver.onUnregistered] won't be called after that request.
     *
     * @param [instance] Registration instance. Can be used to get multiple registrations, eg. for multi-account support.
     */
    @JvmStatic
    fun unregisterApp(context: Context, instance: String = INSTANCE_DEFAULT) {
        val store = Store(context)
        val distributor = getSavedDistributor(context) ?: run {
            store.registrationSet.removeInstances()
            store.removeDistributor()
            return
        }
        val token = store.registrationSet.tryGetToken(instance) ?: return
        val broadcastIntent = Intent().apply {
            `package` = distributor
            action = ACTION_UNREGISTER
            putExtra(EXTRA_TOKEN, token)
        }
        store.registrationSet.removeInstance(instance).ifEmpty {
            store.removeDistributor()
        }
        store.removeDistributor()
        context.sendBroadcast(broadcastIntent)
    }

    /**
     * Get a list of available distributors installed on the system
     *
     * @return The list of distributor's package name
     */
    @JvmStatic
    fun getDistributors(
        context: Context
    ): List<String> {
        return getResolveInfo(context, ACTION_REGISTER).mapNotNull {
            // Remove local package if it has embedded fcm distrib
            // and PlayServices are not available
            if (it.activityInfo.packageName == context.packageName &&
                hasEmbeddedFcmDistributor(context) &&
                !isPlayServicesAvailable(context)
            ) {
                return@mapNotNull null
            }

            return@mapNotNull it.activityInfo.packageName.also { pn ->
                Log.d(TAG, "Found distributor with package name $pn")
            }
        }
    }

    @JvmStatic
    private fun getResolveInfo(context: Context, action: String, packageName: String? = null): List<ResolveInfo> {
        val intent = Intent(action).apply {
            packageName?.let {
                `package` = it
            }
        }
        return (
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.queryBroadcastReceivers(
                        intent,
                        PackageManager.ResolveInfoFlags.of(
                            PackageManager.GET_META_DATA.toLong() +
                                    PackageManager.GET_RESOLVED_FILTER.toLong()
                        )
                    )
                } else {
                    context.packageManager.queryBroadcastReceivers(
                        Intent(ACTION_REGISTER),
                        PackageManager.GET_RESOLVED_FILTER
                    )
                }
                ).filter {
                it.activityInfo.exported || it.activityInfo.packageName == context.packageName
            }
    }

    @JvmStatic
    private fun isLegacyDistributor(context: Context, packageName: String): Boolean {
        return getResolveInfo(context, ACTION_LINK, packageName).none()
    }

    /**
     * Try to use the distributor opening the deeplink "unifiedpush://link"
     *
     * It allows users to define a default distributor for all their applications
     *
     * **External distributors will be favored over embedded distributors.**
     *
     * Be aware that this function starts a new translucent activity in order to
     * get the result of the distributor activity. You may prefer to use [LinkActivityHelper]
     * directly in your own activity instead.
     *
     * ## Usage
     *
     * Kotlin:
     * ```
     * tryUseDefaultDistributor(context) { success ->
     *     if (success) {
     *         //TODO: registerApp
     *     }
     * }
     * ```
     *
     * Java:
     * ```
     * UnifiedPush.tryUseDefaultDistributor(context, success -> {
     *     if (success) {
     *         //TODO: registerApp
     *     }
     *     return null;
     * });
     * ```
     *
     * @param [context] Must be an activity or it will fail and the callback will be called with `false`
     * @param [callback] is a function taking a Boolean as parameter. This boolean is
     * true if the registration using the deeplink succeeded.
     */
    @JvmStatic
    fun tryUseDefaultDistributor(context: Activity, callback: (Boolean) -> Unit) {
        LinkActivity.callback = callback
        val intent = Intent().apply {
            setClassName(context.packageName, LinkActivity::class.java.name)
        }
        context.startActivity(intent)
    }

    /**
     * Save [distributor] as the new distributor to use
     *
     * @param [distributor] The distributor package name
     */
    @JvmStatic
    fun saveDistributor(context: Context, distributor: String) {
        val store = Store(context)
        // We use tryGetDistributor because we don't need
        // to check if the distributor is still installed.
        // There is no reason saveDistributor is called with an
        // uninstalled distributor, and if in any case it is,
        // get*Distributor checks if it is installed.
        if (store.tryGetDistributor() != distributor) {
            store.distributorAck = false
            store.saveDistributor(distributor)
        }

        // If it supports AND_3
        if (!isLegacyDistributor(context, distributor)) {
            Log.d(TAG, "Saving distributor $distributor")
            store.legacyDistributor = false
            broadcastLink(context, store, distributor)
        } else {
            Log.d(TAG, "Saving legacy distributor $distributor")
            store.legacyDistributor = true
        }
    }
    /**
     * Get the distributor registered by the user, but the
     * distributor may not have respond yet to our requests. Most of the time [getAckDistributor] is preferred.
     *
     * @return The distributor package name if any, else null
     */
    @JvmStatic
    fun getSavedDistributor(context: Context): String? = getDistributor(context, Store(context), false)

    /**
     * Get the distributor registered by the user, and the
     * distributor has already respond to our requests
     *
     * @return The distributor package name if any, else null
     */
    @JvmStatic
    fun getAckDistributor(context: Context): String? = getDistributor(context, Store(context), true)

    @JvmStatic
    private fun getDistributor(context: Context, store: Store, ack: Boolean): String? {
        if (ack && !store.distributorAck) {
            return null
        }
        return store.tryGetDistributor()?.let { distributor ->
            if (distributor in getDistributors(context)) {
                Log.d(TAG, "Found saved distributor.")
                distributor
            } else {
                Log.d(TAG, "There was a distributor, but it isn't installed anymore")
                store.registrationSet.forEachInstance {
                    broadcastLocalUnregistered(context, store, it)
                }
                null
            }
        }
    }

    @JvmStatic
    internal fun broadcastLink(context: Context, store: Store, distributor: String) {
        // It must send REGISTRATION_FAILED with ACTION_REQUIRED
        store.lastLinkRequest = store.getEventCountAndIncrement()
        val tempToken = store.newLinkToken()
        val broadcastIntent = Intent().apply {
            `package` = distributor
            action = ACTION_LINK
            putExtra(EXTRA_LINK_TOKEN, tempToken)
        }
        context.sendBroadcast(broadcastIntent)
    }

    @JvmStatic
    private fun broadcastLocalUnregistered(context: Context, store: Store, instance: String) {
        val token = store.registrationSet.tryGetToken(instance) ?: return
        val broadcastIntent = Intent().apply {
            `package` = context.packageName
            action = ACTION_UNREGISTERED
            putExtra(EXTRA_TOKEN, token)
        }
        context.sendBroadcast(broadcastIntent)
    }

    @JvmStatic
    private fun broadcastLocalRegistrationFailed(context: Context, store: Store, instance: String, reason: FailedReason) {
        val token = store.registrationSet.tryGetToken(instance) ?: return
        val broadcastIntent = Intent().apply {
            `package` = context.packageName
            action = ACTION_REGISTRATION_FAILED
            putExtra(EXTRA_TOKEN, token)
            putExtra(EXTRA_REASON, reason.toString())
        }
        context.sendBroadcast(broadcastIntent)
    }

    @JvmStatic
    private fun hasEmbeddedFcmDistributor(context: Context): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SERVICES + PackageManager.GET_RECEIVERS)
        return packageInfo.services?.map { it.name }
            ?.contains("org.unifiedpush.android.embedded_fcm_distributor.fcm.FirebaseForwardingService") == true ||
                packageInfo.receivers?.map { it.name }
                    ?.contains("org.unifiedpush.android.foss_embedded_fcm_distributor.fcm.FirebaseReceiver") == true
    }

    @JvmStatic
    private fun isPlayServicesAvailable(context: Context): Boolean {
        val pm = context.packageManager
        try {
            pm.getPackageInfo("com.google.android.gms", PackageManager.GET_ACTIVITIES)
            return true

        } catch (e: PackageManager.NameNotFoundException) {
            Log.v(TAG, "Google services not found: ${e.message}")
        }
        return false
    }

    /**
     * Unregister all instances and remove the distributor
     */
    @JvmStatic
    fun forceRemoveDistributor(context: Context) {
        val store = Store(context)
        // TODO: send unregistration for all instances
        store.registrationSet.removeInstances()
        store.removeDistributor()
    }
}
