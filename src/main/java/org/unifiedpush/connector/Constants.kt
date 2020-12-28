package org.unifiedpush.connector

/**
 * Constants as defined on the specs
 * https://raw.githubusercontent.com/UnifiedPush/UP-spec/main/constants.md
 */

const val LOG_TAG = "UnifiedPushConnector"

const val PREF_MASTER = "UP-lib"
const val PREF_MASTER_TOKEN = "UP-lib_token"
const val PREF_MASTER_DISTRIBUTOR = "UP-lib_distributor"

const val ACTION_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
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
const val EXTRA_FCM_TOKEN = "fcm_token"

const val FCM_DISTRIBUTOR_NAME = "__FCM__"