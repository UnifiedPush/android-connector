package org.unifiedpush.android.connector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*

abstract class MessagingReceiverHandler(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val up = Registration()

    abstract fun onNewEndpoint(context: Context?, endpoint: String, instance: String)
    abstract fun onRegistrationFailed(context: Context?, instance: String)
    abstract fun onRegistrationRefused(context: Context?, instance: String)
    abstract fun onUnregistered(context: Context?, instance: String)
    abstract fun onMessage(context: Context?, message: String, instance: String)

    override fun doWork(): Result {

        val token = inputData.getString(EXTRA_TOKEN)

        val instance = token?.let { up.getInstance(context, it) } ?: return Result.failure()

        when (inputData.getString(EXTRA_ACTION)) {
            ACTION_NEW_ENDPOINT -> {
                val endpoint = inputData.getString(EXTRA_ENDPOINT)!!
                onNewEndpoint(context, endpoint, instance)
            }
            ACTION_REGISTRATION_FAILED -> {
                val message = inputData.getString(EXTRA_MESSAGE) ?: "No reason supplied"
                Log.i("UP-registration", "Failed: $message")
                onRegistrationFailed(context, instance)
                up.removeToken(context, instance)
            }
            ACTION_REGISTRATION_REFUSED -> {
                val message = inputData.getString(EXTRA_MESSAGE) ?: "No reason supplied"
                Log.i("UP-registration", "Refused: $message")
                onRegistrationRefused(context, instance)
                up.removeToken(context, instance)
            }
            ACTION_UNREGISTERED -> {
                onUnregistered(context, instance)
                up.removeToken(context, instance)
                up.safeRemoveDistributor(context)
            }
            ACTION_MESSAGE -> {
                val message = inputData.getString(EXTRA_MESSAGE)!!
                val id = inputData.getString(EXTRA_MESSAGE_ID) ?: ""
                onMessage(context, message, instance)
                acknowledgeMessage(context, id, token)
            }
        }

        return Result.success()
    }

    private fun acknowledgeMessage(context: Context, id: String, token: String) {
        val broadcastIntent = Intent()
        broadcastIntent.`package` = up.getPrefDistributor(context)
        broadcastIntent.action = ACTION_MESSAGE_ACK
        broadcastIntent.putExtra(EXTRA_TOKEN, token)
        broadcastIntent.putExtra(EXTRA_MESSAGE_ID, id)
        context.sendBroadcast(broadcastIntent)
    }
}

open class MessagingReceiver(private val handlerClass: Class<out MessagingReceiverHandler>) :
    BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val data = Data.Builder()

        data.putString(EXTRA_ACTION, intent.action)
        data.putString(EXTRA_TOKEN, intent.getStringExtra(EXTRA_TOKEN))
        data.putString(EXTRA_ENDPOINT, intent.getStringExtra(EXTRA_ENDPOINT))
        data.putString(EXTRA_MESSAGE, intent.getStringExtra(EXTRA_MESSAGE))
        data.putString(EXTRA_MESSAGE_ID, intent.getStringExtra(EXTRA_MESSAGE_ID))

        val workRequest = OneTimeWorkRequest.Builder(handlerClass)
            .setInputData(data.build())
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
