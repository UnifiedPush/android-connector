package org.unifiedpush.android.connector_fcm_added

import android.content.Context
import android.content.Intent
import android.util.Log
import org.unifiedpush.android.connector.*

interface MessagingReceiverHandlerFCM : MessagingReceiverHandler {
    override fun onNewEndpoint(context: Context?, endpoint: String)
    override fun onUnregistered(context: Context?)
    override fun onMessage(context: Context?, message: String)
    fun getEndpoint(token: String): String
}

open class MessagingReceiverFCM(private val handler: MessagingReceiverHandlerFCM) :
    MessagingReceiver(handler) {
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context,intent)
        when (intent!!.action) {
            /**
             * FAKE DISTRIBTUOR FOR FCM here
             */
            ACTION_REGISTER -> {
                Log.d("UP-MessagingReceiver", "Fake Distributor register")
                val token = getToken(context!!)
                val broadcastIntent = Intent()
                broadcastIntent.`package` = context.packageName
                broadcastIntent.action = ACTION_NEW_ENDPOINT
                broadcastIntent.putExtra(EXTRA_ENDPOINT,
                    this@MessagingReceiverFCM.handler.getEndpoint(token))
                broadcastIntent.putExtra(EXTRA_TOKEN, token)
                context.sendBroadcast(broadcastIntent)
            }
             ACTION_UNREGISTER -> {
                 Log.d("UP-MessagingReceiver", "Fake Distributor unregister")
                 val token = getToken(context!!)
                 val broadcastIntent = Intent()
                 broadcastIntent.`package` = context.packageName
                 broadcastIntent.action = ACTION_UNREGISTERED
                 broadcastIntent.putExtra(EXTRA_TOKEN, token)
                 context.sendBroadcast(broadcastIntent)
            }
        }
    }
}