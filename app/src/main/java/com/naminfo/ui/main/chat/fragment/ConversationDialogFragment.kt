package com.naminfo.ui.main.chat.fragment

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
import com.naminfo.databinding.ChatConversationLongPressMenuBinding

@UiThread
class ConversationDialogFragment(
    private val isMuted: Boolean,
    private val isGroup: Boolean,
    private val isReadOnly: Boolean,
    private val hasUnreadMessages: Boolean,
    private val onDismiss: (() -> Unit)? = null,
    private val onMarkConversationAsRead: (() -> Unit)? = null,
    private val onToggleMute: (() -> Unit)? = null,
    private val onCall: (() -> Unit)? = null,
    private val onDeleteConversation: (() -> Unit)? = null,
    private val onLeaveGroup: (() -> Unit)? = null
) : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "ConversationDialogFragment"
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
        val view = ChatConversationLongPressMenuBinding.inflate(layoutInflater)
        view.isMuted = isMuted
        view.isGroup = isGroup
        view.isReadOnly = isReadOnly
        view.hasUnreadMessages = hasUnreadMessages

        view.setMarkAsReadClickListener {
            onMarkConversationAsRead?.invoke()
            dismiss()
        }

        view.setToggleMuteClickListener {
            onToggleMute?.invoke()
            dismiss()
        }

        view.setCallClickListener {
            onCall?.invoke()
            dismiss()
        }

        view.setDeleteClickListener {
            onDeleteConversation?.invoke()
            dismiss()
        }

        view.setLeaveClickListener {
            onLeaveGroup?.invoke()
            dismiss()
        }

        return view.root
    }
}
