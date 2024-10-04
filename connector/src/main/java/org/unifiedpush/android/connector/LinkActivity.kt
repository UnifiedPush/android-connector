package org.unifiedpush.android.connector

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * @hide
 *
 * Activity declared to start link activity onCreate and store the Auth token.
 *
 * It sends [LinkActivityHelper.startLinkActivityForResult] onCreate
 * and run [LinkActivityHelper.onLinkActivityResult] onActivityResult.
 *
 * It runs [callback] as soon as the interaction with the potential distributor
 * finished.
 */
internal class LinkActivity: Activity() {
    private val helper = LinkActivityHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!helper.startLinkActivityForResult()) {
            stop(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        stop(
            helper.onLinkActivityResult(requestCode, resultCode, data)
        )
    }

    private fun stop(success: Boolean) {
        callback(success)
        finish()
    }

    internal companion object {
        var callback: (Boolean) -> Unit = {}
    }
}