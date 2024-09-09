package org.unifiedpush.android.connector

/**
 * Constants as defined on the specs
 * https://unifiedpush.org/developers/spec/android/
 */

internal const val PREF_MASTER = "unifiedpush.connector"
internal const val PREF_MASTER_TOKEN = "unifiedpush.connector"
internal const val PREF_MASTER_INSTANCE = "unifiedpush.instances"
internal const val PREF_MASTER_DISTRIBUTOR = "unifiedpush.distributor"
internal const val PREF_MASTER_DISTRIBUTOR_ACK = "unifiedpush.distributor_ack"

const val ACTION_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
const val ACTION_REGISTRATION_FAILED = "org.unifiedpush.android.connector.REGISTRATION_FAILED"
const val ACTION_UNREGISTERED = "org.unifiedpush.android.connector.UNREGISTERED"
const val ACTION_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"

const val ACTION_REGISTER = "org.unifiedpush.android.distributor.REGISTER"
const val ACTION_UNREGISTER = "org.unifiedpush.android.distributor.UNREGISTER"
const val ACTION_MESSAGE_ACK = "org.unifiedpush.android.distributor.MESSAGE_ACK"

const val EXTRA_APPLICATION = "application"
const val EXTRA_TOKEN = "token"
const val EXTRA_FEATURES = "features"
const val EXTRA_ENDPOINT = "endpoint"
const val EXTRA_MESSAGE_FOR_DISTRIB = "message"
const val EXTRA_REASON = "reason"
const val EXTRA_VAPID = "vapid"
const val EXTRA_BYTES_MESSAGE = "bytesMessage"
const val EXTRA_MESSAGE_ID = "id"

const val FAILED_REASON_INTERNAL_ERROR = "INTERNAL_ERROR"
const val FAILED_REASON_NETWORK = "NETWORK"
const val FAILED_REASON_ACTION_REQUIRED = "ACTION_REQUIRED"
const val FAILED_REASON_TOO_MANY_PENDING_REQUESTS = "TOO_MANY_PENDING_REQUESTS"
const val FAILED_REASON_VAPID_REQUIRED = "VAPID_REQUIRED"

const val INSTANCE_DEFAULT = "default"
internal const val WAKE_LOCK_TAG = "android-connector:lock"
internal const val LOG_TAG = "UnifiedPush"
