package org.unifiedpush.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

interface MessagingReceiverHandler {
    fun onNewEndpoint(context: Context?, endpoint: String)
    fun onUnregistered(context: Context?)
    fun onUnregisteredAck(context: Context?)
    fun onMessage(context: Context?, message: String)
}

open class MessagingReceiver(private val handler: MessagingReceiverHandler) : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent!!.action) {
            NEW_ENDPOINT -> {
                if (getToken(context!!) != intent.getStringExtra("token")){
                    return
                }
                val endpoint = intent.getStringExtra("endpoint")!!
                this@MessagingReceiver.handler.onNewEndpoint(context, endpoint)
            }
            UNREGISTERED -> {
                intent.getStringExtra("token").let{
                    if(it.isNullOrEmpty())
                        this@MessagingReceiver.handler.onUnregisteredAck(context)
                    else {
                        if (getToken(context!!) != it){
                            return
                        }
                        this@MessagingReceiver.handler.onUnregistered(context)
                    }
                    removeToken(context!!)
                    removeDistributor(context!!)
                }
            }
            MESSAGE -> {
                if (getToken(context!!) != intent.getStringExtra("token")){
                    return
                }
                val message = intent.getStringExtra("message")!!
                val id = intent.getStringExtra("id")?: ""
                this@MessagingReceiver.handler.onMessage(context, message)
                acknowledgeMessage(context, id)
            }
        }
    }
}

private fun acknowledgeMessage(context: Context, id: String) {
    val token = getToken(context)!!
    val broadcastIntent = Intent()
    broadcastIntent.`package` = getDistributor(context)
    broadcastIntent.action = MESSAGE_ACK
    broadcastIntent.putExtra("token", token)
    broadcastIntent.putExtra("id", id)
    context.sendBroadcast(broadcastIntent)
}