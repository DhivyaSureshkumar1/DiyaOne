package com.naminfo.ui.main.history.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.UiThread
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import org.linphone.core.Address
import org.linphone.core.tools.Log
import com.naminfo.databinding.HistoryListFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.main.fragment.AbstractMainFragment
import com.naminfo.ui.main.history.adapter.HistoryListAdapter
import com.naminfo.ui.main.history.model.CallLogModel
import com.naminfo.utils.ConfirmationDialogModel
import com.naminfo.ui.main.history.viewmodel.HistoryListViewModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.Event
import com.naminfo.utils.LinphoneUtils
import com.naminfo.utils.RecyclerViewHeaderDecoration

@UiThread
class HistoryListFragment : AbstractMainFragment() {
    companion object {
        private const val TAG = "[History List Fragment]"
    }

    private lateinit var binding: HistoryListFragmentBinding

    private lateinit var listViewModel: HistoryListViewModel

    private lateinit var adapter: HistoryListAdapter

    private var bottomSheetDialog: BottomSheetDialogFragment? = null

    override fun onDefaultAccountChanged() {
        Log.i(
            "$TAG Default account changed, updating avatar in top bar & re-computing call logs"
        )
        listViewModel.filter()
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (findNavController().currentDestination?.id == R.id.startCallFragment ||
            findNavController().currentDestination?.id == R.id.meetingWaitingRoomFragment
        ) {
            // Holds fragment in place while new fragment slides over it
            return AnimationUtils.loadAnimation(activity, R.anim.hold)
        }
        return super.onCreateAnimation(transit, enter, nextAnim)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = HistoryListAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HistoryListFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listViewModel = ViewModelProvider(this)[HistoryListViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = listViewModel
        observeToastEvents(listViewModel)

        binding.historyList.setHasFixedSize(true)
        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.outlineProvider = outlineProvider
        binding.historyList.clipToOutline = true

        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), adapter)
        binding.historyList.addItemDecoration(headerItemDecoration)

