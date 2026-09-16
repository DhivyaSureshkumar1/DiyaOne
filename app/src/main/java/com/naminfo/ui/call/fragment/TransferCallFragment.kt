package com.naminfo.ui.call.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.getValue
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.R
import com.naminfo.contacts.getListOfSipAddressesAndPhoneNumbers
import org.linphone.core.Address
import org.linphone.core.tools.Log
import com.naminfo.databinding.CallTransferFragmentBinding
import com.naminfo.ui.call.adapter.CallsListAdapter
import com.naminfo.ui.call.model.CallModel
import com.naminfo.ui.call.viewmodel.CallsViewModel
import com.naminfo.ui.call.viewmodel.CurrentCallViewModel
import com.naminfo.ui.main.adapter.ConversationsContactsAndSuggestionsListAdapter
import com.naminfo.ui.main.contacts.model.ContactNumberOrAddressClickListener
import com.naminfo.ui.main.contacts.model.ContactNumberOrAddressModel
import com.naminfo.ui.main.contacts.model.NumberOrAddressPickerDialogModel
import com.naminfo.ui.main.history.viewmodel.StartCallViewModel
import com.naminfo.ui.main.model.ConversationContactOrSuggestionModel
import com.naminfo.utils.ConfirmationDialogModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.LinphoneUtils
import com.naminfo.utils.RecyclerViewHeaderDecoration
import com.naminfo.utils.hideKeyboard
import com.naminfo.utils.setKeyboardInsetListener
import com.naminfo.utils.showKeyboard

@UiThread
class TransferCallFragment : GenericCallFragment() {
    companion object {
        private const val TAG = "[Transfer Call Fragment]"
    }

    private lateinit var binding: CallTransferFragmentBinding

