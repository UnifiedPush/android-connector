package org.unifiedpush.android.connector

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.widget.TextView
import java.util.*

open class Registration {
    open fun registerApp(context: Context) {
        registerApp(context, INSTANCE_DEFAULT)
    }

    open fun registerApp(context: Context, instance: String) {
        val token = getToken(context, instance).let {
            if (it.isEmpty()) newToken(context, instance) else it
        }
        registerAppDistributor(context, getDistributor(context), token)
    }

    fun registerAppDistributor(context: Context, distributor: String, token: String) {
        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_REGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
        context.sendBroadcast(broadcastIntent)
    }

    open fun registerAppWithDialog(context: Context) {
        registerAppWithDialog(context, INSTANCE_DEFAULT)
    }

    open fun registerAppWithDialog(context: Context, instance: String) {
        registerAppWithDialogFromList(context, getDistributors(context)) { registerApp(context, instance) }
    }

    fun registerAppWithDialogFromList(
        context: Context,
        distributors: List<String>,
        registerFunc: (context: Context) -> Unit
    ) {
        when(distributors.size){
            0 -> {
                val message = TextView(context)
                val builder = AlertDialog.Builder(context)
                val s = SpannableString("You need to install a distributor for push notifications to work.\n" +
                        "More information here: https://unifiedpush.org/")
                Linkify.addLinks(s, Linkify.WEB_URLS)
                message.text = s
                message.movementMethod = LinkMovementMethod.getInstance()
                message.setPadding(16,16,16,16)
                builder.setTitle("No distributor found")
                builder.setView(message)
                builder.show()
            }
            1 -> {
                saveDistributor(context, distributors.first())
                registerFunc(context)
            }
            else ->{
                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setTitle("Choose a distributor")

                val distributorsArray = distributors.toTypedArray()
                builder.setItems(distributorsArray) { _, which ->
                    val distributor = distributorsArray[which]
                    saveDistributor(context, distributor)
                    Log.d("UP-Registration", "saving: $distributor")
                    registerFunc(context)
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    }

    open fun unregisterApp(context: Context) {
        unregisterApp(context, INSTANCE_DEFAULT)
    }

    open fun unregisterApp(context: Context, instance: String) {
        unregisterAppDistributor(context, getDistributor(context), instance)
    }

    fun unregisterAppDistributor(context: Context, distributor: String, instance: String) {
        val token = getToken(context, instance)
        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_UNREGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
        context.sendBroadcast(broadcastIntent)
    }

    fun getToken(context: Context, instance: String): String {
        return context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
            "$instance/$PREF_MASTER_TOKEN", null
        ) ?: ""
    }

    open fun newToken(context: Context, instance: String): String {
        val token = UUID.randomUUID().toString()
        saveToken(context, token, instance)
        return token
    }

    fun saveToken(context: Context, token: String, instance: String) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>().toMutableSet()
        if ( !instances.contains(instance) ){
            instances.add(instance)
        }
        prefs.edit().putStringSet(PREF_MASTER_INSTANCE, instances).commit()
        prefs.edit().putString("$instance/$PREF_MASTER_TOKEN", token).commit()
    }

    fun removeToken(context: Context, instance: String) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>().toMutableSet()
        instances.remove(instance)
        prefs.edit().putStringSet(PREF_MASTER_INSTANCE, instances)
        prefs.edit().remove("$instance/$PREF_MASTER_TOKEN").commit()
    }

    fun getInstance(context: Context, token: String): String? {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>().toMutableSet()
        instances.forEach {
            if (prefs.getString("$it/$PREF_MASTER_TOKEN","").equals(token)) {
                return it
            }
        }
        return null
    }

    open fun getDistributors(context: Context): List<String> {
        val intent = Intent()
        intent.action = ACTION_REGISTER
        return context.packageManager.queryBroadcastReceivers(intent, 0).mapNotNull {
            if (it.activityInfo.exported || it.activityInfo.packageName == context.packageName) {
                val packageName = it.activityInfo.packageName
                Log.d("UP-Registration", "Found distributor with package name $packageName")
                packageName
            } else {
                null
            }
        }
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

    fun safeRemoveDistributor(context: Context) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        prefs.getStringSet(PREF_MASTER_INSTANCE, null)
                ?: prefs.edit().remove(PREF_MASTER_DISTRIBUTOR).commit()
    }
}