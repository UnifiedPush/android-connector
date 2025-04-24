package org.unifiedpush.android.connector

import android.app.Activity
import android.app.BroadcastOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.AndroidException
import android.util.Log
import org.unifiedpush.android.connector.internal.LinkActivity
import org.unifiedpush.android.connector.internal.Registration
import org.unifiedpush.android.connector.internal.Store
import org.unifiedpush.android.connector.keys.DefaultKeyManager
import org.unifiedpush.android.connector.keys.KeyManager
import kotlin.jvm.Throws

/**
 * Object containing functions to interact with the distributor
 *
 * <!-- Note: This must be mirrored in Module.md -->
 *
 * ### Use user's default distributor
 *
 * Users are allowed to define a default distributor on their system, because UnifiedPush distributors
 * have to be able to process a deeplink.
 *
 * When you set UnifiedPush for the first time on your application, you will want to use the default user's
 * distributor.
 *
 * From time to time, like every time you starts your application, you should register your application in case the
 * user have uninstalled the previous distributor.
 * If the previous distributor is uninstalled, you can fallback to the default one again.
 *
 * Therefore, you can use [tryUseCurrentOrDefaultDistributor][org.unifiedpush.android.connector.UnifiedPush.tryUseCurrentOrDefaultDistributor]
 * to select the saved distributor or the default one when your application starts (when your main activity is created for instance).
 *
 * When the distributor is saved, you can call [`register`][org.unifiedpush.android.connector.UnifiedPush.register] to request a new registration.
 * It has optional parameters, the following example uses `messageForDistributor` and `vapid`.
 * You can use `instance` to bring multiple-registration support to your application.
 *
 * _If you want, you can use the library `org.unifiedpush.android:connector-ui` instead, it displays a dialog explaining why
 * the OS picker is going to ask which application to pick._
 *
 * <div class="tabs">
 * <input class="tabs_control hidden" type="radio" id="tabs-trydefault-receiver-0" name="tabs-trydefault" checked>
 * <label class="tabs_label" for="tabs-trydefault-receiver-0">Kotlin</label>
 * <div class="tabs_content">
 * <!-- CONTENT KOTLIN -->
 *
 * ```kotlin
 * import org.unifiedpush.android.connector.UnifiedPush
 * /* ... */
 *
 * UnifiedPush.tryUseCurrentOrDefaultDistributor(context) { success ->
 *     if (success) {
 *         // We have a distributor
 *         // Register your app to the distributor
 *         UnifiedPush.register(context, messageForDistributor, vapid)
 *     }
 * }
 * ```
 *
 * <!-- END KOTLIN -->
 * </div>
 * <input class="tabs_control hidden" type="radio" id="tabs-trydefault-receiver-1" name="tabs-trydefault">
 * <label class="tabs_label" for="tabs-trydefault-receiver-1">Java</label>
 * <div class="tabs_content">
 * <!-- CONTENT JAVA -->
 *
 * ```java
 * import static org.unifiedpush.android.connector.ConstantsKt.INSTANCE_DEFAULT;
 * import org.unifiedpush.android.connector.UnifiedPush;
 * /* ... */
 *
 * UnifiedPush.tryUseCurrentOrDefaultDistributor(context, success ->{
 *     if (success) {
 *         // We have a distributor
 *         // Register your app to the distributor
 *         UnifiedPush.register(
 *             context,
 *             INSTANCE_DEFAULT,
 *             messageForDistributor,
 *             vapid
 *         );
 *     }
 * });
 * ```
 *
 * <!-- END JAVA -->
 * </div>
 * </div>
 *
 * Be aware that [tryUseDefaultDistributor][org.unifiedpush.android.connector.UnifiedPush.tryUseDefaultDistributor]
 * starts a new translucent activity in order to get the result of the distributor activity. You may prefer to use
 * [LinkActivityHelper][org.unifiedpush.android.connector.LinkActivityHelper] directly in your own activity instead.
 *
 * ### Use another distributor
 *
 * You will probably want to allow the users to use another distributor but their default one.
 *
 * For this, you can get the list of available distributors with [`getDistributors`][org.unifiedpush.android.connector.UnifiedPush.getDistributors].
 *
 * Once the user has chosen the distributor, you have to save it with [`saveDistributor`][org.unifiedpush.android.connector.UnifiedPush.saveDistributor].
 * This function must be called before [`register`][org.unifiedpush.android.connector.UnifiedPush.register].
 *
 * When the distributor is saved, you can call [`register`][org.unifiedpush.android.connector.UnifiedPush.register] to request a new registration.
 * It has optional parameters, the following example uses `messageForDistributor` and `vapid`.
 * You can use `instance` to bring multiple-registration support to your application.
 *
 * _If you want, the library `org.unifiedpush.android:connector-ui` offers a customizable dialog
 * that request user's choice and register to this distributor._
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
 * // Get a list of distributors that are available
 * val distributors = UnifiedPush.getDistributors(context)
 * // select one or ask the user which distributor to use, eg. with a dialog
 * val userDistrib = yourFunc(distributors)
 * // save the distributor
 * UnifiedPush.saveDistributor(context, userDistrib)
 * // register your app to the distributor
 * UnifiedPush.register(context, messageForDistributor, vapid)
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
 * // Get a list of distributors that are available
 * List<String> distributors = UnifiedPush.getDistributors(context);
 * // select one or show a dialog or whatever
 * String userDistrib = yourFunc(distributors);
 * // the below line will crash the app if no distributors are available
 * UnifiedPush.saveDistributor(context, userDistrib);
 * UnifiedPush.register(
 *     context,
 *     INSTANCE_DEFAULT,
 *     messageForDistributor,
 *     vapid
 * );
 * ```
 *
 * <!-- END JAVA -->
 * </div>
 * </div>
 *
 * ### Unsubscribe
 *
 * To unsubscribe, simply call [`unregister`][org.unifiedpush.android.connector.UnifiedPush.unregister]. Set the instance you want to unsubscribed to if you used one during registration.
 *
 * It removes the distributor if this is the last instance to unregister.
 */
