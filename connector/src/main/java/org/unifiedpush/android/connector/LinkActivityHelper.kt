package org.unifiedpush.android.connector

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
    private val gRequestCode = (1..Int.MAX_VALUE).random()

    /**
     * Start distributor's link activity for result.
     *
     * @return `true` if the activity has been requested else no distributor can handle the request
     */
    fun startLinkActivityForResult(): Boolean {
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("unifiedpush://link")
            }
        resolveLinkActivityPackageName(activity, intent)?.let {
            Log.d(TAG, "Found activity for $it default=${it != "android"}")
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
     * You have to call [UnifiedPush.register] for all your registrations if this returns `true`.
     *
     * @return `true` if the [requestCode] matches the one of the request and the [resultCode]
     *  is OK and the [data] contains the PendingIntent to identify the distributor packageName.
     */
    fun onLinkActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        val isRequestCodeMatching = requestCode == gRequestCode
        val isResultCodeOK = resultCode == RESULT_OK
        if (isRequestCodeMatching && isResultCodeOK) {
            Log.d(TAG, "The deep link has correctly been proceeded")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra("pi", PendingIntent::class.java)
            } else {
                data?.getParcelableExtra<PendingIntent>("pi")
                // targetPackage has been renamed creatorPackage after SDK 17
            }?.targetPackage?.let {
                Log.d(TAG, "Using distributor $it.")
                val store =
                    Store(activity).apply {
                        saveDistributor(it)
                    }
                return true
            } ?: run {
                Log.d(TAG, "Could not find creator of pending intent")
                return false
            }
        } else {
            Log.d(TAG, "The deep link hasn't been proceeded. isRequestCodeMatching=$isRequestCodeMatching isResultCodeOK=$isResultCodeOK")
            return false
        }
    }

    internal companion object {
        private const val TAG = "UnifiedPush.Link"

        /**
         * Resolve the package name of the application able to open `unifiedpush://link`
         *
         * Do not use this function to use the default distributor,
         * call the exposed [tryUseDefaultDistributor][org.unifiedpush.android.connector.UnifiedPush.tryUseDefaultDistributor] function.
         *
         * @param [context] [Context] to request the package manager
         * @param [intent] [Intent] to pass an intent, else it creates one
         *
         * @return `null` if no application is able to open the link,
         * "android" if the system must ask what application should open the link,
         * or the package name of the only, or default, application which can open the link.
         */
        internal fun resolveLinkActivityPackageName(
            context: Context,
            intent: Intent? = null,
        ): String? {
            val lIntent =
                intent ?: Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("unifiedpush://link")
                }
            val pm = context.packageManager
            val resolveInfo = pm.resolveActivity(lIntent, PackageManager.MATCH_DEFAULT_ONLY)
            return resolveInfo?.activityInfo?.packageName
        }
    }
}
