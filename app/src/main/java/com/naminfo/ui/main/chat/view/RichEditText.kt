package com.naminfo.ui.main.chat.view

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import org.linphone.core.tools.Log
import com.naminfo.ui.main.chat.receiver.RichContentReceiver
import com.naminfo.ui.main.viewmodel.SharedMainViewModel
import com.naminfo.utils.Event

/**
 * Allows for image input inside an EditText, usefull for keyboards with gif support for example.
 */
class RichEditText : AppCompatEditText {
    companion object {
        private const val TAG = "[Rich Edit Text]"
    }

    private var controlPressed = false

    private var sendListener: RichEditTextSendListener? = null

    constructor(context: Context) : super(context) {
        initReceiveContentListener()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initReceiveContentListener()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initReceiveContentListener()
    }

    fun setControlEnterListener(listener: RichEditTextSendListener) {
        sendListener = listener
    }

    private fun initReceiveContentListener() {
        ViewCompat.setOnReceiveContentListener(
            this,
            RichContentReceiver.MIME_TYPES,
            RichContentReceiver { uri ->
                Log.i("$TAG Received URI: $uri")
                val activity = context as Activity
                val sharedViewModel = activity.run {
                    ViewModelProvider(activity as ViewModelStoreOwner)[SharedMainViewModel::class.java]
                }
                sharedViewModel.richContentUri.value = Event(uri)
            }
        )

        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_CTRL_LEFT) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    controlPressed = true
                } else if (event.action == KeyEvent.ACTION_UP) {
                    controlPressed = false
                }
                false
            } else if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP && controlPressed) {
                sendListener?.onControlEnterPressedAndReleased()
                true
            } else {
                false
            }
        }
    }

    interface RichEditTextSendListener {
        fun onControlEnterPressedAndReleased()
    }
}