object UnifiedPush {
    // For compatibility purpose with AND_2
    private const val FEATURE_BYTES_MESSAGE = "org.unifiedpush.android.distributor.feature.BYTES_MESSAGE"

    @JvmStatic
    private val VAPID_REGEX = Regex("^[A-Za-z0-9_-]{87}$")

    /**
     * Request a new registration for the [instance] to the saved distributor.
     *
     * [saveDistributor] must be called before this function.
     *
     * If there was a distributor but it has been removed, [MessagingReceiver.onUnregistered] will be called for all subscribed instances.
     *
     * @param [context] To interact with the shared preferences and send broadcast intents.
     * @param [instance] Registration instance. Can be used to get multiple registrations, eg. for multi-account support.
     * @param [messageForDistributor] May be shown by the distributor UI to identify this registration.
     * @param [vapid] VAPID public key ([RFC8292](https://www.rfc-editor.org/rfc/rfc8292)) base64url encoded of the uncompressed form (87 chars long).
     *
     * @throws [VapidNotValidException] if [vapid] is not in the in the uncompressed form and base64url encoded.
     */
    @JvmStatic
    fun register(
        context: Context,
        instance: String = INSTANCE_DEFAULT,
        messageForDistributor: String? = null,
        vapid: String? = null,
    ) {
        register(context, instance, messageForDistributor, vapid, DefaultKeyManager(context))
    }

    /** @hide */
    @Deprecated("Renamed",
        replaceWith = ReplaceWith("UnifiedPush.register(context, instance, messageForDistributor, vapid)"))
    @JvmStatic
    fun registerApp(
        context: Context,
        instance: String = INSTANCE_DEFAULT,
        messageForDistributor: String? = null,
        vapid: String? = null,
    ) = register(context, instance, messageForDistributor, vapid)

    /**
     * [register] with additional [KeyManager] parameter.
     *
     * @param [keyManager] To manager web push keys. By default: [DefaultKeyManager].
     */
    @JvmStatic
    fun register(
        context: Context,
        instance: String = INSTANCE_DEFAULT,
        messageForDistributor: String? = null,
        vapid: String? = null,
        keyManager: KeyManager,
    ) {
        val store = Store(context)
        register(
            context,
            store,
            store.registrationSet.newOrUpdate(
                instance,
                messageForDistributor,
                vapid,
                keyManager,
            ),
        )
    }

    @JvmStatic
    @Throws(VapidNotValidException::class)
    private fun register(
        context: Context,
        store: Store,
        registration: Registration,
    ) {
        val distributor = getDistributor(context, store, false) ?: return
        registration.vapid?.let {
            // This is mainly to catch VAPID used with the wrong format,
            // no need to check if this is a real vapid key
            if (!VAPID_REGEX.matches(it)) {
                throw VapidNotValidException()
            }
        }

        // For SDK < 34
        val dummyIntent = Intent("org.unifiedpush.dummy_app")
        val pi = PendingIntent.getBroadcast(context, 0, dummyIntent, PendingIntent.FLAG_IMMUTABLE)

        val broadcastIntent =
            Intent().apply {
                `package` = distributor
                action = ACTION_REGISTER
                putExtra(EXTRA_TOKEN, registration.token)
                // For compatibility with AND_2
                putExtra(EXTRA_FEATURES, arrayOf(FEATURE_BYTES_MESSAGE))
                // For compatibility with AND_2, replaced by pi for SDK < 34
                putExtra(EXTRA_APPLICATION, context.packageName)
                // For SDK < 34
                putExtra(EXTRA_PI, pi)
                registration.messageForDistributor?.let {
                    putExtra(EXTRA_MESSAGE_FOR_DISTRIB, it)
                }
                registration.vapid?.let {
                    putExtra(EXTRA_VAPID, it)
                }
            }
        if (Build.VERSION.SDK_INT >= 34) {
            val broadcastOptions = BroadcastOptions.makeBasic().setShareIdentityEnabled(true)
            context.sendBroadcast(broadcastIntent, null, broadcastOptions.toBundle())
        } else {
            context.sendBroadcast(broadcastIntent)
        }
    }

