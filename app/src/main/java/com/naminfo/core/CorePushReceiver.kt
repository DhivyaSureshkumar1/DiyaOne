package com.naminfo.core

import org.linphone.core.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.linphone.core.tools.Log

class CorePushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("[Push Notification] Push notification has been received in broadcast receiver")
    }
}
