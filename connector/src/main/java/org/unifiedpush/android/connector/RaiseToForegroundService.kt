package org.unifiedpush.android.connector

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class RaiseToForegroundService: Service() {
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private companion object {
        val binder = Binder()
    }
}