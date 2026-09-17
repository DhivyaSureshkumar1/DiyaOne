package com.naminfo.ui.main.settings.fragment

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import kotlinx.coroutines.launch
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.AccountProfileFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.utils.ConfirmationDialogModel
import com.naminfo.ui.main.settings.viewmodel.AccountProfileViewModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.Event
import com.naminfo.utils.FileUtils

@UiThread
class AccountProfileFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[Account Profile Fragment]"
    }

    private lateinit var binding: AccountProfileFragmentBinding

    private val viewModel: AccountProfileViewModel by navGraphViewModels(
        R.id.main_nav_graph
    )

    private val args: AccountProfileFragmentArgs by navArgs()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            Log.i("$TAG Picture picked [$uri]")
            lifecycleScope.launch {
                val localFileName = FileUtils.getFilePath(requireContext(), uri, true)
                if (localFileName != null) {
                    Log.i("$TAG Picture will be locally stored as [$localFileName]")
                    val path = FileUtils.getProperFilePath(localFileName)
                    viewModel.setNewPicturePath(path)
                } else {
                    Log.e("$TAG Failed to copy [$uri] to local storage")
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
        binding = AccountProfileFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        val identity = args.accountIdentity
        Log.i("$TAG Looking up for account with identity address [$identity]")
        viewModel.findAccountMatchingIdentity(identity)

        binding.setBackClickListener {
            goBack()
        }

        binding.setPickImageClickListener {
            pickImage()
        }

        binding.setDeleteImageClickListener {
            viewModel.setNewPicturePath("")
        }

        binding.setCopySipUriClickListener {
            copyAddressToClipboard(viewModel.sipAddress.value.orEmpty())
        }

        binding.setCopyDeviceIdClickListener {
            copyAddressToClipboard(viewModel.deviceId.value.orEmpty())
        }

        binding.setChangeModeClickListener {
            goToAccountProfileModeFragment()
        }

        binding.setSettingsClickListener {
            if (findNavController().currentDestination?.id == R.id.accountProfileFragment) {
                val action =
                    AccountProfileFragmentDirections.actionAccountProfileFragmentToAccountSettingsFragment(
                        identity
                    )
                findNavController().navigate(action)
            }
        }

        binding.setDeleteAccountClickListener {
            val model = ConfirmationDialogModel()
            val dialog = DialogUtils.getConfirmAccountRemovalDialog(
                requireActivity(),
                model,
                viewModel.isOnDefaultDomain.value == true
            )

            model.dismissEvent.observe(viewLifecycleOwner) {
                it.consume {
                    dialog.dismiss()
                }
            }

            model.confirmEvent.observe(viewLifecycleOwner) { event ->
                event.consume {
                    if (viewModel.signOutInProgress.value != true) {
                        viewModel.deleteAccount()
                        dialog.dismiss()
                    }
                }
            }

            dialog.show()
        }

        viewModel.accountRemovedEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Account has been removed, leaving profile")
                goBack()
            }
        }

        viewModel.accountFoundEvent.observe(viewLifecycleOwner) {
            it.consume { found ->
                if (found) {
                    startPostponedEnterTransition()
                } else {
                    Log.e(
                        "$TAG Failed to find an account matching this identity address [$identity]"
                    )
                    val message = getString(R.string.account_failed_to_find_identity_toast)
                    val icon = R.drawable.warning_circle
                    (requireActivity() as GenericActivity).showRedToast(message, icon)
                    goBack()
                }
            }
        }

        viewModel.signOutError.observe(viewLifecycleOwner) { message ->
            if (message.isNotBlank()) {
                (requireActivity() as GenericActivity).showRedToast(
                    message,
                    R.drawable.warning_circle
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()

        Log.i("$TAG Leaving account profile, saving changes")
        viewModel.saveChangesWhenLeaving()
        sharedViewModel.refreshDrawerMenuAccountsListEvent.value = Event(true)
    }

    private fun goToAccountProfileModeFragment() {
        if (findNavController().currentDestination?.id == R.id.accountProfileFragment) {
            val action =
                AccountProfileFragmentDirections.actionAccountProfileFragmentToAccountProfileModeFragment()
            findNavController().navigate(action)
        }
    }

    private fun pickImage() {
        try {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (anfe: ActivityNotFoundException) {
            Log.e("$TAG Failed to start media picker: $anfe")
        }
    }

    private fun copyAddressToClipboard(value: String) {
        if (AppUtils.copyToClipboard(requireContext(), AppUtils.getString(R.string.sip_address), value)) {
            val message = getString(R.string.sip_address_copied_to_clipboard_toast)
            (requireActivity() as GenericActivity).showGreenToast(message, R.drawable.check)
        }
    }

}
