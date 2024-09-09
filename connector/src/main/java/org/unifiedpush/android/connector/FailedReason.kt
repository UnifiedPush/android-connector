package org.unifiedpush.android.connector

enum class FailedReason {
    INTERNAL_ERROR, NETWORK, ACTION_REQUIRED, TOO_MANY_PENDING_REQUESTS, VAPID_REQUIRED
}

fun String?.toFailedReason(): FailedReason {
    return when (this) {
        FAILED_REASON_NETWORK -> FailedReason.NETWORK
        FAILED_REASON_ACTION_REQUIRED -> FailedReason.ACTION_REQUIRED
        FAILED_REASON_TOO_MANY_PENDING_REQUESTS -> FailedReason.TOO_MANY_PENDING_REQUESTS
        FAILED_REASON_VAPID_REQUIRED -> FailedReason.VAPID_REQUIRED
        else -> FailedReason.INTERNAL_ERROR
    }
}