package com.naminfo.ui.main.history.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.annotation.UiThread
import androidx.core.text.isDigitsOnly
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.R
import org.linphone.core.Address
import org.linphone.core.tools.Log
import com.naminfo.databinding.HistoryFragmentBinding
import com.naminfo.databinding.HistoryPopupMenuBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.main.fragment.SlidingPaneChildFragment
import com.naminfo.ui.main.history.adapter.ContactHistoryListAdapter
import com.naminfo.utils.ConfirmationDialogModel
import com.naminfo.ui.main.history.viewmodel.HistoryViewModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.Event

@UiThread
class HistoryFragment : SlidingPaneChildFragment() {
    companion object {
        private const val TAG = "[History Fragment]"
    }

    private lateinit var binding: HistoryFragmentBinding

    private lateinit var viewModel: HistoryViewModel

    private lateinit var adapter: ContactHistoryListAdapter

    private val args: HistoryFragmentArgs by navArgs()

    override fun goBack(): Boolean {
        sharedViewModel.closeSlidingPaneEvent.value = Event(true)

        if (findNavController().currentDestination?.id == R.id.historyFragment) {
            // If not done this fragment won't be paused, which will cause us issues
            val action = HistoryFragmentDirections.actionHistoryFragmentToEmptyFragment()
            findNavController().navigate(action)
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ContactHistoryListAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HistoryFragmentBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        val callId = args.callId
        Log.i("$TAG Looking up for call log with call id [$callId]")
        viewModel.findCallLogByCallId(callId)

        binding.callHistory.setHasFixedSize(true)
        binding.callHistory.layoutManager = LinearLayoutManager(requireContext())

        viewModel.callLogFoundEvent.observe(viewLifecycleOwner) {
            it.consume { found ->
                if (found) {
                    Log.i(
                        "$TAG Found matching call log for call ID [$callId]"
                    )
                    startPostponedEnterTransition()
                    sharedViewModel.openSlidingPaneEvent.value = Event(true)
                } else {
                    Log.e("$TAG Failed to find call log, going back")
                    goBack()
                }
            }
        }

        sharedViewModel.isSlidingPaneSlideable.observe(viewLifecycleOwner) { slideable ->
            viewModel.showBackButton.value = slideable
        }

        viewModel.historyCallLogs.observe(viewLifecycleOwner) {
            Log.i("$TAG Call history list ready with [${it.size}] items")
            adapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.callHistory.adapter != adapter) {
                binding.callHistory.adapter = adapter
            }
        }

        viewModel.historyDeletedEvent.observe(viewLifecycleOwner) {
            it.consume {
                sharedViewModel.forceRefreshCallLogsListEvent.value = Event(true)

                (requireActivity() as GenericActivity).showGreenToast(
                    getString(R.string.call_history_deleted_toast),
                    R.drawable.check
                )
                goBack()
            }
        }

        viewModel.goToConversationEvent.observe(viewLifecycleOwner) {
            it.consume { conversationId ->
                Log.i("$TAG Going to conversation [$conversationId]")
                sharedViewModel.showConversationEvent.value = Event(conversationId)
                sharedViewModel.navigateToConversationsEvent.value = Event(true)
            }
        }

        viewModel.goToMeetingConversationEvent.observe(viewLifecycleOwner) {
            it.consume { conversationId ->
                if (findNavController().currentDestination?.id == R.id.historyFragment) {
                    Log.i("$TAG Going to meeting conversation [$conversationId]")
                    val action =
                        HistoryFragmentDirections.actionHistoryFragmentToConferenceConversationFragment(
                            conversationId
                        )
                    findNavController().navigate(action)
                }
            }
        }

        viewModel.conferenceToJoinEvent.observe(viewLifecycleOwner) {
            it.consume { conferenceUri ->
                Log.i("$TAG Requesting to go to waiting room for conference URI [$conferenceUri]")
                sharedViewModel.goToMeetingWaitingRoomEvent.value = Event(conferenceUri)
            }
        }

        binding.setBackClickListener {
            goBack()
        }

        binding.setMenuClickListener {
            showPopupMenu()
        }

        binding.setCopyPeerSipUriClickListener {
            copyNumberOrAddressToClipboard(viewModel.callLogModel.value?.address)
        }
    }

    private fun copyNumberOrAddressToClipboard(address: Address?) {
        if (address != null) {
            val username = address.username.orEmpty()
            if (username.isNotEmpty() && (username.startsWith("+") || username.isDigitsOnly())) {
                Log.i("$TAG Adding phone number [$username] into clipboard")
                if (AppUtils.copyToClipboard(requireContext(), AppUtils.getString(R.string.phone_number), username)) {
                    (requireActivity() as GenericActivity).showGreenToast(
                        getString(R.string.phone_number_copied_to_clipboard_toast),
                        R.drawable.check
                    )
                }
            } else {
                val sipUri = address.asStringUriOnly()
                Log.i("$TAG Adding SIP address [$sipUri] into clipboard")
                if (AppUtils.copyToClipboard(requireContext(), AppUtils.getString(R.string.sip_address), sipUri)) {
                    (requireActivity() as GenericActivity).showGreenToast(
                        getString(R.string.sip_address_copied_to_clipboard_toast),
                        R.drawable.check
                    )
                }
            }
        }
    }

    private fun showPopupMenu() {
        val popupView: HistoryPopupMenuBinding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.history_popup_menu,
            null,
            false
        )

        val popupWindow = PopupWindow(
            popupView.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.contactExists = viewModel.callLogModel.value?.friendExists == true
        popupView.isConferenceCallLog = viewModel.isConferenceCallLog.value == true
        popupView.disableAddContact = corePreferences.disableAddContact

        popupView.setAddToContactsListener {
            val addressToAdd = viewModel.callLogModel.value?.displayedAddress.orEmpty()
            Log.i("$TAG Navigating to new contact with pre-filled value [$addressToAdd]")
            sharedViewModel.sipAddressToAddToNewContact = addressToAdd
            sharedViewModel.displayNameToSetToNewContact = viewModel.callLogModel.value?.avatarModel?.contactName.orEmpty()
            sharedViewModel.navigateToContactsEvent.value = Event(true)
            sharedViewModel.showNewContactEvent.value = Event(true)
            popupWindow.dismiss()
        }

        popupView.setGoToContactListener {
            val friendRefKey = viewModel.callLogModel.value?.friendRefKey
            if (!friendRefKey.isNullOrEmpty()) {
                Log.i("$TAG Navigating to contact with ref key [$friendRefKey]")
                sharedViewModel.navigateToContactsEvent.value = Event(true)
                sharedViewModel.showContactEvent.value = Event(friendRefKey)
            }
            popupWindow.dismiss()
        }

        popupView.setDeleteAllHistoryClickListener {
            showDeleteConfirmationDialog()
            popupWindow.dismiss()
        }

        popupView.setCopyNumberClickListener {
            popupWindow.dismiss()
            copyNumberOrAddressToClipboard(viewModel.callLogModel.value?.address)
        }

        // Elevation is for showing a shadow around the popup
        popupWindow.elevation = 20f
        popupWindow.showAsDropDown(binding.callDetailsMenu, 0, 0, Gravity.BOTTOM)
    }

    private fun showDeleteConfirmationDialog() {
        val model = ConfirmationDialogModel()
        val dialog = DialogUtils.getRemoveCallLogsConfirmationDialog(
            requireActivity(),
            model
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialog.dismiss()
            }
        }

        model.confirmEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.w(
                    "$TAG Removing call entries with [${viewModel.callLogModel.value?.address?.asStringUriOnly()}] from database"
                )
                viewModel.deleteHistory()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