    private val viewModel: StartCallViewModel by navGraphViewModels(
        R.id.call_nav_graph
    )

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                viewModel.isNumpadVisible.value = false
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) { }
    }

    private lateinit var callViewModel: CurrentCallViewModel

    private lateinit var callsViewModel: CallsViewModel

    private lateinit var callsAdapter: CallsListAdapter

    private lateinit var contactsAdapter: ConversationsContactsAndSuggestionsListAdapter

    private var numberOrAddressPickerDialog: Dialog? = null

    private val listener = object : ContactNumberOrAddressClickListener {
        @UiThread
        override fun onClicked(model: ContactNumberOrAddressModel) {
            val address = model.address
            if (address != null) {
                coreContext.postOnCoreThread {
                    doCallTransfer(address, model.name)
                }
            }
        }

        @UiThread
        override fun onLongPress(model: ContactNumberOrAddressModel) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        callsAdapter = CallsListAdapter(showTransferIconInsteadOfCallState = true)
        contactsAdapter = ConversationsContactsAndSuggestionsListAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CallTransferFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        callViewModel = requireActivity().run {
            ViewModelProvider(this)[CurrentCallViewModel::class.java]
        }
        observeToastEvents(callViewModel)

        callsViewModel = requireActivity().run {
            ViewModelProvider(this)[CallsViewModel::class.java]
        }
        observeToastEvents(callsViewModel)

        binding.viewModel = viewModel
        binding.callsViewModel = callsViewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            findNavController().popBackStack()
        }

        binding.setHideNumpadClickListener {
            viewModel.hideNumpad()
        }

        binding.callsList.setHasFixedSize(true)
        binding.contactsAndSuggestionsList.setHasFixedSize(true)

        binding.contactsAndSuggestionsList.layoutManager = LinearLayoutManager(requireContext())
        binding.callsList.layoutManager = LinearLayoutManager(requireContext())

        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), contactsAdapter)
        binding.contactsAndSuggestionsList.addItemDecoration(headerItemDecoration)

        callsAdapter.callClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                showConfirmAttendedTransferDialog(model)
            }
        }

        contactsAdapter.onClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                startCallTransfer(model)
            }
        }

        callsViewModel.callsExceptCurrentOne.observe(viewLifecycleOwner) {
            Log.i("$TAG Calls list updated with [${it.size}] items")
            callsAdapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.callsList.adapter != callsAdapter) {
                binding.callsList.adapter = callsAdapter
            }
        }

        viewModel.modelsList.observe(
            viewLifecycleOwner
        ) {
            Log.i("$TAG Contacts & suggestions list is ready with [${it.size}] items")
            val count = contactsAdapter.itemCount
            contactsAdapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.contactsAndSuggestionsList.adapter != contactsAdapter) {
                binding.contactsAndSuggestionsList.adapter = contactsAdapter
            }

            if (count == 0) {
                (view.parent as? ViewGroup)?.doOnPreDraw {
                    startPostponedEnterTransition()
                }
            }
        }

        viewModel.searchFilter.observe(viewLifecycleOwner) { filter ->
            val trimmed = filter.trim()
            viewModel.applyFilter(trimmed)
        }

        viewModel.removedCharacterAtCurrentPositionEvent.observe(viewLifecycleOwner) {
            it.consume {
                val selectionStart = binding.searchBar.selectionStart
                val selectionEnd = binding.searchBar.selectionEnd
                if (selectionStart > 0) {
                    binding.searchBar.text =
                        binding.searchBar.text?.delete(
                            selectionStart - 1,
                            selectionEnd
                        )
                    binding.searchBar.setSelection(selectionStart - 1)
                }
            }
        }

        viewModel.appendDigitToSearchBarEvent.observe(viewLifecycleOwner) {
            it.consume { digit ->
                val newValue = "${binding.searchBar.text}$digit"
                binding.searchBar.setText(newValue)
                binding.searchBar.setSelection(newValue.length)
            }
        }

        viewModel.requestKeyboardVisibilityChangedEvent.observe(viewLifecycleOwner) {
            it.consume { show ->
                if (show) {
                    // To automatically open keyboard
                    binding.searchBar.showKeyboard()
                } else {
                    binding.searchBar.requestFocus()
                    binding.searchBar.hideKeyboard()
                }
            }
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(binding.numpadLayout.root)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)

        viewModel.isNumpadVisible.observe(viewLifecycleOwner) { visible ->
            if (visible) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        viewModel.initiateBlindTransferEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val address = pair.first
                val displayName = pair.second
                showConfirmBlindTransferDialog(address, displayName)
            }
        }

        binding.root.setKeyboardInsetListener { keyboardVisible ->
            if (keyboardVisible) {
                viewModel.isNumpadVisible.value = false
            }
        }
    }

    override fun onPause() {
        super.onPause()

        viewModel.searchFilter.value = ""
        numberOrAddressPickerDialog?.dismiss()
        numberOrAddressPickerDialog = null
    }

    override fun onResume() {
        super.onResume()

        viewModel.title.value = getString(
            R.string.call_transfer_current_call_title,
            callViewModel.displayedName.value ?: callViewModel.displayedAddress.value
        )

        coreContext.postOnCoreThread {
            if (corePreferences.automaticallyShowDialpad) {
                viewModel.isNumpadVisible.postValue(true)
            }
        }
    }

    private fun startCallTransfer(model: ConversationContactOrSuggestionModel) {
        coreContext.postOnCoreThread {
            val friend = model.friend
            if (friend == null) {
                doCallTransfer(model.address, model.name)
                return@postOnCoreThread
            }

            val singleAvailableAddress = LinphoneUtils.getSingleAvailableAddressForFriend(friend)
            if (singleAvailableAddress != null) {
                Log.i(
                    "$TAG Only 1 SIP address or phone number found for contact [${friend.name}], starting call directly"
                )
                doCallTransfer(singleAvailableAddress, model.name)
            } else {
                val list = friend.getListOfSipAddressesAndPhoneNumbers(listener)
                Log.i(
                    "$TAG [${list.size}] numbers or addresses found for contact [${friend.name}], showing selection dialog"
                )

                coreContext.postOnMainThread {
                    val numberOrAddressModel = NumberOrAddressPickerDialogModel(list)
                    val dialog =
                        DialogUtils.getNumberOrAddressPickerDialog(
                            requireActivity(),
                            numberOrAddressModel
                        )
                    numberOrAddressPickerDialog = dialog

                    numberOrAddressModel.dismissEvent.observe(viewLifecycleOwner) { event ->
                        event.consume {
                            dialog.dismiss()
                        }
                    }

                    dialog.show()
                }
            }
        }
    }

    @WorkerThread
    private fun doCallTransfer(address: Address, name: String) {
        coreContext.postOnMainThread {
            showConfirmBlindTransferDialog(address, name)
        }
    }

    private fun showConfirmAttendedTransferDialog(callModel: CallModel) {
        val from = callViewModel.displayedName.value.orEmpty()
        val to = callModel.displayName.value.orEmpty()
        Log.i("$TAG Asking user confirmation before doing attended transfer of call with [$from] to [$to](${callModel.call.remoteAddress.asStringUriOnly()})")
        val label = AppUtils.getFormattedString(
            R.string.call_transfer_confirm_dialog_message,
            from,
            to
        )
        val model = ConfirmationDialogModel(label)
        val dialog = DialogUtils.getConfirmCallTransferCallDialog(
            requireActivity(),
            model
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Attended transfer was cancelled by user")
                dialog.dismiss()
            }
        }

        model.confirmEvent.observe(viewLifecycleOwner) {
            it.consume {
                coreContext.postOnCoreThread {
                    val call = callModel.call
                    Log.i(
                        "$TAG Transferring (attended) call to [${call.remoteAddress.asStringUriOnly()}]"
                    )
                    callViewModel.attendedTransferCallTo(call)
                }

                dialog.dismiss()
                findNavController().popBackStack()
            }
        }

        dialog.show()
    }

    private fun showConfirmBlindTransferDialog(toAddress: Address, toDisplayName: String) {
        val from = callViewModel.displayedName.value.orEmpty()
        Log.i("$TAG Asking user confirmation before doing blind transfer of call with [$from] to [$toDisplayName](${toAddress.asStringUriOnly()})")
        val label = AppUtils.getFormattedString(
            R.string.call_transfer_confirm_dialog_message,
            from,
            toDisplayName
        )
        val model = ConfirmationDialogModel(label)
        val dialog = DialogUtils.getConfirmCallTransferCallDialog(
            requireActivity(),
            model
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Blind transfer was cancelled by user")
                dialog.dismiss()
            }
        }

        model.confirmEvent.observe(viewLifecycleOwner) {
            it.consume {
                coreContext.postOnCoreThread {
                    Log.i("$TAG Transferring (blind) call to [${toAddress.asStringUriOnly()}]")
                    callViewModel.blindTransferCallTo(toAddress)
                }

                dialog.dismiss()
                findNavController().popBackStack()
            }
        }

        dialog.show()
    }
}
