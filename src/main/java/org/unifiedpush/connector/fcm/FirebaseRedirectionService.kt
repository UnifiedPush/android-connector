package org.unifiedpush.connector.fcm

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.unifiedpush.connector.*

class FirebaseRedirectionService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        val intent = Intent()
        intent.action = ACTION_NEW_ENDPOINT
        intent.setPackage(baseContext.packageName)
        intent.putExtra(EXTRA_FCM_TOKEN, token)
        baseContext.sendBroadcast(intent)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val intent = Intent()
        intent.action = ACTION_MESSAGE
        intent.setPackage(baseContext.packageName)
        // TODO: how best pass the message data?
        intent.putExtra(EXTRA_MESSAGE, message.rawData)
        intent.putExtra(EXTRA_MESSAGE_ID, message.messageId)
        baseContext.sendBroadcast(intent)
    }
}