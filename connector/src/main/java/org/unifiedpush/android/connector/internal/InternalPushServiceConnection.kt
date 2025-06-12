package org.unifiedpush.android.connector.internal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.TAG
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import java.util.LinkedList
import java.util.Timer
import kotlin.concurrent.schedule

internal object InternalPushServiceConnection {

    /** Static instance of the [PushService] */
    private lateinit var mService: PushService

    /**
     * If we are binding to [PushService.PushBinder] to store the [PushService]
     * In this case, new event should be added to the [eventsQueue]
     */
    private var binding: Boolean = false

    /**
     * If [mService] has been connected
     */
    private var connected: Boolean = false

    /**
     * Queues of events, if we are binding to the service
     */
    private var eventsQueue = LinkedList<Event>()

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(TAG, "Service is connected")
            val binder = service as PushService.PushBinder
            mService = binder.getService()
            synchronized(this@InternalPushServiceConnection) {
                connected = true
                binding = false
            }
            handlePendingEvents()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "Service is disconnected")
            synchronized(this@InternalPushServiceConnection) {
                connected = false
                binding = false
            }
        }
    }

    /**
     * Bind to [PushService] with application context to store it in a static object,
     * and unbind 1 second later
     */
    private fun bind(context: Context) {
        Log.d(TAG, "Binding to PushService")
        val context = context.applicationContext
        Intent().apply {
            action = PushService.ACTION_PUSH_EVENT
            `package` = context.packageName
        }.also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        Timer().schedule(5_000L) {
            Log.d(TAG, "Unbinding")
            context.unbindService(connection)
        }
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
        var connected: Boolean
        synchronized(this) {
            connected = InternalPushServiceConnection.connected
            if (!connected) {
                eventsQueue.add(event)
                if (!binding) {
                    binding = true
                    shouldBind = true
                }
            }
        }
        if (connected) {
            event.handle()
        } else if (shouldBind) {
            bind(context)
        } else {
            Log.d(TAG, "Event has been added to the queue")
        }
    }

    private fun handlePendingEvents() {
        while (eventsQueue.isNotEmpty()) {
            eventsQueue.pop().handle()
        }
    }
}