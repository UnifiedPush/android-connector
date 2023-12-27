package org.unifiedpush.android.connector

/**
 * Constants as defined on the specs
 * https://github.com/UnifiedPush/UP-spec/blob/main/specifications.md
 */

const val PREF_MASTER = "unifiedpush.connector"
const val PREF_MASTER_TOKEN = "unifiedpush.connector"
const val PREF_MASTER_INSTANCE = "unifiedpush.instances"
const val PREF_MASTER_DISTRIBUTOR = "unifiedpush.distributor"
const val PREF_MASTER_DISTRIBUTOR_ACK = "unifiedpush.distributor_ack"
const val PREF_MASTER_NO_DISTRIB_DIALOG_ACK = "unifiedpush.no_distrib_dialog"

const val ACTION_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
const val ACTION_REGISTRATION_FAILED = "org.unifiedpush.android.connector.REGISTRATION_FAILED"
const val ACTION_REGISTRATION_REFUSED = "org.unifiedpush.android.connector.REGISTRATION_REFUSED"
const val ACTION_UNREGISTERED = "org.unifiedpush.android.connector.UNREGISTERED"
const val ACTION_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"

const val ACTION_REGISTER = "org.unifiedpush.android.distributor.REGISTER"
const val ACTION_UNREGISTER = "org.unifiedpush.android.distributor.UNREGISTER"
const val ACTION_MESSAGE_ACK = "org.unifiedpush.android.distributor.MESSAGE_ACK"

const val EXTRA_APPLICATION = "application"
const val EXTRA_TOKEN = "token"
const val EXTRA_FEATURES = "features"
const val EXTRA_ENDPOINT = "endpoint"
const val EXTRA_MESSAGE = "message"
const val EXTRA_BYTES_MESSAGE = "bytesMessage"
const val EXTRA_MESSAGE_ID = "id"

const val INSTANCE_DEFAULT = "default"
const val WAKE_LOCK_TAG = "android-connector:lock"
const val LOG_TAG = "UnifiedPush"
