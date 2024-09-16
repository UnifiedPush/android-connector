package org.unifiedpush.android.connector

/**
 * Constants as defined on the specs
 * https://unifiedpush.org/developers/spec/android/
 */

internal const val PREF_MASTER = "unifiedpush.connector"
internal const val PREF_MASTER_TEMP_TOKEN = "unifiedpush.temp_token"
internal const val PREF_MASTER_AUTH = "unifiedpush.auth"
internal const val PREF_MASTER_INSTANCES = "unifiedpush.instances"
internal const val PREF_MASTER_DISTRIBUTOR = "unifiedpush.distributor"
internal const val PREF_MASTER_DISTRIBUTOR_ACK = "unifiedpush.distributor_ack"
internal const val PREF_MASTER_DISTRIBUTOR_AND2 = "unifiedpush.distributor_and_2"
internal const val PREF_MASTER_EVENT_COUNT = "unifiedpush.event_count"
internal const val PREF_MASTER_LAST_LINK = "unifiedpush.last_link"

internal const val PREF_CONNECTOR_TOKEN = "%s/unifiedpush.connector"
internal const val PREF_CONNECTOR_EVENT_COUNT = "%s/unifiedpush.event_count"
internal const val PREF_CONNECTOR_VAPID = "%s/unifiedpush.vapid"
internal const val PREF_CONNECTOR_MESSAGE = "%s/unifiedpush.message"
internal const val PREF_CONNECTOR_ACK = "%s/unifiedpush.ack"

const val ACTION_LINKED = "org.unifiedpush.android.connector.LINKED"
const val ACTION_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
const val ACTION_REGISTRATION_FAILED = "org.unifiedpush.android.connector.REGISTRATION_FAILED"
const val ACTION_UNREGISTERED = "org.unifiedpush.android.connector.UNREGISTERED"
const val ACTION_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"

const val ACTION_LINK = "org.unifiedpush.android.distributor.LINK"
const val ACTION_REGISTER = "org.unifiedpush.android.distributor.REGISTER"
const val ACTION_UNREGISTER = "org.unifiedpush.android.distributor.UNREGISTER"
const val ACTION_MESSAGE_ACK = "org.unifiedpush.android.distributor.MESSAGE_ACK"

const val EXTRA_APPLICATION = "application"
const val EXTRA_TOKEN = "token"
const val EXTRA_AUTH = "auth"
const val EXTRA_FEATURES = "features"
const val EXTRA_ENDPOINT = "endpoint"
const val EXTRA_MESSAGE_FOR_DISTRIB = "message"
const val EXTRA_REASON = "reason"
const val EXTRA_VAPID = "vapid"
const val EXTRA_BYTES_MESSAGE = "bytesMessage"
const val EXTRA_MESSAGE_ID = "id"

// const val FAILED_REASON_INTERNAL_ERROR = "INTERNAL_ERROR"
const val FAILED_REASON_NETWORK = "NETWORK"
const val FAILED_REASON_ACTION_REQUIRED = "ACTION_REQUIRED"
const val FAILED_REASON_UNAUTH = "UNAUTH"
const val FAILED_REASON_VAPID_REQUIRED = "VAPID_REQUIRED"

const val INSTANCE_DEFAULT = "default"
internal const val WAKE_LOCK_TAG = "android-connector:lock"
internal const val TAG = "UnifiedPush"
