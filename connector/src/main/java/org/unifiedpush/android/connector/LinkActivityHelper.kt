package org.unifiedpush.android.connector

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * Helper with functions to request the distributor's link activity for result and process the result
 *
 * ## Usage
 *
 * In your activity, define a new LinkActivityHelper, override onActivityResult to use
 * [onLinkActivityResult] then use [startLinkActivityForResult] to start activity on the
 * distributor.
 *
 * ```
 * class MyActivity: Activity() {
 *     /* ... */
 *     private val helper = LinkActivityHelper(this)
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         if (!helper.startLinkActivityForResult()) {
 *             // No distributor found
 *         }
 *     }
 *
 *     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
 *         if (helper.onLinkActivityResult(requestCode, resultCode, data)) {
 *             // The distributor is saved, you can request registrations with UnifiedPush.registerApp now
 *         } else {
 *            // An error occurred, consider no distributor found for the moment
 *         }
 *     }
 * /* ... */
 * }
 * ```
 */
class LinkActivityHelper(private val activity: Activity) {
    private val TAG = "UnifiedPush.Link"
    private val gRequestCode = (1..Int.MAX_VALUE).random()

    /**
     * Start distributor's link activity for result.
     *
     * @return `true` if the activity has been requested else no distributor can handle the request
     */
    fun startLinkActivityForResult(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("unifiedpush://link")
        }
        val pm = activity.packageManager
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.let {
            Log.d(TAG, "Found activity for ${it.activityInfo.packageName} default=${it.activityInfo.packageName != "android"}")
            activity.startActivityForResult(intent, gRequestCode)
            return true
        } ?: run {
            Log.d(TAG, "No activity found for deeplink")
        }
        return false
    }

    /**
     * Process result from the distributor's activity
     *
     * @return `true` if the [requestCode] matches the one of the request, if the [resultCode]
     *  is OK, and if the [data] contains the Auth token and the distributor packageName.
     */
    fun onLinkActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val isRequestCodeMatching = requestCode == gRequestCode
        val isResultCodeOK = resultCode == RESULT_OK
        if (isRequestCodeMatching && isResultCodeOK) {
            Log.d(TAG, "The deep link has correctly been proceeded")
            val authToken = data?.getStringExtra(EXTRA_AUTH_TOKEN)
            val application = data?.getStringExtra(EXTRA_APPLICATION)
            if (authToken != null && application != null) {
                Log.d(TAG, "Using distributor $application.")
                val store = Store(activity).apply {
                    saveDistributor(application)
                    this.authToken = authToken
                    distributorAck = true
                }
                UnifiedPush.registerEveryUnAckApp(activity, store)
                return true
            } else {
                Log.d(TAG, "Unexpected null extra")
            }
            return false
        } else {
            Log.d(TAG, "The deep link hasn't been proceeded. isRequestCodeMatching=$isRequestCodeMatching isResultCodeOK=$isResultCodeOK")
            return false
        }
    }
}