package org.unifiedpush.android.connector

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import java.util.LinkedList

internal object ServiceConnection {
    private lateinit var mService: PushService
    private var binding: Boolean = false
    private var mBound: Boolean = false
    private var eventsQueue = LinkedList<Event>()

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "Service is connected")
            val binder = service as PushService.PushBinder
            mService = binder.getService()
            synchronized(this@ServiceConnection) {
                mBound = true
                binding = false
            }
            handlePendingEvents()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "Service is disconnected")
            synchronized(this@ServiceConnection) {
                mBound = false
                binding = false
            }
        }
    }

    private fun bind(context: Context) {
        val context = context.applicationContext
        Intent().apply {
            action = PushService.ACTION_PUSH_EVENT
            `package` = context.packageName
        }.also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun stop(context: Context) {
        val context = context.applicationContext
        context.unbindService(connection)
        mBound = false
    }

    sealed class Event {
        class NewEndpoint(val endpoint: PushEndpoint, val instance: String): Event()
        class Message(val message: PushMessage, val instance: String): Event()
        class RegistrationFailed(val reason: FailedReason, val instance: String): Event()
        class Unregistered(val instance: String): Event()

        fun handle() {
            when (this) {
                is Message -> mService.onMessage(message, instance)
                is NewEndpoint -> mService.onNewEndpoint(endpoint, instance)
                is RegistrationFailed -> mService.onRegistrationFailed(reason, instance)
                is Unregistered -> mService.onUnregistered(instance)
            }
        }
    }

    fun sendEvent(context: Context, event: Event) {
        var shouldBind = false
        var bound: Boolean
        synchronized(this) {
            bound = mBound
            if (!mBound) {
                eventsQueue.add(event)
                if (!binding) {
                    binding = true
                    shouldBind = true
                }
            }
        }
        if (shouldBind) {
            bind(context)
        } else if (bound) {
            event.handle()
        } else {
            Log.d(TAG, "Even has been added to the queue")
        }
    }

    private fun handlePendingEvents() {
        while (eventsQueue.isNotEmpty()) {
            eventsQueue.pop().handle()
        }
    }
}