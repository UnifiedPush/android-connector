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
import kotlin.collections.ArrayList

object UnifiedPush {

    const val FEATURE_BYTES_MESSAGE = "org.unifiedpush.android.distributor.feature.BYTES_MESSAGE"

    @JvmStatic
    fun registerApp(context: Context,
                    instance: String = INSTANCE_DEFAULT,
                    features: ArrayList<String> = ArrayList(),
                    messageForDistributor: String = ""
    ) {
        val store = Store(context)
        val token = store.getToken(instance) ?:run {
            store.newToken(instance)
        }

        val distributor = store.getDistributor()

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
        val store = Store(context)
        val distributor = store.getDistributor()
        val token = store.getToken(instance)
        val broadcastIntent = Intent()
        broadcastIntent.`package` = distributor
        broadcastIntent.action = ACTION_UNREGISTER
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        store.removeInstance(instance)
        safeRemoveDistributor(context)
        context.sendBroadcast(broadcastIntent)
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
        Store(context).saveDistributor(distributor)
    }

    @JvmStatic
    fun getDistributor(context: Context): String {
        val store = Store(context)
        store.getDistributor()?.let { distributor ->
            if (distributor in getDistributors(context)) {
                Log.d(LOG_TAG,"Found saved distributor.")
                return distributor
            }
        }
        return ""
    }

    @JvmStatic
    fun safeRemoveDistributor(context: Context) {
        val store = Store(context)
        if (store.getInstances().isEmpty()) {
            store.removeDistributor()
        }
    }

    @JvmStatic
    fun forceRemoveDistributor(context: Context) {
        val store = Store(context)
        store.getInstances().forEach {
            store.removeInstance(it)
        }
        store.removeInstances()
        store.removeDistributor()
    }
}