        adapter.callLogLongClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                val modalBottomSheet = HistoryMenuDialogFragment(
                    model.friendExists,
                    { // onDismiss
                        adapter.resetSelection()
                    },
                    { // onAddToContact
                        val addressToAdd = model.displayedAddress
                        Log.i(
                            "$TAG Navigating to new contact with pre-filled value [$addressToAdd]"
                        )

                        sharedViewModel.sipAddressToAddToNewContact = addressToAdd
                        sharedViewModel.displayNameToSetToNewContact = model.avatarModel.contactName.orEmpty()
                        sharedViewModel.navigateToContactsEvent.value = Event(true)
                        sharedViewModel.showNewContactEvent.value = Event(true)
                    },
                    { // onGoToContact
                        val friendRefKey = model.friendRefKey
                        if (!friendRefKey.isNullOrEmpty()) {
                            Log.i("$TAG Navigating to contact with ref key [$friendRefKey]")

                            sharedViewModel.navigateToContactsEvent.value = Event(true)
                            sharedViewModel.showContactEvent.value = Event(friendRefKey)
                        } else {
                            Log.w(
                                "$TAG Can't navigate to existing friend, ref key is null or empty"
                            )
                        }
                    },
                    { // onCopyNumberOrAddressToClipboard
                        copyNumberOrAddressToClipboard(model.address)
                    },
                    { // onDeleteCallLog
                        showDeleteConfirmationDialog(model)
                    }
                )
                modalBottomSheet.show(parentFragmentManager, HistoryMenuDialogFragment.TAG)
                bottomSheetDialog = modalBottomSheet
            }
        }

        adapter.callLogClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                val uri = model.id
                Log.i("$TAG Show details for call log with ID [$uri]")
                if (!uri.isNullOrEmpty()) {
                    val navController = binding.historyNavContainer.findNavController()
                    val action =
                        HistoryFragmentDirections.actionGlobalHistoryFragment(uri)
                    navController.navigate(action)
                }
            }
        }

        adapter.callLogCallBackClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                coreContext.postOnCoreThread { core ->
                    val conferenceInfo = core.findConferenceInformationFromUri(model.address)
                    if (conferenceInfo != null) {
                        Log.i(
                            "$TAG Going to waiting room for conference [${conferenceInfo.subject}]"
                        )
                        sharedViewModel.goToMeetingWaitingRoomEvent.postValue(
                            Event(model.address.asStringUriOnly())
                        )
                    } else {
                        Log.i("$TAG Starting call to [${model.address.asStringUriOnly()}]")
                        coreContext.startAudioCall(model.address)
                    }
                }
            }
        }

        adapter.callFriendClickedEvent.observe(viewLifecycleOwner) {
            it.consume { friend ->
                coreContext.postOnCoreThread {
                    val preferredAddress = LinphoneUtils.getFirstAvailableAddressForFriend(friend)
                    if (preferredAddress != null) {
                        Log.i(
                            "$TAG Calling preferred address for contact [${friend.name}] directly"
                        )
                        coreContext.startAudioCall(preferredAddress)
                    }
                }
            }
        }

        adapter.callAddressClickedEvent.observe(viewLifecycleOwner) {
            it.consume { address ->
                Log.i("$TAG Starting call to [${address.asStringUriOnly()}]")
                coreContext.startAudioCall(address)
            }
        }

        listViewModel.callLogs.observe(viewLifecycleOwner) {
            adapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.historyList.adapter != adapter) {
                binding.historyList.adapter = adapter
            }

            Log.i("$TAG Call logs ready with [${it.size}] items")
            listViewModel.fetchInProgress.value = false
        }

        listViewModel.historyInsertedEvent.observe(viewLifecycleOwner) {
            it.consume {
                // Scroll to top to display latest call log
                binding.historyList.scrollToPosition(0)
            }
        }

        listViewModel.historyDeletedEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.w("$TAG All call logs have been deleted")
                (requireActivity() as GenericActivity).showGreenToast(
                    getString(R.string.call_history_deleted_toast),
                    R.drawable.check
                )
            }
        }

        sharedViewModel.forceRefreshCallLogsListEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Re-compute call log history")
                listViewModel.filter()
            }
        }

        sharedViewModel.goToMeetingWaitingRoomEvent.observe(viewLifecycleOwner) {
            it.consume { uri ->
                if (findNavController().currentDestination?.id == R.id.historyListFragment) {
                    Log.i("$TAG Navigating to meeting waiting room fragment with URI [$uri]")
                    val action =
                        HistoryListFragmentDirections.actionHistoryListFragmentToMeetingWaitingRoomFragment(
                            uri
                        )
                    findNavController().navigate(action)
                }
            }
        }

        binding.setDeleteAllClickListener {
            showDeleteAllConfirmationDialog()
        }

        binding.setStartCallClickListener {
            if (findNavController().currentDestination?.id == R.id.historyListFragment) {
                Log.i("$TAG Navigating to start call fragment")
                val action =
                    HistoryListFragmentDirections.actionHistoryListFragmentToStartCallFragment()
                findNavController().navigate(action)
            }
        }

        // AbstractMainFragment related

        listViewModel.title.value = getString(R.string.bottom_navigation_calls_label)
        setViewModel(listViewModel)
        initViews(
            binding.slidingPaneLayout,
            binding.topBar,
            binding.bottomNavBar,
            R.id.historyListFragment
        )
    }

    override fun onPause() {
        super.onPause()

        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }

    override fun onResume() {
        super.onResume()

        Log.i("$TAG Fragment is resumed, resetting missed calls count")
        sharedViewModel.resetMissedCallsCountEvent.value = Event(true)
        sharedViewModel.refreshDrawerMenuAccountsListEvent.value = Event(false)

        if (shouldRefreshDataInOnResume()) {
            Log.i("$TAG Keep app alive setting is enabled, refreshing view just in case")
            listViewModel.filter()
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

    private fun showDeleteAllConfirmationDialog() {
        val model = ConfirmationDialogModel()
        val dialog = DialogUtils.getRemoveAllCallLogsConfirmationDialog(
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
                Log.w("$TAG Removing all call entries from database")
                listViewModel.removeAllCallLogs()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmationDialog(callLogModel: CallLogModel) {
        val dialogModel = ConfirmationDialogModel()
        val dialog = DialogUtils.getRemoveCallLogConfirmationDialog(
            requireActivity(),
            dialogModel
        )

        dialogModel.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialog.dismiss()
            }
        }

        dialogModel.confirmEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Deleting call log with ref key or call ID [${callLogModel.id}]")
                callLogModel.delete()
                listViewModel.filter()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

}
