package com.naminfo.ui.call.conference.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import androidx.core.view.doOnLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import org.linphone.core.Participant
import org.linphone.core.tools.Log
import com.naminfo.databinding.CallConferenceParticipantsListFragmentBinding
import com.naminfo.databinding.CallConferenceParticipantsListPopupMenuBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.call.adapter.ConferenceParticipantsListAdapter
import com.naminfo.ui.call.fragment.GenericCallFragment
import com.naminfo.ui.call.viewmodel.CurrentCallViewModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.ConfirmationDialogModel
import com.naminfo.utils.DialogUtils

class ConferenceParticipantsListFragment : GenericCallFragment() {
    companion object {
        private const val TAG = "[Conference Participants List Fragment]"
    }

    private lateinit var binding: CallConferenceParticipantsListFragmentBinding

    private lateinit var viewModel: CurrentCallViewModel

    private lateinit var adapter: ConferenceParticipantsListAdapter

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (findNavController().currentDestination?.id == R.id.conferenceAddParticipantsFragment) {
            // Holds fragment in place while new fragment slides over it
            return AnimationUtils.loadAnimation(activity, R.anim.hold)
        }
        return super.onCreateAnimation(transit, enter, nextAnim)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConferenceParticipantsListAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CallConferenceParticipantsListFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = requireActivity().run {
            ViewModelProvider(this)[CurrentCallViewModel::class.java]
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)
        observeToastEvents(viewModel.conferenceModel)

        binding.participantsList.setHasFixedSize(true)
        binding.participantsList.layoutManager = LinearLayoutManager(requireContext())
        binding.participantsList.outlineProvider = outlineProvider
        binding.participantsList.clipToOutline = true

        binding.setBackClickListener {
            findNavController().popBackStack()
        }

        binding.setAddParticipantsClickListener {
            if (findNavController().currentDestination?.id == R.id.conferenceParticipantsListFragment) {
                val action =
                    ConferenceParticipantsListFragmentDirections.actionConferenceParticipantsListFragmentToConferenceAddParticipantsFragment()
                findNavController().navigate(action)
            }
        }

        binding.setShowMenuClickListener {
            showPopupMenu(binding.showMenu)
        }

        viewModel.conferenceModel.participants.observe(viewLifecycleOwner) {
            Log.i("$TAG participants list updated with [${it.size}] items")
            adapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.participantsList.adapter != adapter) {
                binding.participantsList.adapter = adapter
            }
        }

        viewModel.conferenceModel.removeParticipantEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val displayName = pair.first
                val participant = pair.second
                showKickParticipantDialog(displayName, participant)
            }
        }

        viewModel.isSendingVideo.observe(viewLifecycleOwner) { sending ->
            coreContext.postOnCoreThread { core ->
                core.nativePreviewWindowId = if (sending) {
                    Log.i("$TAG We are sending video, setting capture preview surface")
                    binding.localPreviewVideoSurface
                } else {
                    Log.i("$TAG We are not sending video, clearing capture preview surface")
                    null
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        (binding.root as? ViewGroup)?.doOnLayout {
            setupVideoPreview(binding.localPreviewVideoSurface)
        }
    }

    override fun onPause() {
        super.onPause()

        cleanVideoPreview(binding.localPreviewVideoSurface)
    }

    private fun showKickParticipantDialog(displayName: String, participant: Participant) {
        val model = ConfirmationDialogModel()
        val dialog = DialogUtils.getKickConferenceParticipantConfirmationDialog(
            requireActivity(),
            model,
            displayName
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialog.dismiss()
            }
        }

        model.confirmEvent.observe(viewLifecycleOwner) {
            it.consume {
                coreContext.postOnCoreThread {
                    viewModel.conferenceModel.kickParticipant(participant)
                }
                val message = getString(R.string.conference_participant_was_kicked_out_toast)
                val icon = R.drawable.check
                (requireActivity() as GenericActivity).showGreenToast(message, icon)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showPopupMenu(view: View) {
        val popupView: CallConferenceParticipantsListPopupMenuBinding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.call_conference_participants_list_popup_menu,
            null,
            false
        )

        val popupWindow = PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.setShareInvitationClickListener {
            val sipUri = viewModel.conferenceModel.sipUri.value.orEmpty()
            if (sipUri.isNotEmpty()) {
                Log.i("$TAG Sharing conference SIP URI [$sipUri]")
                val label = "Conference SIP address"
                AppUtils.copyToClipboard(requireContext(), label, sipUri)
            }

            popupWindow.dismiss()
        }

        // Elevation is for showing a shadow around the popup
        popupWindow.elevation = 20f
        popupWindow.showAsDropDown(view, 0, 0, Gravity.BOTTOM)
    }
}
