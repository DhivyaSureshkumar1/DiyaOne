package com.naminfo.ui.main.chat.fragment

import android.Manifest
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.naminfo.contacts.PhoneContactNumbers
import com.naminfo.ui.main.contacts.viewmodel.MatchedContactsViewModel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.StartChatFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.main.chat.viewmodel.StartConversationViewModel
import com.naminfo.ui.main.fragment.GenericAddressPickerFragment
import com.naminfo.ui.main.model.GroupSetOrEditSubjectDialogModel
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.Event
import com.naminfo.utils.hideKeyboard

@UiThread
class StartConversationFragment : GenericAddressPickerFragment() {
    companion object {
        private const val TAG = "[Start Conversation Fragment]"
        private const val PERMISSION_REQUESTED = "new_chat_contacts_permission_requested"
    }

    private lateinit var binding: StartChatFragmentBinding

    override lateinit var viewModel: StartConversationViewModel

    private lateinit var matchedViewModel: MatchedContactsViewModel
    private var contactsPermissionRequested = false
    private var permissionRequestInProgress = false

    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionRequestInProgress = false
        if (::matchedViewModel.isInitialized) matchedViewModel.reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsPermissionRequested = savedInstanceState?.getBoolean(PERMISSION_REQUESTED) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = StartChatFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[StartConversationViewModel::class.java]
        matchedViewModel = ViewModelProvider(requireActivity())[MatchedContactsViewModel::class.java]

        postponeEnterTransition()
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            goBack()
        }

        binding.setAskForGroupConversationSubjectClickListener {
            showGroupConversationSubjectDialog()
        }

        setupRecyclerView(binding.contactsList)

        matchedViewModel.contacts.observe(viewLifecycleOwner) {
            viewModel.setMatchedContacts(it)
        }
        matchedViewModel.loading.observe(viewLifecycleOwner) {
            viewModel.contactsLoading.value = it
        }
        matchedViewModel.status.observe(viewLifecycleOwner) {
            viewModel.contactsStatus.value = it
        }

        viewModel.modelsList.observe(
            viewLifecycleOwner
        ) {
            Log.i("$TAG Contacts & suggestions list is ready with [${it.size}] items")
            adapter.submitList(it)

            attachAdapter()

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }
        }

        viewModel.chatRoomCreatedEvent.observe(viewLifecycleOwner) {
            it.consume { conversationId ->
                Log.i(
                    "$TAG Conversation [$conversationId] has been created, navigating to it"
                )
                sharedViewModel.showConversationEvent.value = Event(conversationId)
                goBack()
            }
        }

        viewModel.defaultAccountChangedEvent.observe(viewLifecycleOwner) {
            it.consume {
                viewModel.updateGroupChatButtonVisibility()
                matchedViewModel.reload(accountChanged = true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::matchedViewModel.isInitialized || permissionRequestInProgress) return

        if (PhoneContactNumbers.hasPermission(requireContext())) {
            matchedViewModel.reload()
        } else if (!contactsPermissionRequested) {
            contactsPermissionRequested = true
            permissionRequestInProgress = true
            matchedViewModel.reload()
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            matchedViewModel.reload()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(PERMISSION_REQUESTED, contactsPermissionRequested)
        super.onSaveInstanceState(outState)
    }

    private fun showGroupConversationSubjectDialog() {
        val model = GroupSetOrEditSubjectDialogModel("", isGroupConversation = true)

        val dialog = DialogUtils.getSetOrEditGroupSubjectDialog(
            requireContext(),
            viewLifecycleOwner,
            model
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Set conversation subject cancelled")
                dialog.dismiss()
            }
        }

        model.confirmEvent.observe(viewLifecycleOwner) {
            it.consume { newSubject ->
                if (newSubject.isNotEmpty()) {
                    Log.i(
                        "$TAG Conversation subject has been set to [$newSubject]"
                    )
                    viewModel.subject.value = newSubject
                    viewModel.createGroupChatRoom()

                    dialog.currentFocus?.hideKeyboard()
                    dialog.dismiss()
                } else {
                    val message = getString(R.string.conversation_invalid_empty_subject_toast)
                    val icon = R.drawable.warning_circle
                    (requireActivity() as GenericActivity).showRedToast(message, icon)
                }
            }
        }

        Log.i("$TAG Showing dialog to set conversation subject")
        dialog.show()
    }
}
