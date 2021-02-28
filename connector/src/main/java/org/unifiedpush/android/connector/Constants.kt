package org.unifiedpush.android.connector

/**
 * Constants as defined on the specs
 * https://github.com/UnifiedPush/UP-spec/blob/main/specifications.md
 */

const val PREF_MASTER = "UP-lib"
const val PREF_MASTER_TOKEN = "UP-lib_token"
const val PREF_MASTER_INSTANCE = "UP-lib_instances"
const val PREF_MASTER_DISTRIBUTOR = "UP-lib_distributor"

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
const val EXTRA_ENDPOINT = "endpoint"
const val EXTRA_MESSAGE = "message"
const val EXTRA_MESSAGE_ID = "id"

const val INSTANCE_DEFAULT = "default"