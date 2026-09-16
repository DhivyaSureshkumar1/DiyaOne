package com.naminfo.ui.main.chat.view

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView

@UiThread
class ChatBubbleTextView : AppCompatTextView {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        // Required for PatternClickableSpan
        movementMethod = LinkMovementMethod.getInstance()
    }
}