    /**
     * Send an unregistration request for the [instance] to the saved distributor and remove the registration. Remove the distributor if this is the last instance registered.
     *
     * [MessagingReceiver.onUnregistered] won't be called after that request.
     *
     * @param [context] To interact with the shared preferences and send broadcast intents.
     * @param [instance] Registration instance. Can be used to get multiple registrations, eg. for multi-account support.
     */
    @JvmStatic
    fun unregister(
        context: Context,
        instance: String = INSTANCE_DEFAULT,
    ) {
        unregister(context, instance, DefaultKeyManager(context))
    }

    /** @hide */
    @Deprecated("Renamed", replaceWith = ReplaceWith("UnifiedPush.unregister(context, instance)"))
    @JvmStatic
    fun unregisterApp(
        context: Context,
        instance: String = INSTANCE_DEFAULT
    ) = unregister(context, instance)

    /**
     * [unregister] with additional [KeyManager] parameter.
     *
     * @param [keyManager] To manager web push keys. By default: [DefaultKeyManager].
     */
    @JvmStatic
    fun unregister(
        context: Context,
        instance: String = INSTANCE_DEFAULT,
        keyManager: KeyManager,
    ) {
        val store = Store(context)
        val distributor =
            getDistributor(context, store, false) ?: run {
                // This should not be necessary
                store.registrationSet.removeInstances(keyManager)
                store.removeDistributor()
                return
            }

        // For SDK < 34
        val dummyIntent = Intent("org.unifiedpush.dummy_app")
        val pi = PendingIntent.getBroadcast(context, 0, dummyIntent, PendingIntent.FLAG_IMMUTABLE)

        val token = store.registrationSet.tryGetToken(instance) ?: return
        val broadcastIntent =
            Intent().apply {
                `package` = distributor
                action = ACTION_UNREGISTER
                putExtra(EXTRA_TOKEN, token)
                // For SDK < 34
                putExtra(EXTRA_PI, pi)
            }
        store.registrationSet.removeInstance(instance, keyManager).ifEmpty {
            store.removeDistributor()
        }

        if (Build.VERSION.SDK_INT >= 34) {
            val broadcastOptions = BroadcastOptions.makeBasic().setShareIdentityEnabled(true)
            context.sendBroadcast(broadcastIntent, null, broadcastOptions.toBundle())
        } else {
            context.sendBroadcast(broadcastIntent)
        }
    }

