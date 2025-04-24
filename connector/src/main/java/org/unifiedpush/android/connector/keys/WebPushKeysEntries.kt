package org.unifiedpush.android.connector.keys

import android.util.Log
import org.unifiedpush.android.connector.TAG

internal interface WebPushKeysEntries {
    fun getOrNullWebPushKeys(): WebPushKeys? {
        return if (hasWebPushKeys()) {
            Log.d(TAG, "getOrNullWebPushKeys: WebPushKeys found")
            getWebPushKeys()
        } else {
            Log.d(TAG, "getOrNullWebPushKeys: No webPushKeys found")
            null
        }
    }

    fun getWebPushKeys(): WebPushKeys?

    fun genWebPushKeys(): WebPushKeys

    fun hasWebPushKeys(): Boolean

    fun deleteWebPushKeys()
}
