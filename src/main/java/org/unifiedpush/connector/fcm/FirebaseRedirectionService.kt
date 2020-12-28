package org.unifiedpush.connector.fcm

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.unifiedpush.connector.*

class FirebaseRedirectionService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d(LOG_TAG, "Firebase onNewToken $token")
        val intent = Intent()
        intent.action = ACTION_NEW_ENDPOINT
        intent.setPackage(baseContext.packageName)
        intent.putExtra(EXTRA_FCM_TOKEN, token)
        intent.putExtra(EXTRA_TOKEN, getToken(baseContext))
        baseContext.sendBroadcast(intent)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(LOG_TAG, "Firebase onMessageReceived ${message.messageId}")
        val intent = Intent()
        intent.action = ACTION_MESSAGE
        intent.setPackage(baseContext.packageName)
        // TODO: how best pass the message data?
        intent.putExtra(EXTRA_MESSAGE, message.rawData.toString())
        intent.putExtra(EXTRA_MESSAGE_ID, message.messageId)
        intent.putExtra(EXTRA_TOKEN, getToken(baseContext))
        baseContext.sendBroadcast(intent)
    }
}