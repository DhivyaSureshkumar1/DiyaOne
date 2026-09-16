package com.naminfo.ui.call.conference.fragment

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
import com.naminfo.databinding.CallConferenceLayoutBottomSheetBinding
import com.naminfo.ui.call.conference.viewmodel.ConferenceViewModel

@UiThread
class ConferenceLayoutMenuDialogFragment(
    val conferenceModel: ConferenceViewModel,
    private val onDismiss: (() -> Unit)? = null
) : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "ConferenceLayoutMenuDialogFragment"
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
        val view = CallConferenceLayoutBottomSheetBinding.inflate(layoutInflater)
        view.lifecycleOwner = viewLifecycleOwner

        view.viewModel = conferenceModel

        view.setGridClickListener {
            if (conferenceModel.participantDevices.value.orEmpty().size < 7) {
                conferenceModel.changeLayout(ConferenceViewModel.GRID_LAYOUT)
                dismiss()
            }
        }

        view.setActiveSpeakerClickListener {
            conferenceModel.changeLayout(ConferenceViewModel.ACTIVE_SPEAKER_LAYOUT)
            dismiss()
        }

        view.setAudioOnlyClickListener {
            conferenceModel.changeLayout(ConferenceViewModel.AUDIO_ONLY_LAYOUT)
            dismiss()
        }

        return view.root
    }
}
