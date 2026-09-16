package com.naminfo.core

import org.linphone.core.*

import android.content.Intent
import android.os.IBinder
import androidx.annotation.MainThread
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.tools.Log
import org.linphone.core.tools.service.CoreService

@MainThread
class CoreInCallService : CoreService() {
    companion object {
        private const val TAG = "[Core InCall Service]"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("$TAG Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("$TAG onStartCommand")
        coreContext.notificationsManager.onInCallServiceStarted(this)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("$TAG Task removed, doing nothing")

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.i("$TAG onDestroy")
        coreContext.notificationsManager.onInCallServiceDestroyed()

        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun createServiceNotificationChannel() {
        // Do nothing, app's Notifications Manager will do the job
    }

    override fun createServiceNotification() {
        // Do nothing, app's Notifications Manager will do the job
    }

    override fun showForegroundServiceNotification(isVideoCall: Boolean) {
        // Do nothing, app's Notifications Manager will do the job
    }

    override fun hideForegroundServiceNotification() {
        // Do nothing, app's Notifications Manager will do the job
    }
}
