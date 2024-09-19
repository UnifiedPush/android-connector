package org.unifiedpush.android.connector

enum class FailedReason {
    INTERNAL_ERROR, NETWORK, ACTION_REQUIRED, UNAUTH, VAPID_REQUIRED, DISTRIBUTOR_NOT_SAVED
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
