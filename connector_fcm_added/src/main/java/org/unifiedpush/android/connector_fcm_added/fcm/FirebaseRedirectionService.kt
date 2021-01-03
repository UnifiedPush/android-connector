package org.unifiedpush.android.connector_fcm_added.fcm

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.unifiedpush.android.connector.ACTION_MESSAGE
import org.unifiedpush.android.connector.EXTRA_MESSAGE
import org.unifiedpush.android.connector.EXTRA_MESSAGE_ID
import org.unifiedpush.android.connector.EXTRA_TOKEN
import org.unifiedpush.android.connector_fcm_added.*

class FirebaseRedirectionService : FirebaseMessagingService() {
    private val up = RegistrationFCM()
    override fun onNewToken(token: String) {
        Log.d("UP-FCM", "Firebase onNewToken $token")
        if (up.getDistributor(baseContext) == FCM_DISTRIBUTOR_NAME) {
            up.saveToken(baseContext, token)
            up.registerApp(baseContext)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("UP-FCM", "Firebase onMessageReceived ${message.messageId}")
        val intent = Intent()
        intent.action = ACTION_MESSAGE
        intent.setPackage(baseContext.packageName)
        intent.putExtra(EXTRA_MESSAGE, message.notification?.body.toString())
        intent.putExtra(EXTRA_MESSAGE_ID, message.messageId)
        intent.putExtra(EXTRA_TOKEN, up.getToken(baseContext))
        baseContext.sendBroadcast(intent)
    }
}