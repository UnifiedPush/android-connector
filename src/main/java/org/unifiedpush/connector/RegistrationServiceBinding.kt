package org.unifiedpush.connector

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.core.os.bundleOf

/**
 * This class is used to register to the distributor
 */

// TODO: find
private const val distributor_package = "com.flyingpanda.noprovider2push"
private const val registration_service_name = "$distributor_package.services.RegisterService"

data class Registration(val senderUid: Int, val endpoint: String)

interface RegistrationServiceHandler {
    fun onConnected(service: RegistrationServiceBinding)
    fun onRegistered(service: RegistrationServiceBinding, registration: Registration)
    fun onUnregistered(service: RegistrationServiceBinding)
}

class RegistrationServiceBinding(var context: Context, var bindingHandler: RegistrationServiceHandler){
    /** Messenger for communicating with service.  */
    private var messengerToDistributor: Messenger? = null
    /** To known if it if bound to the service */
    private var isBound = false
    private var waitingForInfo = false

    /**
     * Handler of incoming messages from service.
     */
    private class ReplyHandler(var service: RegistrationServiceBinding) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                TYPE_CONNECTOR_REGISTER_SUCCESS -> {
                    if(service.waitingForInfo) {
                        service.waitingForInfo = false
                        val endpoint = msg.data?.getString("endpoint").toString()
                        logi("new gateway: $endpoint")
                        service.bindingHandler.onRegistered(service,
                                Registration(msg.sendingUid,endpoint))
                    }
                }
                TYPE_CONNECTOR_UNREGISTER_ACKNOWLEDGE -> {
                    logi("App is unregistered")
                    service.bindingHandler.onUnregistered(service)
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val replyMessenger = Messenger(ReplyHandler(this))

    /**
     * Class for interacting with the main interface of the Registration service.
     */
    private val connectionToDistributor: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            messengerToDistributor = Messenger(service)
            isBound = true
            logi("Register Service connected")
        }

        override fun onServiceDisconnected(className: ComponentName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            messengerToDistributor = null
            unbindRemoteService()
            logi("Register Service disconnected")
        }
    }

    fun bindRemoteService() {
        val intent = Intent()
        intent.component = ComponentName(distributor_package , registration_service_name)
        context.bindService(intent, connectionToDistributor, Context.BIND_AUTO_CREATE)
    }

    fun unbindRemoteService() {
        if (isBound) {
            // Detach our existing connection.
            context.unbindService(connectionToDistributor)
            isBound = false
        }
    }

    fun registerApp(serviceName: String){
        if(!isBound){
            logw("Trying to register app without being bound to distributor registration service")
            return
        }
        try {
            val msg = Message.obtain(null,
                TYPE_CONNECTOR_REGISTER, 0, 0)
            msg.replyTo = replyMessenger
            // TODO: get this information without the bundle
            msg.data = bundleOf("package" to context.packageName, "service" to serviceName)
            waitingForInfo = true
            messengerToDistributor!!.send(msg)
        } catch (e: RemoteException) {
            waitingForInfo = false
            // There is nothing special we need to do if the service
            // has crashed.
            logw("The distributor registration service has crashed during registration")
        }
    }

    fun unregisterApp(){
        if(!isBound){
            logw("Trying to unregister app without being bound to distributor registration service")
            return
        }
        try {
            val msg = Message.obtain(null,
                TYPE_CONNECTOR_UNREGISTER, 0, 0)
            msg.replyTo = replyMessenger
            // TODO: this information without the bundle
            msg.data = bundleOf("package" to context.packageName)
            messengerToDistributor!!.send(msg)
        } catch (e: RemoteException) {
            // There is nothing special we need to do if the service
            // has crashed.
            logw("The distributor registration service has crashed during unregistration")
        }
    }
}