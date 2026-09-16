package com.naminfo.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import java.io.FileNotFoundException
import com.naminfo.contacts.AvatarGenerator
import org.linphone.core.tools.Log
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap
import com.naminfo.R

class ImageUtils {
    companion object {
        private const val TAG = "[Image Utils]"

        @AnyThread
        fun generatedAvatarIfNeededAndReturnPath(context: Context, initials: String): String {
            val darkMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val suffix = if (darkMode) "_dark" else "_light"

            val generatedAvatarPath = FileUtils.getFileStorageCacheDir("$initials$suffix.png", overrideExisting = true)
            if (generatedAvatarPath.exists()) {
                val path = generatedAvatarPath.absolutePath
                return path
            }

            val builder = AvatarGenerator(context)
            builder.setInitials(initials)
            builder.setAvatarSize(AppUtils.getDimension(R.dimen.avatar_big_size).toInt())
            builder.setTextSize(AppUtils.getDimension(R.dimen.avatar_initials_call_text_size))

            val bitmap = builder.buildBitmap(false)
            val path = FileUtils.storeBitmap(bitmap, generatedAvatarPath)
            return path
        }

        @WorkerThread
        fun getBitmap(
            context: Context,
            path: String?,
            round: Boolean = false
        ): Bitmap? {
            Log.d("$TAG Trying to create Bitmap from path [$path]")
            if (path != null) {
                try {
                    val fromPictureUri = path.toUri()
                    // We make a copy to ensure Bitmap will be Software and not Hardware, required for shortcuts
                    val bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, fromPictureUri)
                    ).copy(
                        Bitmap.Config.ARGB_8888,
                        true
                    )
                    return if (round) {
                        getRoundBitmap(bitmap)
                    } else {
                        bitmap
                    }
                } catch (fnfe: FileNotFoundException) {
                    Log.e("$TAG File [$path] not found: $fnfe")
                    return null
                } catch (e: Exception) {
                    Log.e("$TAG Failed to get bitmap using path [$path]: $e")
                    return null
                }
            }

            Log.e("$TAG Can't get bitmap from null URI")
            return null
        }

        @AnyThread
        private fun getRoundBitmap(bitmap: Bitmap): Bitmap {
            val output = createBitmap(bitmap.width, bitmap.height)
            val canvas = Canvas(output)
            val color = -0xbdbdbe
            val paint = Paint()
            val rect =
                Rect(0, 0, bitmap.width, bitmap.height)
            paint.isAntiAlias = true
            canvas.drawARGB(0, 0, 0, 0)
            paint.color = color
            canvas.drawCircle(
                bitmap.width / 2.toFloat(),
                bitmap.height / 2.toFloat(),
                bitmap.width / 2.toFloat(),
                paint
            )
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }
    }
}
