package com.naminfo.compatibility

import android.Manifest
import android.app.ActivityOptions
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import org.linphone.core.tools.Log

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class Api33Compatibility {
    companion object {
        private const val TAG = "[API 33 Compatibility]"

        fun getAllRequiredPermissionsArray(): Array<String> {
            return arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        }

        fun isPostNotificationsPermissionGranted(context: Context): Boolean {
            return context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun getPendingIntentActivityOptions(): ActivityOptions {
            val options = ActivityOptions.makeBasic()
            options.isPendingIntentBackgroundActivityLaunchAllowed = true
            return options
        }

        fun hasTelecomManagerFeature(context: Context): Boolean {
            val hasFeature = context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_TELECOM
            )
            Log.i("$TAG Feature [${PackageManager.FEATURE_TELECOM}] is [${if (hasFeature) "available" else "not available"}]")
            return hasFeature
        }
    }
}
