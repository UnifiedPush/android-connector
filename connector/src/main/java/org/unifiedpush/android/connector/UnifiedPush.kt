package org.unifiedpush.android.connector

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object UnifiedPush {

    // For compatibility purpose with AND_2
    private const val FEATURE_BYTES_MESSAGE = "org.unifiedpush.android.distributor.feature.BYTES_MESSAGE"

    @JvmStatic
    fun registerApp(
        context: Context,
        instance: String = INSTANCE_DEFAULT,
        messageForDistributor: String = "",
        vapid: String? = null
    ) {
        val store = Store(context)
        val token = store.getTokenOrNew(instance)

        val distributor = getSavedDistributor(context) ?: return

        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_REGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        // For compatibility purpose with AND_2
        broadcastIntent.putExtra(EXTRA_FEATURES, arrayOf(FEATURE_BYTES_MESSAGE))
        broadcastIntent.putExtra(EXTRA_MESSAGE_FOR_DISTRIB, messageForDistributor)
        vapid?.let {
            broadcastIntent.putExtra(EXTRA_VAPID, it)
        }
        broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
        context.sendBroadcast(broadcastIntent)
    }

    @JvmStatic
    fun unregisterApp(context: Context, instance: String = INSTANCE_DEFAULT) {
        val store = Store(context)
        val distributor = getSavedDistributor(context) ?: run {
            store.removeInstances()
            store.removeDistributor()
            return
        }
        val token = store.tryGetToken(instance) ?: return
        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_UNREGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        store.removeInstance(instance, removeDistributor = true)
        context.sendBroadcast(broadcastIntent)
    }

    @JvmStatic
    fun getDistributors(
        context: Context
    ): List<String> {
        return (
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryBroadcastReceivers(
                    Intent(ACTION_REGISTER),
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
        ).mapNotNull {
            val packageName = it.activityInfo.packageName

            if (packageName == context.packageName &&
                hasEmbeddedFcmDistributor(context) &&
                !isPlayServicesAvailable(context) ) {
                return@mapNotNull null
            }
            if (it.activityInfo.exported || packageName == context.packageName) {
                Log.d(LOG_TAG, "Found distributor with package name $packageName")
                packageName
            } else {
                null
            }
        }
    }

    @JvmStatic
    fun saveDistributor(context: Context, distributor: String) {
        Store(context).saveDistributor(distributor)
    }

    /**
     * This function returns the distributor registered by the user,
     * but the distributor may not have sent a new endpoint yet
     */
    @JvmStatic
    fun getSavedDistributor(context: Context): String? = getDistributor(context, false)

    /**
     * This function returns the distributor registered by the user,
     * and the distributor has already sent a new endpoint
     */
    @JvmStatic
    fun getAckDistributor(context: Context): String? = getDistributor(context, true)

    @JvmStatic
    private fun getDistributor(context: Context, ack: Boolean): String? {
        val store = Store(context)
        if (ack && !store.distributorAck) {
            return null
        }
        return store.tryGetDistributor()?.let { distributor ->
            if (distributor in getDistributors(context)) {
                Log.d(LOG_TAG, "Found saved distributor.")
                distributor
            } else {
                Log.d(LOG_TAG, "There was a distributor, but it isn't installed anymore")
                store.forEachInstance {
                    broadcastLocalUnregistered(context, it)
                }
                null
            }
        }
    }

    @JvmStatic
    private fun broadcastLocalUnregistered(context: Context, instance: String) {
        val store = Store(context)
        val token = store.tryGetToken(instance) ?: return
        val broadcastIntent = Intent()
        broadcastIntent.`package` = context.packageName
        broadcastIntent.action = ACTION_UNREGISTERED
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        store.removeInstance(instance, removeDistributor = true)
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
            Log.v(LOG_TAG, e.message!!)
        }
        return false
    }

    @JvmStatic
    fun forceRemoveDistributor(context: Context) {
        val store = Store(context)
        store.removeInstances()
        store.removeDistributor()
    }
}
