package com.naminfo.ui.call.viewmodel

import android.content.res.Configuration
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.window.layout.FoldingFeature
import com.naminfo.ui.GenericViewModel
import com.naminfo.utils.Event

class SharedCallViewModel
    @UiThread
    constructor() : GenericViewModel() {
    val toggleFullScreenEvent = MutableLiveData<Event<Boolean>>()

    val foldingState = MutableLiveData<FoldingFeature>()

    // For moving video preview purposes
    var videoPreviewX: Float = 0f
    var videoPreviewY: Float = 0f
    var videoPreviewOrientation: Int = Configuration.ORIENTATION_UNDEFINED
}
