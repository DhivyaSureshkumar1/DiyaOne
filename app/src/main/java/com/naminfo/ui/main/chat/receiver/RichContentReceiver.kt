package com.naminfo.ui.main.chat.receiver

import android.content.ClipData
import android.net.Uri
import android.view.View
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import org.linphone.core.tools.Log

class RichContentReceiver(private val contentReceived: (uri: Uri) -> Unit) :
    OnReceiveContentListener {
    companion object {
        private const val TAG = "[Rich Content Receiver]"

        val MIME_TYPES = arrayOf("image/png", "image/gif", "image/jpeg")
    }

    override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
        val (uriContent, remaining) = payload.partition { item -> item.uri != null }
        if (uriContent != null) {
            val clip: ClipData = uriContent.clip
            for (i in 0 until clip.itemCount) {
                val uri: Uri = clip.getItemAt(i).uri
                Log.i("$TAG Found URI: $uri")
                contentReceived(uri)
            }
        }
        // Return anything that your app didn't handle. This preserves the default platform
        // behavior for text and anything else that you aren't implementing custom handling for.
        return remaining
    }
}
