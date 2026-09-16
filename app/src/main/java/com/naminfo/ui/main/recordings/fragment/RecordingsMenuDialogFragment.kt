package com.naminfo.ui.main.recordings.fragment

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
import com.naminfo.databinding.RecordingsListLongPressMenuBinding

@UiThread
class RecordingsMenuDialogFragment(
    private val onDismiss: (() -> Unit)? = null,
    private val onShare: (() -> Unit)? = null,
    private val onExport: (() -> Unit)? = null,
    private val onDelete: (() -> Unit)? = null
) : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "RecordingsMenuDialogFragment"
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
        val view = RecordingsListLongPressMenuBinding.inflate(layoutInflater)

        view.setDeleteClickListener {
            onDelete?.invoke()
            dismiss()
        }

        view.setExportClickListener {
            onExport?.invoke()
            dismiss()
        }

        view.setShareClickListener {
            onShare?.invoke()
            dismiss()
        }

        return view.root
    }
}
