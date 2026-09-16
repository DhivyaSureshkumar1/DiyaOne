package com.naminfo.ui.call.model

import androidx.annotation.WorkerThread
import org.linphone.core.AudioDevice

data class AudioDeviceModel
    @WorkerThread
    constructor(
    val audioDevice: AudioDevice,
    val name: String,
    val type: AudioDevice.Type,
    val isCurrentlySelected: Boolean,
    val isEnabled: Boolean,
    private val onAudioDeviceSelected: (() -> Unit)? = null
) {
    var dismissDialog: (() -> Unit)? = null

    fun onClicked() {
        onAudioDeviceSelected?.invoke()
        dismissDialog?.invoke()
    }
}
