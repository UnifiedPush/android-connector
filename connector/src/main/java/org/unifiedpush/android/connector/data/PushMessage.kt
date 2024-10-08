package org.unifiedpush.android.connector.data

/**
 * Contains the push message. It has been correctly decrypted if [decrypted] is `true`.
 */
class PushMessage(
    /** Content of the push message. */
    val content: ByteArray,
    /** Whether it has been correctly decrypted. */
    val decrypted: Boolean,
)
