package com.naminfo.ui.call.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.R
import com.naminfo.contacts.getListOfSipAddressesAndPhoneNumbers
import org.linphone.core.Address
import org.linphone.core.tools.Log
import com.naminfo.databinding.StartCallFragmentBinding
import com.naminfo.ui.main.adapter.ConversationsContactsAndSuggestionsListAdapter
import com.naminfo.ui.main.contacts.model.ContactNumberOrAddressClickListener
import com.naminfo.ui.main.contacts.model.ContactNumberOrAddressModel
import com.naminfo.ui.main.contacts.model.NumberOrAddressPickerDialogModel
import com.naminfo.ui.main.history.viewmodel.StartCallViewModel
import com.naminfo.ui.main.model.ConversationContactOrSuggestionModel
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.LinphoneUtils
import com.naminfo.utils.RecyclerViewHeaderDecoration
import com.naminfo.utils.hideKeyboard
import com.naminfo.utils.setKeyboardInsetListener
import com.naminfo.utils.showKeyboard

class NewCallFragment : GenericCallFragment() {
    companion object {
        private const val TAG = "[New Call Fragment]"
    }

    private lateinit var binding: StartCallFragmentBinding

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

    private lateinit var adapter: ConversationsContactsAndSuggestionsListAdapter

    private val listener = object : ContactNumberOrAddressClickListener {
        @UiThread
        override fun onClicked(model: ContactNumberOrAddressModel) {
            val address = model.address
            if (address != null) {
                coreContext.postOnCoreThread {
                    startCall(address)
                }
            }
        }

        @UiThread
        override fun onLongPress(model: ContactNumberOrAddressModel) {
        }
    }

    private var numberOrAddressPickerDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConversationsContactsAndSuggestionsListAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = StartCallFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            findNavController().popBackStack()
        }

        binding.setHideNumpadClickListener {
            viewModel.hideNumpad()
        }

        binding.contactsAndSuggestionsList.setHasFixedSize(true)

        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), adapter)
        binding.contactsAndSuggestionsList.addItemDecoration(headerItemDecoration)

        adapter.onClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                startCall(model)
            }
        }

        binding.contactsAndSuggestionsList.layoutManager = LinearLayoutManager(requireContext())

        viewModel.modelsList.observe(
            viewLifecycleOwner
        ) {
            Log.i("$TAG Contacts & suggestions list is ready with [${it.size}] items")
            val count = adapter.itemCount
            adapter.submitList(it)

            // Wait for adapter to have items before setting it in the RecyclerView,
            // otherwise scroll position isn't retained
            if (binding.contactsAndSuggestionsList.adapter != adapter) {
                binding.contactsAndSuggestionsList.adapter = adapter
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

        binding.root.setKeyboardInsetListener { keyboardVisible ->
            if (keyboardVisible) {
                viewModel.isNumpadVisible.value = false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        coreContext.postOnCoreThread {
            if (corePreferences.automaticallyShowDialpad) {
                viewModel.isNumpadVisible.postValue(true)
            }
        }
    }

    override fun onPause() {
        super.onPause()

        viewModel.searchFilter.value = ""
        numberOrAddressPickerDialog?.dismiss()
        numberOrAddressPickerDialog = null
    }

    private fun startCall(model: ConversationContactOrSuggestionModel) {
        coreContext.postOnCoreThread {
            val friend = model.friend
            if (friend == null) {
                startCall(model.address)
                return@postOnCoreThread
            }

            val singleAvailableAddress = LinphoneUtils.getSingleAvailableAddressForFriend(friend)
            if (singleAvailableAddress != null) {
                Log.i(
                    "$TAG Only 1 SIP address or phone number found for contact [${friend.name}], starting call directly"
                )
                startCall(singleAvailableAddress)
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
    private fun startCall(address: Address) {
        Log.i("$TAG Calling [${address.asStringUriOnly()}]")
        coreContext.startAudioCall(address)

        coreContext.postOnMainThread {
            try {
                findNavController().popBackStack()
            } catch (ise: IllegalStateException) {
                Log.e("$TAG Can't go back: $ise")
            }
        }
    }
}
