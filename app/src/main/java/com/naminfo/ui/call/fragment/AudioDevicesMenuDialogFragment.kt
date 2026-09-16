package com.naminfo.ui.call.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.naminfo.databinding.CallAudioDevicesBottomSheetBinding
import com.naminfo.ui.call.model.AudioDeviceModel

@UiThread
class AudioDevicesMenuDialogFragment(
    private val devicesList: List<AudioDeviceModel>,
    private val onDismiss: (() -> Unit)? = null
) : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "AudioDevicesMenuDialogFragment"
    }

    override fun onCancel(dialog: DialogInterface) {
        onDismiss?.invoke()
        super.onCancel(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDismiss?.invoke()
        super.onDismiss(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        // Makes sure all menu entries are visible,
        // required for landscape mode (otherwise only first item is visible)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = CallAudioDevicesBottomSheetBinding.inflate(layoutInflater)
        view.lifecycleOwner = viewLifecycleOwner

        for (device in devicesList) {
            device.dismissDialog = {
                dismiss()
            }
        }
        view.devices = devicesList

        return view.root
    }
}
