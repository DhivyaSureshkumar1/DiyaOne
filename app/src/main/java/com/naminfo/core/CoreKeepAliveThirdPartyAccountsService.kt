package com.naminfo.core

import org.linphone.core.*

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.MainThread
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.tools.Log

@MainThread
class CoreKeepAliveThirdPartyAccountsService : Service() {
    companion object {
        private const val TAG = "[Core Keep Alive Third Party Accounts Service]"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("$TAG Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("$TAG onStartCommand")
        coreContext.notificationsManager.onKeepAliveServiceStarted(this)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i("$TAG Task removed, doing nothing")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.i("$TAG onDestroy")
        coreContext.notificationsManager.onKeepAliveServiceDestroyed()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }
}
