package com.naminfo.ui.main.help.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.fragment.findNavController
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.HelpFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.ui.main.help.viewmodel.HelpViewModel
import com.naminfo.utils.ConfirmationDialogModel
import com.naminfo.utils.DialogUtils
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider

@UiThread
class HelpFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[Help Fragment]"
    }

    private lateinit var binding: HelpFragmentBinding

    private lateinit var viewModel: HelpViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = HelpFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this)[HelpViewModel::class.java]
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            goBack()
        }

        binding.setDebugClickListener {
            if (findNavController().currentDestination?.id == R.id.helpFragment) {
                val action = HelpFragmentDirections.actionHelpFragmentToDebugFragment()
                findNavController().navigate(action)
            }
        }

        binding.setUserGuideClickListener {
            val url = getString(R.string.website_user_guide_url)
            openUrlInBrowser(url)
        }

        binding.setPrivacyPolicyClickListener {
            val url = getString(R.string.website_privacy_policy_url)
            openUrlInBrowser(url)
        }

        binding.setLicensesClickListener {
            val url = getString(R.string.website_open_source_licences_usage_url)
            openUrlInBrowser(url)
        }

        binding.setTranslateClickListener {
            val url = getString(R.string.website_translate_weblate_url)
            openUrlInBrowser(url)
        }

        viewModel.newVersionAvailableEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val version = pair.first
                val url = pair.second
                showUpdateAvailableDialog(version, url)
            }
        }

        viewModel.versionUpToDateEvent.observe(viewLifecycleOwner) {
            it.consume {
                (requireActivity() as GenericActivity).showGreenToast(
                    getString(R.string.help_version_up_to_date_toast_message),
                    R.drawable.info
                )
            }
        }

        viewModel.errorEvent.observe(viewLifecycleOwner) {
            it.consume {
                (requireActivity() as GenericActivity).showRedToast(
                    getString(R.string.help_error_checking_version_toast_message),
                    R.drawable.warning_circle
                )
            }
        }
    }

    private fun showUpdateAvailableDialog(version: String, url: String) {
        val message = getString(R.string.help_dialog_update_available_message, version)

        val model = ConfirmationDialogModel()
        val dialog = DialogUtils.getUpdateAvailableDialog(
            requireActivity(),
            model,
            message
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialog.dismiss()
            }
        }

        model.confirmEvent.observe(viewLifecycleOwner) {
            it.consume {
                openUrlInBrowser(url)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun openUrlInBrowser(url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(browserIntent)
        } catch (ise: IllegalStateException) {
            Log.e(
                "$TAG Can't start ACTION_VIEW intent for URL [$url], IllegalStateException: $ise"
            )
        } catch (anfe: ActivityNotFoundException) {
            Log.e(
                "$TAG Can't start ACTION_VIEW intent for URL [$url], ActivityNotFoundException: $anfe"
            )
        } catch (e: Exception) {
            Log.e(
                "$TAG Can't start ACTION_VIEW intent for URL [$url]: $e"
            )
        }
    }
}
