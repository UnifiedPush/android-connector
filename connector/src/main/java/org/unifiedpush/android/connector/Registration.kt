package org.unifiedpush.android.connector

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*

fun registerApp(context: Context){
    val token = getToken(context).let {
        if (it.isEmpty()) newToken(context) else it
    }
    registerAppDistributor(context, getDistributor(context),token)
}

fun registerAppDistributor(context: Context, distributor: String, token: String) {
    val broadcastIntent = Intent()
    broadcastIntent.`package` = distributor
    broadcastIntent.action = ACTION_REGISTER
    broadcastIntent.putExtra(EXTRA_TOKEN, token)
    broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
    context.sendBroadcast(broadcastIntent)
}

fun registerAppWithDialog(context: Context){
    registerAppWithDialogFromList(context, getDistributors(context)) { registerApp(context) }
}

fun registerAppWithDialogFromList(context: Context, distributors: List<String>, registerFunc: (context: Context)-> Unit){
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)
    builder.setTitle("Choose a distributor")

    val distributorsArray = distributors.toTypedArray()
    builder.setItems(distributorsArray) { _, which ->
        val distributor = distributorsArray[which]
        saveDistributor(context, distributor)
        Log.d("CheckActivity","distributor: $distributor")
        registerFunc(context)
    }

    val dialog: AlertDialog = builder.create()
    dialog.show()
}

fun unregisterApp(context: Context){
    unregisterAppDistributor(context, getDistributor(context))
}

fun unregisterAppDistributor(context: Context, distributor: String) {
    val token = getToken(context)
    val broadcastIntent = Intent()
    broadcastIntent.`package` = distributor
    broadcastIntent.action = ACTION_UNREGISTER
    broadcastIntent.putExtra(EXTRA_TOKEN, token)
    broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
    context.sendBroadcast(broadcastIntent)
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

fun saveToken(context: Context, token: String) {
    context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE).edit()
        .putString(PREF_MASTER_TOKEN, token).commit()
}

fun removeToken(context: Context){
    context.getSharedPreferences(PREF_MASTER,Context.MODE_PRIVATE).edit()
        .remove(PREF_MASTER_TOKEN).commit()
}

fun getDistributors(context: Context): List<String> {
    val intent = Intent()
    intent.action = ACTION_REGISTER
    return context.packageManager.queryBroadcastReceivers(intent, 0).mapNotNull {
        val packageName = it.activityInfo.packageName
        Log.d("UP-Registration", "Found distributor with package name $packageName")
        packageName
    }
}

fun saveDistributor(context: Context, distributor: String){
    context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE).edit()
        .putString(PREF_MASTER_DISTRIBUTOR, distributor).commit()
}

fun getDistributor(context: Context): String {
    return context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
        PREF_MASTER_DISTRIBUTOR, ""
    ) ?: ""
}

fun removeDistributor(context: Context){
    context.getSharedPreferences(PREF_MASTER,Context.MODE_PRIVATE).edit()
        .remove(PREF_MASTER_TOKEN).commit()
}