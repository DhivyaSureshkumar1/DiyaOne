package com.naminfo.ui.call.fragment

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.naminfo.databinding.CallsListLongPressMenuBinding
import com.naminfo.ui.call.model.CallModel

class CallMenuDialogFragment(
    private val callModel: CallModel,
    private val onDismiss: (() -> Unit)? = null
) : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "CallMenuDialogFragment"
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
        val view = CallsListLongPressMenuBinding.inflate(layoutInflater)
        view.lifecycleOwner = viewLifecycleOwner

        view.setHangUpClickListener {
            callModel.hangUp()
            dismiss()
        }

        view.setPauseResumeClickListener {
            callModel.togglePauseResume()
            dismiss()
        }

        view.isPaused = callModel.isPaused.value == true

        return view.root
    }
}
