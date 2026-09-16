package com.naminfo.ui.assistant.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.AssistantLandingFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.GenericFragment
import com.naminfo.ui.assistant.model.AcceptConditionsAndPolicyDialogModel
import com.naminfo.ui.assistant.viewmodel.AccountLoginViewModel
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.PhoneNumberUtils
import androidx.core.net.toUri

@UiThread
class LandingFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Landing Fragment]"
    }

    private lateinit var binding: AssistantLandingFragmentBinding

    private val viewModel: AccountLoginViewModel by navGraphViewModels(
        R.id.assistant_nav_graph
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AssistantLandingFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            requireActivity().finish()
        }

        binding.setHelpClickListener {
            if (findNavController().currentDestination?.id == R.id.landingFragment) {
                val action =
                    LandingFragmentDirections.actionLandingFragmentToHelpFragment()
                findNavController().navigate(action)
            }
        }

        binding.setRegisterClickListener {
            if (viewModel.conditionsAndPrivacyPolicyAccepted) {
                goToRegisterFragment()
            } else {
                showAcceptConditionsAndPrivacyDialog(goToAccountCreate = true)
            }
        }

        binding.setQrCodeClickListener {
            if (findNavController().currentDestination?.id == R.id.landingFragment) {
                val action =
                    LandingFragmentDirections.actionLandingFragmentToQrCodeScannerFragment()
                findNavController().navigate(action)
            }
        }

        binding.setThirdPartySipAccountLoginClickListener {
            if (viewModel.conditionsAndPrivacyPolicyAccepted) {
                goToLoginThirdPartySipAccountFragment(false)
            } else {
                showAcceptConditionsAndPrivacyDialog(goToThirdPartySipAccountLogin = true)
            }
        }

        binding.setForgottenPasswordClickListener {
            if (findNavController().currentDestination?.id == R.id.landingFragment) {
                val action =
                    LandingFragmentDirections.actionLandingFragmentToRecoverAccountFragment()
                findNavController().navigate(action)
            }
        }

        viewModel.showPassword.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                delay(50)
                binding.password.setSelection(binding.password.text?.length ?: 0)
            }
        }

        viewModel.accountLoggedInEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Account successfully logged-in, leaving assistant")
                requireActivity().finish()
            }
        }

        viewModel.accountLoginErrorEvent.observe(viewLifecycleOwner) {
            it.consume { message ->
                (requireActivity() as GenericActivity).showRedToast(
                    message,
                    R.drawable.warning_circle
                )
            }
        }

        viewModel.skipLandingToThirdPartySipAccountEvent.observe(viewLifecycleOwner) {
            it.consume {
                goToLoginThirdPartySipAccountFragment(true)
            }
        }

        val telephonyManager = requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val countryIso = telephonyManager.networkCountryIso
        coreContext.postOnCoreThread {
            val dialPlan = PhoneNumberUtils.getDeviceDialPlan(countryIso)
            if (dialPlan != null) {
                viewModel.internationalPrefix.postValue(dialPlan.countryCallingCode)
                viewModel.internationalPrefixIsoCountryCode.postValue(dialPlan.isoCountryCode)
            }
        }
    }

    private fun goToRegisterFragment() {
        if (findNavController().currentDestination?.id == R.id.landingFragment) {
            val action = LandingFragmentDirections.actionLandingFragmentToRegisterFragment()
            findNavController().navigate(action)
        }
    }

    private fun goToLoginThirdPartySipAccountFragment(skipWarning: Boolean) {
        if (findNavController().currentDestination?.id == R.id.landingFragment) {
            val action = if (skipWarning) {
                LandingFragmentDirections.actionLandingFragmentToThirdPartySipAccountLoginFragment()
            } else {
                LandingFragmentDirections.actionLandingFragmentToThirdPartySipAccountWarningFragment()
            }
            findNavController().navigate(action)
        }
    }

    private fun showAcceptConditionsAndPrivacyDialog(
        goToAccountCreate: Boolean = false,
        goToThirdPartySipAccountLogin: Boolean = false
    ) {
        val model = AcceptConditionsAndPolicyDialogModel()
        val dialog = DialogUtils.getAcceptConditionsAndPrivacyDialog(
            requireActivity(),
            model
        )

        model.dismissEvent.observe(viewLifecycleOwner) {
            it.consume {
                dialog.dismiss()
            }
        }

        model.conditionsAcceptedEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Conditions & Privacy policy have been accepted")
                coreContext.postOnCoreThread {
                    corePreferences.conditionsAndPrivacyPolicyAccepted = true
                }
                dialog.dismiss()

                if (goToAccountCreate) {
                    goToRegisterFragment()
                } else if (goToThirdPartySipAccountLogin) {
                    goToLoginThirdPartySipAccountFragment(false)
                }
            }
        }

        model.privacyPolicyClickedEvent.observe(viewLifecycleOwner) {
            it.consume {
                val url = getString(R.string.website_privacy_policy_url)
                openUrlInBrowser(url)
            }
        }

        model.generalTermsClickedEvent.observe(viewLifecycleOwner) {
            it.consume {
                val url = getString(R.string.website_terms_and_conditions_url)
                openUrlInBrowser(url)
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
