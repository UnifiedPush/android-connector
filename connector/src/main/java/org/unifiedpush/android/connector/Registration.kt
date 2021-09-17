package org.unifiedpush.android.connector

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.widget.TextView
import java.util.*

open class Registration {
    fun registerApp(context: Context) {
        registerApp(context, INSTANCE_DEFAULT)
    }

    fun registerApp(context: Context, instance: String) {
        val token = getToken(context, instance).let {
            if (it.isEmpty()) newToken(context, instance) else it
        }

        val distributor = getPrefDistributor(context)

        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_REGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
        context.sendBroadcast(broadcastIntent)
    }

    fun registerAppWithDialog(context: Context) {
        registerAppWithDialog(context, INSTANCE_DEFAULT)
    }

    fun registerAppWithDialog(context: Context, instance: String) {

        if (getDistributor(context).isNotEmpty()) {
            registerApp(context, instance)
            return
        }

        val distributors = getDistributors(context)

        when(distributors.size) {
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
                registerApp(context, instance)
            }
            else ->{
                val builder: AlertDialog.Builder = AlertDialog.Builder(context)
                builder.setTitle("Choose a distributor")

                val distributorsArray = distributors.toTypedArray()
                val distributorsNameArray = distributorsArray.map {
                    try {
                        val ai = context.packageManager.getApplicationInfo(it, 0)
                        context.packageManager.getApplicationLabel(ai)
                    } catch (e: PackageManager.NameNotFoundException) {
                        it
                    } as String
                }.toTypedArray()
                builder.setItems(distributorsNameArray) { _, which ->
                    val distributor = distributorsArray[which]
                    saveDistributor(context, distributor)
                    Log.d("UP-Registration", "saving: $distributor")
                    registerApp(context, instance)
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    }

    fun unregisterApp(context: Context) {
        unregisterApp(context, INSTANCE_DEFAULT)
    }

    fun unregisterApp(context: Context, instance: String) {
        val distributor = getPrefDistributor(context)
        val token = getToken(context, instance)
        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_UNREGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
        context.sendBroadcast(broadcastIntent)
    }

    private fun getToken(context: Context, instance: String): String {
        return context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
            "$instance/$PREF_MASTER_TOKEN", null
        ) ?: ""
    }

    private fun newToken(context: Context, instance: String): String {
        val token = UUID.randomUUID().toString()
        saveToken(context, token, instance)
        return token
    }

    private fun saveToken(context: Context, token: String, instance: String) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>().toMutableSet()
        if ( !instances.contains(instance) ){
            instances.add(instance)
        }
        prefs.edit().putStringSet(PREF_MASTER_INSTANCE, instances).commit()
        prefs.edit().putString("$instance/$PREF_MASTER_TOKEN", token).commit()
    }

    internal fun removeToken(context: Context, instance: String) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>().toMutableSet()
        instances.remove(instance)
        prefs.edit().putStringSet(PREF_MASTER_INSTANCE, instances).commit()
        prefs.edit().remove("$instance/$PREF_MASTER_TOKEN").commit()
    }

    internal fun getInstance(context: Context, token: String): String? {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>().toMutableSet()
        instances.forEach {
            if (prefs.getString("$it/$PREF_MASTER_TOKEN","").equals(token)) {
                return it
            }
        }
        return null
    }

    fun getDistributors(context: Context): List<String> {
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
        val distributor = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
            PREF_MASTER_DISTRIBUTOR, null ) ?: return ""

        if (distributor in getDistributors(context)) {
            Log.d("UP-Registration","Found saved distributor.")
            return distributor
        }
        Log.d("UP-Registration","Saved distributor isn't accessible anymore: removing it.")
        forceRemoveDistributor(context)
        return ""
    }

    internal fun getPrefDistributor(context: Context): String {
        return context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
            PREF_MASTER_DISTRIBUTOR, null
        ) ?: ""
    }

    fun safeRemoveDistributor(context: Context) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        if (prefs.getStringSet(PREF_MASTER_INSTANCE, emptySet())?.isEmpty() != false)
                prefs.edit().remove(PREF_MASTER_DISTRIBUTOR).commit()
    }

    private fun forceRemoveDistributor(context: Context) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        prefs.edit().remove(PREF_MASTER_DISTRIBUTOR).commit()
    }
}