    /**
     * Get a list of available distributors installed on the system
     *
     * @return The list of distributor's package name
     */
    @JvmStatic
    fun getDistributors(context: Context): List<String> {
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
        }.distinct()
    }

    @JvmStatic
    internal fun getResolveInfo(
        context: Context,
        action: String,
        packageName: String? = null,
    ): List<ResolveInfo> {
        val intent =
            Intent(action).apply {
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
                            PackageManager.GET_RESOLVED_FILTER.toLong(),
                    ),
                )
            } else {
                context.packageManager.queryBroadcastReceivers(
                    intent,
                    PackageManager.GET_RESOLVED_FILTER,
                )
            }
        ).filter {
            it.activityInfo.exported || it.activityInfo.packageName == context.packageName
        }
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
     *         //TODO: register
     *     }
     * }
     * ```
     *
     * Java:
     * ```
     * UnifiedPush.tryUseDefaultDistributor(context, success -> {
     *     if (success) {
     *         //TODO: register
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
    fun tryUseDefaultDistributor(
        context: Context,
        callback: (Boolean) -> Unit,
    ) {
        if (context is Activity) {
            LinkActivityHelper.resolveLinkActivityPackageName(context)?.let {
                if (it == "android") {
                    LinkActivity.callback = callback
                    val intent =
                        Intent().apply {
                            setClassName(context.packageName, LinkActivity::class.java.name)
                        }
                    context.startActivity(intent)
                } else {
                    saveDistributor(context, it)
                    callback(true)
                }
            } ?: run {
                // Fallback to AND_2, if there is only one distributor, we use it.
                val distributors = getDistributors(context)
                if (distributors.size == 1) {
                    saveDistributor(context, distributors.first())
                    callback(true)
                } else {
                    callback(false)
                }
            }
        } else {
            callback(false)
        }
    }

    /**
     * Try to use the saved distributor else, use the default distributor opening the deeplink "unifiedpush://link"
     *
     * It can be used on application startup to register to the distributor.
     * If you had already registered to a distributor, this ensure the connection is working.
     * If the previous distributor has been uninstalled, it will fallback to the user's default.
     * If you register for the first time, it will use the user's default Distributor or the OS will
     * ask what it should use.
     *
     * **External distributors will be favored over embedded distributors.**
     *
     * Be aware that this function may start a new translucent activity in order to
     * get the result of the distributor activity. You may prefer to use [LinkActivityHelper]
     * directly in your own activity instead.
     *
     * ## Usage
     *
     * Kotlin:
     * ```
     * tryUseCurrentOrDefaultDistributor(context) { success ->
     *     if (success) {
     *         //TODO: register
     *     }
     * }
     * ```
     *
     * Java:
     * ```
     * UnifiedPush.tryUseCurrentOrDefaultDistributor(context, success -> {
     *     if (success) {
     *         //TODO: register
     *     }
     *     return null;
     * });
     * ```
     *
     * @param [context] Must be an activity or it will fail if there is no current distributor and the callback will be called with `false`
     * @param [callback] is a function taking a Boolean as parameter. This boolean is
     * true if the registration using the deeplink succeeded.
     */
    @JvmStatic
    fun tryUseCurrentOrDefaultDistributor(
        context: Context,
        callback: (Boolean) -> Unit,
    ) {
        getAckDistributor(context)?.let {
            callback(true)
        } ?: run {
            tryUseDefaultDistributor(context, callback)
        }
    }

    /**
     * Save [distributor] as the new distributor to use
     *
     * @param [context] To interact with the shared preferences.
     * @param [distributor] The distributor package name
     */
    @JvmStatic
    fun saveDistributor(
        context: Context,
        distributor: String,
    ) {
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
    }

    /**
     * Get the distributor registered by the user, but the
     * distributor may not have respond yet to our requests. Most of the time [getAckDistributor] is preferred.
     *
     * Will call [MessagingReceiver.onUnregistered] for all instances if the distributor
     * is not installed anymore.
     *
     * @return The distributor package name if any, else null
     */
    @JvmStatic
    fun getSavedDistributor(context: Context): String? = getDistributor(context, Store(context), false)

    /**
     * Get the distributor registered by the user, and the
     * distributor has already respond to our requests
     *
     * Will call [MessagingReceiver.onUnregistered] for all instances if the distributor
     * is not installed anymore.
     *
     * @return The distributor package name if any, else null
     */
    @JvmStatic
    fun getAckDistributor(context: Context): String? = getDistributor(context, Store(context), true)

    /**
     * Get the current distributor.
     *
     * Send [ACTION_UNREGISTERED] for all instances if the distributor in [Store.tryGetDistributor]
     * is not installed anymore.
     *
     * @param [context] [Context] to interact with package manager.
     * @param [store] [Store] to access our shared preferences.
     * @param [ack] `true` if the distributor has to be ack.
     */
    @JvmStatic
    private fun getDistributor(
        context: Context,
        store: Store,
        ack: Boolean,
    ): String? {
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
    private fun broadcastLocalUnregistered(
        context: Context,
        store: Store,
        instance: String,
    ) {
        val token = store.registrationSet.tryGetToken(instance) ?: return
        val broadcastIntent =
            Intent().apply {
                `package` = context.packageName
                action = ACTION_UNREGISTERED
                putExtra(EXTRA_TOKEN, token)
            }
        context.sendBroadcast(broadcastIntent)
    }

    @JvmStatic
    private fun hasEmbeddedFcmDistributor(context: Context): Boolean {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SERVICES + PackageManager.GET_RECEIVERS,
            )
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
     *
     * @param [context] To interact with the shared preferences and send broadcast intents.
     */
    @JvmStatic
    fun removeDistributor(context: Context) {
        removeDistributor(context, DefaultKeyManager(context))
    }

    /** @hide */
    @Deprecated("Renamed", replaceWith = ReplaceWith("UnifiedPush.removeDistributor(context)"))
    @JvmStatic
    fun forceRemoveDistributor(context: Context) = removeDistributor(context)

    /**
     * [removeDistributor] with additional [KeyManager] parameter.
     *
     * @param [keyManager] To manager web push keys. By default: [DefaultKeyManager].
     */
    @JvmStatic
    fun removeDistributor(
        context: Context,
        keyManager: KeyManager,
    ) {
        val store = Store(context)
        store.registrationSet.forEachInstance {
            unregister(context, it)
        }
        store.registrationSet.removeInstances(keyManager)
        store.removeDistributor()
    }

    /**
     * The VAPID public key is not in the right format.
     *
     * It should be in the uncompressed form, and base64url encoded (87 chars long)
     */
    class VapidNotValidException : AndroidException()
}
