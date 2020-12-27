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
                }
                this@MessagingReceiver.handler.onUnregistered(context)
            }
            MESSAGE -> {
                if (getToken(context!!) != intent.getStringExtra("token")){
                    return
                }
                val message = intent.getStringExtra("message")!!
                this@MessagingReceiver.handler.onMessage(context, message)
            }
        }
    }
}