package org.unifiedpush.android.connector_fcm_added

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import org.unifiedpush.android.connector.*

const val FCM_DISTRIBUTOR_NAME = "Firebase Cloud Messaging"

fun registerApp(context: Context) {
    var distributor = getDistributor(context)

    val token = getToken(context).let {
        if (it.isEmpty()) newToken(context) else it
    }

    if (token.isEmpty()) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { _token ->
                Log.d("UP-Registration","token: $_token")
                saveToken(context,_token)
                registerApp(context)
            }
        return
    }

    if (distributor == FCM_DISTRIBUTOR_NAME) {
        distributor = context.packageName
    }

    registerAppDistributor(context,distributor,token)
}

fun registerAppWithDialog(context: Context){
    registerAppWithDialogFromList(context,
        getDistributors(context)
    )
}

fun unregisterApp(context: Context) {
    var distributor = getDistributor(context)

    if (distributor == FCM_DISTRIBUTOR_NAME) {
        distributor = context.packageName
    }
    unregisterAppDistributor(context,distributor)
}

fun getToken(context: Context): String {
    return org.unifiedpush.android.connector.getToken(context)
}

fun newToken(context: Context): String {
    if (getDistributor(context) == FCM_DISTRIBUTOR_NAME)
        return ""
    return org.unifiedpush.android.connector.newToken(context)
}

fun saveToken(context: Context, token: String) {
    org.unifiedpush.android.connector.saveToken(context,token)
}

fun removeToken(context: Context) {
    org.unifiedpush.android.connector.removeToken(context)
}

fun getDistributors(context: Context): List<String> {
    var distributors = org.unifiedpush.android.connector.getDistributors(context)
    distributors.drop(distributors.indexOf(context.packageName))
    val intent = Intent()
    intent.action = ACTION_REGISTER

    if (GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
        distributors += FCM_DISTRIBUTOR_NAME
    }
    return distributors
}

fun saveDistributor(context: Context, distributor: String){
    org.unifiedpush.android.connector.saveDistributor(context,distributor)
}

fun getDistributor(context: Context): String {
    return org.unifiedpush.android.connector.getDistributor(context)
}

fun removeDistributor(context: Context){
    org.unifiedpush.android.connector.removeDistributor(context)
}
