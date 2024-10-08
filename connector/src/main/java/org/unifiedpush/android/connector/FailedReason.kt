package org.unifiedpush.android.connector

/**
 * A [registration request][UnifiedPush.registerApp] may fail for different reasons.
 */
enum class FailedReason {
/**
 * This is a generic error type, you can try to register again directly.
 */
    INTERNAL_ERROR,
/**
 * The registration failed because of missing network connection, try again when network is back.
 */
    NETWORK,
/**
 * The distributor requires a user action to work. For instance, the distributor may be log out of the push server and requires the user to log in. The user must interact with the distributor or sending a new registration will fail again.
 */
    ACTION_REQUIRED,
/**
 * The distributor requires a VAPID key and you didn't provide one during [registration][UnifiedPush.registerApp].
 */
    VAPID_REQUIRED,
/**
 * The distributor is not installed anymore, you can inform the user about that and request to pick another one.
 */
    DISTRIBUTOR_NOT_SAVED
}

internal fun String?.toFailedReason(): FailedReason {
    return this?.let {
        try {
            FailedReason.valueOf(this)
        } catch (e: IllegalArgumentException) {
            FailedReason.INTERNAL_ERROR
        }
    } ?: FailedReason.INTERNAL_ERROR
}
