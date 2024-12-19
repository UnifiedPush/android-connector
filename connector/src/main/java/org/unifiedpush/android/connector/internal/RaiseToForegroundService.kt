package org.unifiedpush.android.connector.internal

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * @hide
 *
 * Service to raise the application to foreground as defined in the specifications
 *
 * The distributor can bind with foreground importance to this service for 5 seconds
 * to raise the application to the foreground, allowing it to start a foreground
 * service from the background during this 5 seconds
 */
class RaiseToForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private companion object {
        val binder = Binder()
    }
}
