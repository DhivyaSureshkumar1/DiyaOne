package com.naminfo.ui.main.contacts.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.naminfo.BR
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.ContactNewOrEditFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.main.MainActivity
import com.naminfo.ui.main.contacts.model.NewOrEditNumberOrAddressModel
import com.naminfo.ui.main.contacts.viewmodel.ContactNewOrEditViewModel
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.utils.ConfirmationDialogModel
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.Event
import com.naminfo.utils.FileUtils

@UiThread
class NewContactFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[New Contact Fragment]"
    }

    private lateinit var binding: ContactNewOrEditFragmentBinding

    private lateinit var viewModel: ContactNewOrEditViewModel

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showAbortConfirmationDialogIfPendingChanges()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.i("$TAG Picture picked [$uri]")
            val localFileName = FileUtils.getFileStorageCacheDir(
                ContactNewOrEditViewModel.TEMP_PICTURE_NAME
            )
            lifecycleScope.launch {
                if (FileUtils.copyFile(uri, localFileName)) {
                    val newPath = FileUtils.getProperFilePath(
                        localFileName.absolutePath
                    )
                    Log.i("$TAG Copied file [$uri] to [$newPath]")
                    viewModel.picturePath.value = newPath
                } else {
                    Log.e(
                        "$TAG Failed to copy file from [$uri] to [${localFileName.absolutePath}]"
                    )
                }
            }
        } else {
            Log.w("$TAG No picture picked")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ContactNewOrEditFragmentBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )

        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this)[ContactNewOrEditViewModel::class.java]
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        viewModel.findFriendByRefKey("")

        val addressToAdd = sharedViewModel.sipAddressToAddToNewContact
        if (addressToAdd.isNotEmpty()) {
            Log.i("$TAG Pre-filling new contact form with SIP address [$addressToAdd]")
            sharedViewModel.sipAddressToAddToNewContact = ""

            coreContext.postOnCoreThread {
                viewModel.addSipAddress(addressToAdd)
            }
        }

        val displayNameToSetToNewContact = sharedViewModel.displayNameToSetToNewContact
        if (displayNameToSetToNewContact.isNotEmpty()) {
            if (displayNameToSetToNewContact.contains(" ")) {
                val split = displayNameToSetToNewContact.split(" ")
                val firstName = split[0]
                val lastName = split[1]
                Log.i("$TAG Pre-filling new contact form with first name [$firstName] and last name [$lastName]")
                viewModel.firstName.value = firstName
                viewModel.lastName.value = lastName
            } else {
                Log.i("$TAG Pre-filling new contact form with last name [$displayNameToSetToNewContact]")
                viewModel.lastName.value = displayNameToSetToNewContact
            }
            sharedViewModel.displayNameToSetToNewContact = ""
        }

        binding.setBackClickListener {
            showAbortConfirmationDialogIfPendingChanges()
        }

        binding.setPickImageClickListener {
            pickImage()
        }

        binding.setDeleteImageClickListener {
            viewModel.picturePath.value = ""
        }

        viewModel.saveChangesEvent.observe(viewLifecycleOwner) {
            it.consume { refKey ->
                if (refKey.isNotEmpty()) {
                    backPressedCallback.isEnabled = false
                    findNavController().popBackStack()

                    sharedViewModel.showContactEvent.value = Event(refKey)
                    (requireActivity() as GenericActivity).showGreenToast(
                        getString(R.string.contact_editor_saved_contact_toast),
                        R.drawable.info
                    )
                } else {
                    (requireActivity() as GenericActivity).showRedToast(
                        getString(R.string.contact_editor_error_saving_contact_toast),
                        R.drawable.warning_circle
                    )
                }
            }
        }

        viewModel.friendFoundEvent.observe(viewLifecycleOwner) {
            it.consume {
                binding.sipAddresses.removeAllViews()
                for (items in viewModel.sipAddresses) {
                    addCell(items)
                }
                binding.phoneNumbers.removeAllViews()
                for (items in viewModel.phoneNumbers) {
                    addCell(items)
                }
            }
        }

        viewModel.addNewNumberOrAddressFieldEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                addCell(model)
            }
        }

        viewModel.removeNewNumberOrAddressFieldEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                removeCell(model)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        backPressedCallback.isEnabled = true
    }

    private fun addCell(model: NewOrEditNumberOrAddressModel) {
        val inflater = requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val parent = if (model.isSip) binding.sipAddresses else binding.phoneNumbers

        val cellBinding: ViewDataBinding = if (model.isSip) {
            DataBindingUtil.inflate(
                inflater,
                R.layout.contact_new_or_edit_sip_address_cell,
                parent,
                false
            )
        } else {
            DataBindingUtil.inflate(
                inflater,
                R.layout.contact_new_or_edit_phone_number_cell,
                parent,
                false
            )
        }
        cellBinding.setVariable(BR.model, model)
        cellBinding.lifecycleOwner = (requireActivity() as MainActivity)

        parent.addView(cellBinding.root)
    }

    private fun removeCell(model: NewOrEditNumberOrAddressModel) {
        val parent = if (model.isSip) binding.sipAddresses else binding.phoneNumbers
        parent.removeAllViews()
        val source = if (model.isSip) viewModel.sipAddresses else viewModel.phoneNumbers
        for (items in source) {
            addCell(items)
        }
    }

    private fun pickImage() {
        try {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (anfe: ActivityNotFoundException) {
            Log.e("$TAG Failed to start media picker: $anfe")
        }
    }

    private fun showAbortConfirmationDialogIfPendingChanges() {
        if (!viewModel.isPendingChanges()) {
            Log.i("$TAG No changes detected, do not show confirmation dialog")
            backPressedCallback.isEnabled = false
            findNavController().popBackStack()
            return
        }

        val model = ConfirmationDialogModel()
        val dialog = DialogUtils.getCancelContactChangesConfirmationDialog(
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
                backPressedCallback.isEnabled = false
                findNavController().popBackStack()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
