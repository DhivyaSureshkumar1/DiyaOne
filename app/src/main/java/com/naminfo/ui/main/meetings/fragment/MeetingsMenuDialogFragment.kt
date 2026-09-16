package com.naminfo.ui.main.meetings.fragment

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
import com.naminfo.databinding.MeetingsListLongPressMenuBinding

@UiThread
class MeetingsMenuDialogFragment(
    private val showCancelActionInsteadOfDelete: Boolean,
    private val onDismiss: (() -> Unit)? = null,
    private val onDeleteMeeting: (() -> Unit)? = null
) : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "MeetingsMenuDialogFragment"
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
        val view = MeetingsListLongPressMenuBinding.inflate(layoutInflater)
        view.cancelInsteadOfDelete = showCancelActionInsteadOfDelete

        view.setDeleteClickListener {
            onDeleteMeeting?.invoke()
            dismiss()
        }

        return view.root
    }
}
