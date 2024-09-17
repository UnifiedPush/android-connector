package org.unifiedpush.android.connector

enum class FailedReason {
    INTERNAL_ERROR, NETWORK, ACTION_REQUIRED, UNAUTH, VAPID_REQUIRED, DISTRIBUTOR_NOT_SAVED
}

fun String?.toFailedReason(): FailedReason {
    return when (this) {
        FAILED_REASON_NETWORK -> FailedReason.NETWORK
        FAILED_REASON_ACTION_REQUIRED -> FailedReason.ACTION_REQUIRED
        FAILED_REASON_UNAUTH -> FailedReason.UNAUTH
        FAILED_REASON_VAPID_REQUIRED -> FailedReason.VAPID_REQUIRED
        // This is a reason implemented by the lib, not in the spec because
        // it doesn't involve any interaction with the (missing) distributor
        "DISTRIBUTOR_NOT_SAVED" -> FailedReason.DISTRIBUTOR_NOT_SAVED
        else -> FailedReason.INTERNAL_ERROR
    }
}