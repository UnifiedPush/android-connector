package org.unifiedpush.android.connector

import android.annotation.SuppressLint
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
import kotlin.collections.ArrayList

object UnifiedPush {

    const val FEATURE_BYTES_MESSAGE = "org.unifiedpush.android.distributor.feature.BYTES_MESSAGE"

    @JvmStatic
    fun registerApp(context: Context,
                    instance: String = INSTANCE_DEFAULT,
                    features: ArrayList<String> = ArrayList(),
                    messageForDistributor: String = ""
    ) {
        val token = getToken(context, instance).let {
            if (it.isEmpty()) newToken(context, instance) else it
        }

        val distributor = getPrefDistributor(context)

        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_REGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_FEATURES, features)
        broadcastIntent.putExtra(EXTRA_MESSAGE, messageForDistributor)
        broadcastIntent.putExtra(EXTRA_APPLICATION, context.packageName)
        context.sendBroadcast(broadcastIntent)
    }

    @JvmStatic
    fun registerAppWithDialog(context: Context,
                              instance: String = INSTANCE_DEFAULT,
                              dialogMessage: String = "You need to install a distributor " +
                                      "for push notifications to work.\n" +
                                      "More information here:\n" +
                                      "https://unifiedpush.org/",
                              features: ArrayList<String> = ArrayList(),
                              messageForDistributor: String = ""
    ) {

        if (getDistributor(context).isNotEmpty()) {
            registerApp(context, instance)
            return
        }

        val distributors = getDistributors(context, features)

        when(distributors.size) {
            0 -> {
                val message = TextView(context)
                val builder = AlertDialog.Builder(context)
                val s = SpannableString(dialogMessage)
                Linkify.addLinks(s, Linkify.WEB_URLS)
                message.text = s
                message.movementMethod = LinkMovementMethod.getInstance()
                message.setPadding(32,32,32,32)
                builder.setTitle("No distributor found")
                builder.setView(message)
                builder.show()
            }
            1 -> {
                saveDistributor(context, distributors.first())
                registerApp(context, instance, features, messageForDistributor)
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
                    Log.d(LOG_TAG, "saving: $distributor")
                    registerApp(context, instance, features, messageForDistributor)
                }
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    }

    @JvmStatic
    fun unregisterApp(context: Context, instance: String = INSTANCE_DEFAULT) {
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

    @SuppressLint("MutatingSharedPrefs")
    private fun saveToken(context: Context, token: String, instance: String) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>().toMutableSet()
        if ( !instances.contains(instance) ){
            instances.add(instance)
        }
        prefs.edit().putStringSet(PREF_MASTER_INSTANCE, instances).commit()
        prefs.edit().putString("$instance/$PREF_MASTER_TOKEN", token).commit()
    }

    @SuppressLint("MutatingSharedPrefs")
    internal fun removeToken(context: Context, instance: String) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>().toMutableSet()
        instances.remove(instance)
        prefs.edit().putStringSet(PREF_MASTER_INSTANCE, instances).commit()
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

    @JvmStatic
    fun getDistributors(context: Context,
                        features: ArrayList<String> = ArrayList()
    ): List<String> {
        val distributors = mutableListOf<String>()
        val intent = Intent()
        intent.action = ACTION_REGISTER
        distributors.addAll(
            context.packageManager.queryBroadcastReceivers(intent,
                PackageManager.GET_RESOLVED_FILTER
            ).mapNotNull {
                val actions = mutableListOf<String>()
                val packageName = it.activityInfo.packageName
                it.filter?.let { filter ->
                    val actionIterator = filter.actionsIterator()
                    while (actionIterator.hasNext()) {
                        actions.add(actionIterator.next())
                    }
                }
                features.forEach {
                    feature -> if (feature !in actions){
                        Log.i(LOG_TAG, "Found distributor $packageName" +
                                " without feature $feature")
                        return@mapNotNull null 
                    } 
                }
                if (it.activityInfo.exported || packageName == context.packageName) {
                    Log.d(LOG_TAG, "Found distributor with package name $packageName")
                    packageName
                } else {
                    null
                }
            }
        )
        return distributors
    }

    @JvmStatic
    fun saveDistributor(context: Context, distributor: String) {
        context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE).edit()
            .putString(PREF_MASTER_DISTRIBUTOR, distributor).commit()
    }

    @JvmStatic
    fun getDistributor(context: Context): String {
        val distributor = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
            PREF_MASTER_DISTRIBUTOR, null ) ?: return ""

        if (distributor in getDistributors(context)) {
            Log.d(LOG_TAG,"Found saved distributor.")
            return distributor
        }
        Log.d(LOG_TAG,"Saved distributor isn't accessible anymore: removing it.")
        forceRemoveDistributor(context)
        return ""
    }

    internal fun getPrefDistributor(context: Context): String {
        return context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)?.getString(
            PREF_MASTER_DISTRIBUTOR, null
        ) ?: ""
    }

    @JvmStatic
    fun safeRemoveDistributor(context: Context) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        if (prefs.getStringSet(PREF_MASTER_INSTANCE, emptySet())?.isEmpty() != false)
                prefs.edit().remove(PREF_MASTER_DISTRIBUTOR).commit()
    }

    @JvmStatic
    fun forceRemoveDistributor(context: Context) {
        val prefs = context.getSharedPreferences(PREF_MASTER, Context.MODE_PRIVATE)
        val instances = prefs.getStringSet(PREF_MASTER_INSTANCE, null)?: emptySet<String>()
        instances.forEach {
            prefs.edit().remove("$it/$PREF_MASTER_TOKEN").commit()
        }
        prefs.edit().remove(PREF_MASTER_INSTANCE).commit()
        prefs.edit().remove(PREF_MASTER_DISTRIBUTOR).commit()
    }
}
