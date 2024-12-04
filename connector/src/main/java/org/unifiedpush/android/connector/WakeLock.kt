package org.unifiedpush.android.connector

import android.content.Context
import android.os.PowerManager

class WakeLock(context: Context) {
    private val lock = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            /*
             * 10 secs = timeout of 10 seconds that the system allows before considering
             * the receiver to be blocked and a candidate to be killed
             */
            acquire(10_000L) // 10 secs
        }
    }

    fun release() {
       lock?.let {
           if (it.isHeld) {
               it.release()
           }
       }
    }
}