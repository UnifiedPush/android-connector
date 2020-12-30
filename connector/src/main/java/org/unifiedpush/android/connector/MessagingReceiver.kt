package org.unifiedpush.android.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

interface MessagingReceiverHandler {
    fun onNewEndpoint(context: Context?, endpoint: String)
    fun onUnregistered(context: Context?)
    fun onMessage(context: Context?, message: String)
    fun getEndpoint(token: String): String
}

open class MessagingReceiver(private val handler: MessagingReceiverHandler) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (getToken(context!!) != intent!!.getStringExtra(EXTRA_TOKEN)) {
            return
        }
        when (intent!!.action) {
            ACTION_NEW_ENDPOINT -> {
                val endpoint = intent.getStringExtra(EXTRA_ENDPOINT)!!
                this@MessagingReceiver.handler.onNewEndpoint(context, endpoint)
            }
            ACTION_UNREGISTERED -> {
                this@MessagingReceiver.handler.onUnregistered(context)
                removeToken(context!!)
                removeDistributor(context!!)
            }
            ACTION_MESSAGE -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE)!!
                val id = intent.getStringExtra(EXTRA_MESSAGE_ID) ?: ""
                this@MessagingReceiver.handler.onMessage(context, message)
                acknowledgeMessage(context, id)
            }
            /**
             * FAKE DISTRIBTUOR FOR FCM here
             */
            ACTION_REGISTER -> {
                Log.d("UP-MessagingReceiver", "Fake Distributor register")
                val token = getToken(context)
                val broadcastIntent = Intent()
                broadcastIntent.`package` = context!!.packageName
                broadcastIntent.action = ACTION_NEW_ENDPOINT
                broadcastIntent.putExtra(EXTRA_ENDPOINT,
                    this@MessagingReceiver.handler.getEndpoint(token))
                broadcastIntent.putExtra(EXTRA_TOKEN, token)
                context.sendBroadcast(broadcastIntent)
            }
             ACTION_UNREGISTER -> {
                 Log.d("UP-MessagingReceiver", "Fake Distributor unregister")
                 val token = getToken(context)
                 val broadcastIntent = Intent()
                 broadcastIntent.`package` = context!!.packageName
                 broadcastIntent.action = ACTION_UNREGISTERED
                 broadcastIntent.putExtra(EXTRA_TOKEN, token)
                 context.sendBroadcast(broadcastIntent)
            }
        }
    }
}

private fun acknowledgeMessage(context: Context, id: String) {
    val token = getToken(context)!!
    val broadcastIntent = Intent()
    broadcastIntent.`package` = getDistributor(context)
    broadcastIntent.action = ACTION_MESSAGE_ACK
    broadcastIntent.putExtra(EXTRA_TOKEN, token)
    broadcastIntent.putExtra(EXTRA_MESSAGE_ID, id)
    context.sendBroadcast(broadcastIntent)
}
