package com.naminfo.compatibility

import android.app.Activity
import android.app.Notification
import android.app.PictureInPictureParams
import android.app.Service
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import org.linphone.core.tools.Log
import com.naminfo.utils.AppUtils

class Api28Compatibility {
    companion object {
        private const val TAG = "[API 28 Compatibility]"

        fun startServiceForeground(service: Service, id: Int, notification: Notification): Boolean {
            try {
                service.startForeground(
                    id,
                    notification
                )
                return true
            } catch (e: Exception) {
                Log.e("$TAG Can't start service as foreground! $e")
            }
            return false
        }

        fun enterPipMode(activity: Activity): Boolean {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(AppUtils.getPipRatio(activity))
                .build()
            try {
                if (!activity.enterPictureInPictureMode(params)) {
                    Log.e("$TAG Failed to enter PiP mode")
                } else {
                    Log.i("$TAG Entered PiP mode")
                    return true
                }
            } catch (e: Exception) {
                Log.e("$TAG Can't build PiP params: $e")
            }
            return false
        }

        fun getMediaCollectionUri(isImage: Boolean, isVideo: Boolean, isAudio: Boolean): Uri {
            return when {
                isImage -> {
                    MediaStore.Images.Media.getContentUri("external")
                }
                isVideo -> {
                    MediaStore.Video.Media.getContentUri("external")
                }
                isAudio -> {
                    MediaStore.Audio.Media.getContentUri("external")
                }
                else -> Uri.EMPTY
            }
        }

        fun hasTelecomManagerFeature(context: Context): Boolean {
            val hasFeature = context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_CONNECTION_SERVICE
            )
            Log.i("$TAG Feature [${PackageManager.FEATURE_CONNECTION_SERVICE}] is [${if (hasFeature) "available" else "not available"}]")
            return hasFeature
        }
    }
}
