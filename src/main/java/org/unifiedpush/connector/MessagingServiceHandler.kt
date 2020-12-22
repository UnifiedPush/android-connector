package org.unifiedpush.connector

import android.os.Handler
import android.os.Looper
import android.os.Message

/**
 * This handler is used to receive notifications
 * from the distributor (once registered)
 */

open class MessagingServiceHandler(looper: Looper) : Handler(looper) {

    override fun handleMessage(msg: Message) {
        if (!this.isTrusted(msg.sendingUid)) {
            logw("Message received from untrusted ID (${msg.sendingUid})")
            return;
        }
        /**
         * TODO:
         * - reply TYPE_DISTRIBUTOR_MESSAGE_ACKNOLOEDGE
         * - handle TPYE_DISTRIBUTOR_ENDPOINT_CHANGED
         * - handle TPYE_DISTRIBUTOR_ENDPOINT_UNREGISTERED
         */
        when (msg.what) {
            TYPE_DISTRIBUTOR_MESSAGE -> onMessage(
                msg.data?.getString("message")?: ""
            )
            else -> super.handleMessage(msg)
        }
    }

    open fun onMessage(message: String){}

    open fun isTrusted(uid: Int): Boolean {
        /** You  need to override this function
         * to control the master uid
         */
        return false
    }
}