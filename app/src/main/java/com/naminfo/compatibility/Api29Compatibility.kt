package com.naminfo.compatibility

import android.content.Intent
import android.net.InetAddresses.isNumericAddress
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.contentcapture.ContentCaptureContext
import android.view.contentcapture.ContentCaptureSession
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class Api29Compatibility {
    companion object {
        fun getMediaCollectionUri(isImage: Boolean, isVideo: Boolean, isAudio: Boolean): Uri {
            return when {
                isImage -> {
                    MediaStore.Images.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                }
                isVideo -> {
                    MediaStore.Video.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                }
                isAudio -> {
                    MediaStore.Audio.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                }
                else -> Uri.EMPTY
            }
        }

        fun extractLocusIdFromIntent(intent: Intent): String? {
            return intent.getStringExtra(Intent.EXTRA_LOCUS_ID)
        }

        fun setLocusIdInContentCaptureSession(root: View, conversationId: String) {
            val session: ContentCaptureSession? = root.contentCaptureSession
            if (session != null) {
                session.contentCaptureContext = ContentCaptureContext.forLocusId(conversationId)
            }
        }

        fun isIpAddress(string: String): Boolean {
            return isNumericAddress(string)
        }
    }
}
