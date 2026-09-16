package com.naminfo.ui.main.history.fragment

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
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.databinding.HistoryListLongPressMenuBinding

@UiThread
class HistoryMenuDialogFragment(
    private val contactExists: Boolean,
    private val onDismiss: (() -> Unit)? = null,
    private val onAddToContact: (() -> Unit)? = null,
    private val onGoToContact: (() -> Unit)? = null,
    private val onCopyNumberOrAddressToClipboard: (() -> Unit)? = null,
    private val onDeleteCallLog: (() -> Unit)? = null
) : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "HistoryMenuDialogFragment"
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
        val view = HistoryListLongPressMenuBinding.inflate(layoutInflater)
        view.contactExists = contactExists
        view.disableAddContact = corePreferences.disableAddContact

        view.setCopyNumberClickListener {
            onCopyNumberOrAddressToClipboard?.invoke()
            dismiss()
        }

        view.setDeleteClickListener {
            onDeleteCallLog?.invoke()
            dismiss()
        }

        view.setAddToContactsListener {
            onAddToContact?.invoke()
            dismiss()
        }

        view.setGoToContactClickListener {
            onGoToContact?.invoke()
            dismiss()
        }

        return view.root
    }
}
