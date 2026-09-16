package com.naminfo.compatibility

import android.app.ActivityOptions
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import org.linphone.core.tools.Log
import androidx.core.net.toUri

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Api34Compatibility {
    companion object {
        private const val TAG = "[API 34 Compatibility]"

        fun startServiceForeground(
            service: Service,
            id: Int,
            notification: Notification,
            foregroundServiceType: Int
        ): Boolean {
            try {
                service.startForeground(
                    id,
                    notification,
                    foregroundServiceType
                )
                return true
            } catch (e: Exception) {
                Log.e("$TAG Can't start service as foreground! $e")
            }
            return false
        }

        fun hasFullScreenIntentPermission(context: Context): Boolean {
            val notificationManager = context.getSystemService(NotificationManager::class.java) as NotificationManager
            // See https://developer.android.com/reference/android/app/NotificationManager#canUseFullScreenIntent%28%29
            val granted = notificationManager.canUseFullScreenIntent()
            if (granted) {
                Log.i("$TAG Full screen intent permission is granted")
            } else {
                Log.w("$TAG Full screen intent permission isn't granted yet!")
            }
            return granted
        }

        fun requestFullScreenIntentPermission(context: Context) {
            val intent = Intent()
            // See https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
            intent.action = Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT
            intent.data = "package:${context.packageName}".toUri()
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            Log.i("$TAG Starting ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT")
            try {
                context.startActivity(intent, null)
            } catch (anfe: ActivityNotFoundException) {
                Log.e("$TAG Failed to start intent for granting full screen intent permission: $anfe")
            }
        }

        fun sendPendingIntent(pendingIntent: PendingIntent, bundle: Bundle) {
            pendingIntent.send(bundle)
        }

        fun getPendingIntentActivityOptions(creator: Boolean): ActivityOptions {
            val options = ActivityOptions.makeBasic().apply {
                if (creator) {
                    pendingIntentCreatorBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                } else {
                    pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
            }
            return options
        }
    }
}
