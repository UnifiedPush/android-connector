package org.unifiedpush.android.connector

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log

object UnifiedPush {

    // For compatibility purpose with AND_2
    private const val FEATURE_BYTES_MESSAGE = "org.unifiedpush.android.distributor.feature.BYTES_MESSAGE"

    @JvmStatic
    fun registerApp(
        context: Context,
        instance: String = INSTANCE_DEFAULT,
        messageForDistributor: String? = null,
        vapid: String? = null
    ) {
        val store = Store(context)
        registerApp(
            context,
            store,
            store.registrationSet.newOrUpdate(
                instance,
                messageForDistributor,
                vapid,
                store.getEventCountAndIncrement()
            )
        )
    }

    @JvmStatic
    internal fun registerApp(
        context: Context,
        store: Store,
        registration: Registration
    ) {

        // TODO throw error if store.tryGetDistributor is null
        val distributor = store.tryGetDistributor() ?: return
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
    }

    @JvmStatic
    private fun registerAppv2(context: Context, store: Store, registration: Registration) {

        // If it is empty, then the distributor has been uninstalled
        // getDistributor sends UNREGISTERED locally
        // TODO throw error if the distrib has been uninstalled
        val distributor = getDistributor(context, store, false) ?: return
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

        // Here we want to be sure the distributor is still installed
        // or we return => we use getDistributor and not the store directly
        // It doesn't have to be ack yet, because it is

        // If the distributor is uninstalled, getDistributor sends UNREGISTERED locally
        // TODO throw error if the distrib has been uninstalled
        val distributor = getDistributor(context, store, false) ?: return
        // If the auth token is empty, it means the LINKED response hasn't been received yet
        // The registration request is saved already, and the registration will be send when
        // the LINKED is received.
        val auth = store.authToken ?: return

        val broadcastIntent = Intent().apply {
            `package` = distributor
            action = ACTION_REGISTER
            putExtra(EXTRA_TOKEN, registration.token)
            putExtra(EXTRA_AUTH, auth)
            registration.messageForDistributor?.let {
                putExtra(EXTRA_MESSAGE_FOR_DISTRIB, it)
            }
            registration.vapid?.let {
                putExtra(EXTRA_VAPID, it)
            }
        }
        context.sendBroadcast(broadcastIntent)
    }

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
     * This function returns the distributor registered by the user,
     * but the distributor may not have sent a new endpoint yet
     */
    @JvmStatic
    fun getSavedDistributor(context: Context): String? = getDistributor(context, Store(context), false)

    /**
     * This function returns the distributor registered by the user,
     * and the distributor has already sent a new endpoint
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
        val tempToken = store.newTempToken()
        val broadcastIntent = Intent().apply {
            `package` = distributor
            action = ACTION_LINK
            putExtra(EXTRA_AUTH, tempToken)
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
        store.registrationSet.removeInstance(instance).ifEmpty {
            store.removeDistributor()
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

    @JvmStatic
    fun forceRemoveDistributor(context: Context) {
        val store = Store(context)
        store.registrationSet.removeInstances()
        store.removeDistributor()
    }
}
