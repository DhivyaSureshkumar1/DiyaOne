package com.naminfo.compatibility

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Environment
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.linphone.core.tools.Log
import com.naminfo.utils.AppUtils

@RequiresApi(Build.VERSION_CODES.S)
class Api31Compatibility {
    companion object {
        private const val TAG = "[API 31 Compatibility]"

        fun enableAutoEnterPiP(activity: Activity, enable: Boolean) {
            try {
                activity.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(AppUtils.getPipRatio(activity))
                        .setAutoEnterEnabled(enable)
                        .build()
                )
                Log.i("$TAG PiP auto enter has been [${if (enable) "enabled" else "disabled"}]")
            } catch (iae: IllegalArgumentException) {
                Log.e("$TAG Can't set PiP params: $iae")
            } catch (ise: IllegalStateException) {
                Log.e("$TAG Can't set PiP params: $ise")
            }
        }

        fun setBlurRenderEffect(view: View) {
            val blurEffect = RenderEffect.createBlurEffect(16F, 16F, Shader.TileMode.MIRROR)
            view.setRenderEffect(blurEffect)
        }

        fun removeBlurRenderEffect(view: View) {
            view.setRenderEffect(null)
        }

        fun forceDarkMode(context: Context) {
            val uiManager = ContextCompat.getSystemService(context, UiModeManager::class.java)
            if (uiManager == null) {
                Log.e("$TAG Failed to get UiModeManager system service!")
            }
            uiManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
        }

        fun forceLightMode(context: Context) {
            val uiManager = ContextCompat.getSystemService(context, UiModeManager::class.java)
            if (uiManager == null) {
                Log.e("$TAG Failed to get UiModeManager system service!")
            }
            uiManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
        }

        fun setAutoLightDarkMode(context: Context) {
            val uiManager = ContextCompat.getSystemService(context, UiModeManager::class.java)
            if (uiManager == null) {
                Log.e("$TAG Failed to get UiModeManager system service!")
            }
            uiManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
        }

        fun getRecordingsDirectory(): String {
            return Environment.DIRECTORY_RECORDINGS
        }
    }
}
