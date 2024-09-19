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
internal const val PREF_CONNECTOR_PUBKEY = "%s/unifiedpush.webpush.pubkey"
internal const val PREF_CONNECTOR_PRIVKEY = "%s/unifiedpush.webpush.privkey"
internal const val PREF_CONNECTOR_AUTH = "%s/unifiedpush.webpush.auth"

internal const val ACTION_LINKED = "org.unifiedpush.android.connector.LINKED"
internal const val ACTION_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
internal const val ACTION_REGISTRATION_FAILED = "org.unifiedpush.android.connector.REGISTRATION_FAILED"
internal const val ACTION_UNREGISTERED = "org.unifiedpush.android.connector.UNREGISTERED"
internal const val ACTION_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"

internal const val ACTION_LINK = "org.unifiedpush.android.distributor.LINK"
internal const val ACTION_REGISTER = "org.unifiedpush.android.distributor.REGISTER"
internal const val ACTION_UNREGISTER = "org.unifiedpush.android.distributor.UNREGISTER"
internal const val ACTION_MESSAGE_ACK = "org.unifiedpush.android.distributor.MESSAGE_ACK"

internal const val EXTRA_APPLICATION = "application"
internal const val EXTRA_TOKEN = "token"
internal const val EXTRA_AUTH = "auth"
internal const val EXTRA_FEATURES = "features"
internal const val EXTRA_ENDPOINT = "endpoint"
internal const val EXTRA_MESSAGE_FOR_DISTRIB = "message"
internal const val EXTRA_REASON = "reason"
internal const val EXTRA_VAPID = "vapid"
internal const val EXTRA_BYTES_MESSAGE = "bytesMessage"
internal const val EXTRA_MESSAGE_ID = "id"

internal const val WAKE_LOCK_TAG = "android-connector:lock"
internal const val TAG = "UnifiedPush"

/**
 * Default instance used by the library during registration and unregistration
 * if not passed as an argument
 */
const val INSTANCE_DEFAULT = "default"
