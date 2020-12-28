package org.unifiedpush.connector

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import java.util.*

fun registerApp(context: Context): String {
    val token = getToken(context).let {
        if (it.isEmpty()) newToken(context) else it
    }

    val distributor = getDistributor(context)
    if (distributor != FCM_DISTRIBUTOR_NAME) {
        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_REGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
        context.sendBroadcast(broadcastIntent)
        return token
    }
    return ""
}

fun registerAppWithDialog(context: Context) {
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder.setTitle("Choose a distributor")

    val distributors = getDistributors(context).toTypedArray()
    builder.setItems(distributors) { _, which ->
        val distributor = distributors[which]
        saveDistributor(context, distributor)
        Log.d("CheckActivity", "distributor: $distributor")
        registerApp(context)
    }

    val dialog: AlertDialog = builder.create()
    dialog.show()
}


fun unregisterApp(context: Context) {
    val distributor = getDistributor(context)

    if (distributor != FCM_DISTRIBUTOR_NAME) {
        val token = getToken(context)
        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_UNREGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
        context.sendBroadcast(broadcastIntent)
    }
}

fun getToken(context: Context): String {
    return context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
        PREF_MASTER_TOKEN, ""
    ) ?: ""
}

fun newToken(context: Context): String {
    val token = UUID.randomUUID().toString()
    context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE).edit()
        .putString(PREF_MASTER_TOKEN, token).commit()
    return token
}

fun removeToken(context: Context) {
    context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE).edit()
        .remove(PREF_MASTER_TOKEN).commit()
}

fun getDistributors(context: Context): List<String> {
    val intent = Intent()
    intent.action = ACTION_REGISTER

    val fcm = if (GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    ) {
        listOf(FCM_DISTRIBUTOR_NAME)
    } else {
        listOf()
    }

    return context.packageManager.queryBroadcastReceivers(intent, 0).mapNotNull {
        val packageName = it.activityInfo.packageName
        Log.d("UnifiedPush-Registration", "Found distributor with package name $packageName")
        packageName
    } + fcm
}

fun saveDistributor(context: Context, distributor: String) {
    context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE).edit()
        .putString(PREF_MASTER_DISTRIBUTOR, distributor).commit()
}

fun getDistributor(context: Context): String {
    return context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
        PREF_MASTER_DISTRIBUTOR, ""
    ) ?: ""
}

fun removeDistributor(context: Context) {
    context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE).edit()
        .remove(PREF_MASTER_TOKEN).commit()
}