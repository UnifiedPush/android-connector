package org.unifiedpush.connector

/**
 * Constants as defined on the specs
 * https://raw.githubusercontent.com/UnifiedPush/UP-spec/main/constants.md
 */

const val TYPE_CONNECTOR_REGISTER = 0x11
const val TYPE_CONNECTOR_REGISTER_SUCCESS = 0x12
const val TYPE_CONNECTOR_REGISTER_FAILED = 0x10
const val TYPE_CONNECTOR_UNREGISTER = 0x21
const val TYPE_CONNECTOR_UNREGISTER_ACKNOWLEDGE = 0x22

const val TYPE_DISTRIBUTOR_MESSAGE = 0x91
const val TYPE_DISTRIBUTOR_MESSAGE_ACKNOWLEDGE = 0x92
const val TYPE_DISTRIBUTOR_ENDPOINT_CHANGED = 0xA1
const val TYPE_DISTRIBUTOR_UNREGISTERED = 0xB1

const val PREF_MASTER = "UP-lib"
const val PREF_MASTER_KEY_ID = "UP-lib_id"