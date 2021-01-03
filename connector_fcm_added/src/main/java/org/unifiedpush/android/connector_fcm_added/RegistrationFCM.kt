package org.unifiedpush.android.connector_fcm_added

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import org.unifiedpush.android.connector.*

const val FCM_DISTRIBUTOR_NAME = "Firebase Cloud Messaging"

class RegistrationFCM : Registration() {
    override fun registerApp(context: Context) {
        var distributor = getDistributor(context)

        val token = getToken(context).let {
            if (it.isEmpty()) newToken(context) else it
        }

        if (token.isEmpty()) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { _token ->
                Log.d("UP-Registration", "token: $_token")
                saveToken(context, _token)
                registerApp(context)
            }
            return
        }
        if (distributor == FCM_DISTRIBUTOR_NAME) {
            distributor = context.packageName
        }

        registerAppDistributor(context, distributor, token)
    }

    override fun registerAppWithDialog(context: Context) {
        registerAppWithDialogFromList(
            context,
            getDistributors(context)
        ) { registerApp(context) }
    }

    override fun unregisterApp(context: Context) {
        var distributor = getDistributor(context)

        if (distributor == FCM_DISTRIBUTOR_NAME) {
            distributor = context.packageName
        }
        unregisterAppDistributor(context, distributor)
    }

    override fun getDistributors(context: Context): List<String> {
        val distributors =
            super.getDistributors(context).toMutableList()
        distributors.remove(context.packageName)

        val intent = Intent()
        intent.action = ACTION_REGISTER

        if (GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        ) {
            distributors.add(FCM_DISTRIBUTOR_NAME)
        }
        return distributors
    }
}
