package org.unifiedpush.android.connector.data

/**
 * Contains the push endpoint and the associated [PublicKeySet].
 */
class PushEndpoint(
    /** URL to push notifications to. */
    val url: String,
    /** Web Push public key set. */
    val pubKeySet: PublicKeySet?,
)
