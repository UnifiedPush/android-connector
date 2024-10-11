package org.unifiedpush.android.connector.keys

internal interface WebPushKeysEntries {
    fun getOrNullWebPushKeys(): WebPushKeys? {
        return if (hasWebPushKeys()) getWebPushKeys()
        else null
    }
    fun getWebPushKeys(): WebPushKeys?
    fun genWebPushKeys(): WebPushKeys
    fun hasWebPushKeys(): Boolean
    fun deleteWebPushKeys()
}