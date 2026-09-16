package com.naminfo.ui.assistant.fragment

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.AssistantRegisterConfirmSmsCodeFragmentBinding
import com.naminfo.ui.GenericFragment
import com.naminfo.ui.assistant.viewmodel.AccountCreationViewModel

@UiThread
class RegisterCodeConfirmationFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Register Code Confirmation Fragment]"
    }

    private lateinit var binding: AssistantRegisterConfirmSmsCodeFragmentBinding

    private val viewModel: AccountCreationViewModel by navGraphViewModels(
        R.id.assistant_nav_graph
    )

    private var accountCreated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AssistantRegisterConfirmSmsCodeFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            goBack()
        }

        viewModel.accountCreatedEvent.observe(viewLifecycleOwner) {
            it.consume {
                val identity = viewModel.username.value.orEmpty()
                Log.i("$TAG Account [$identity] has been created, leaving assistant")
                accountCreated = true
                requireActivity().finish()
            }
        }

        // This won't work starting Android 10 as clipboard access is denied unless app has focus,
        // which won't be the case when the SMS arrives unless it is added into clipboard from a notification
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener {
            val data = clipboard.primaryClip
            if (data != null && data.itemCount > 0) {
                val clip = data.getItemAt(0).text?.toString() ?: ""
                if (clip.length == 4) {
                    Log.i(
                        "$TAG Found 4 digits [$clip] as primary clip in clipboard, using it and clear it"
                    )
                    viewModel.smsCodeFirstDigit.value = clip[0].toString()
                    viewModel.smsCodeSecondDigit.value = clip[1].toString()
                    viewModel.smsCodeThirdDigit.value = clip[2].toString()
                    viewModel.smsCodeLastDigit.value = clip[3].toString()
                    clipboard.clearPrimaryClip()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!accountCreated) {
            Log.w("$TAG Account wasn't completely created, remove Account & Auth Info from Core")
            viewModel.abortAccountCreation()
        }
    }

    private fun goBack() {
        findNavController().popBackStack()
    }
}
