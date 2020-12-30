package org.unifiedpush.android.connector.fcm

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.unifiedpush.android.connector.*

class FirebaseRedirectionService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Log.d("FCM", "Firebase onNewToken $token")
        saveToken(baseContext,token)
        registerApp(baseContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Firebase onMessageReceived ${message.messageId}")
        val intent = Intent()
        intent.action = ACTION_MESSAGE
        intent.setPackage(baseContext.packageName)
        intent.putExtra(EXTRA_MESSAGE, message.notification?.body.toString())
        intent.putExtra(EXTRA_MESSAGE_ID, message.messageId)
        intent.putExtra(EXTRA_TOKEN, getToken(baseContext))
        baseContext.sendBroadcast(intent)
    }
}