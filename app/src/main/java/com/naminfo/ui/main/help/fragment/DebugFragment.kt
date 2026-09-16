package com.naminfo.ui.main.help.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.HelpDebugFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.assistant.AssistantActivity
import com.naminfo.ui.fileviewer.FileViewerActivity
import com.naminfo.ui.main.MainActivity
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.ui.main.help.viewmodel.HelpViewModel
import com.naminfo.utils.AppUtils

class DebugFragment : GenericMainFragment() {
    private lateinit var binding: HelpDebugFragmentBinding

    private lateinit var viewModel: HelpViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HelpDebugFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this)[HelpViewModel::class.java]
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        viewModel.canConfigFileBeViewed.postValue(requireActivity() is MainActivity)

        binding.setBackClickListener {
            goBack()
        }

        binding.setAppVersionClickListener {
            val label = getString(R.string.help_troubleshooting_app_version_title)
            val value = viewModel.appVersion.value.orEmpty()
            AppUtils.copyToClipboard(requireContext(), label, value)
        }

        binding.setSdkVersionClickListener {
            val label = getString(R.string.help_troubleshooting_sdk_version_title)
            val value = viewModel.sdkVersion.value.orEmpty()
            AppUtils.copyToClipboard(requireContext(), label, value)
        }

        viewModel.debugLogsCleanedEvent.observe(viewLifecycleOwner) {
            it.consume {
                (requireActivity() as GenericActivity).showGreenToast(
                    getString(R.string.help_troubleshooting_debug_logs_cleaned_toast_message),
                    R.drawable.info
                )
            }
        }

        viewModel.uploadDebugLogsFinishedEvent.observe(viewLifecycleOwner) {
            it.consume { url ->
                if (requireActivity() is AssistantActivity) {
                    AppUtils.copyToClipboard(requireContext(), "Logs upload URL", url)
                    return@consume
                }

                val appName = requireContext().getString(R.string.app_name)
                val intent = Intent(Intent.ACTION_SEND)
                intent.putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf(
                        requireContext().getString(
                            R.string.help_advanced_send_debug_logs_email_address
                        )
                    )
                )
                intent.putExtra(Intent.EXTRA_SUBJECT, "$appName Logs")
                intent.putExtra(Intent.EXTRA_TEXT, url)
                intent.type = "text/plain"

                try {
                    requireContext().startActivity(
                        Intent.createChooser(
                            intent,
                            requireContext().getString(
                                R.string.help_troubleshooting_share_logs_dialog_title
                            )
                        )
                    )
                } catch (ex: ActivityNotFoundException) {
                    Log.e(ex)
                }
            }
        }

        viewModel.uploadDebugLogsErrorEvent.observe(viewLifecycleOwner) {
            it.consume {
                (requireActivity() as GenericActivity).showRedToast(
                    getString(R.string.help_troubleshooting_debug_logs_upload_error_toast_message),
                    R.drawable.warning_circle
                )
            }
        }

        viewModel.showConfigFileEvent.observe(viewLifecycleOwner) {
            it.consume { path ->
                if (findNavController().currentDestination?.id == R.id.debugFragment) {
                    val intent = Intent(requireActivity(), FileViewerActivity::class.java)
                    val bundle = Bundle()
                    bundle.putString("path", path)
                    val nowInSeconds = System.currentTimeMillis() / 1000
                    bundle.putLong("timestamp", nowInSeconds)
                    intent.putExtras(bundle)
                    startActivity(intent)
                }
            }
        }
    }
}
