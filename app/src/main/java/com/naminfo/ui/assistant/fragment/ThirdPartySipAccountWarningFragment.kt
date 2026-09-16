package com.naminfo.ui.assistant.fragment

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
import com.naminfo.databinding.AssistantThirdPartySipAccountWarningFragmentBinding
import com.naminfo.ui.GenericFragment
import androidx.core.net.toUri

@UiThread
class ThirdPartySipAccountWarningFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Third Party SIP Account Warning Fragment]"
    }

    private lateinit var binding: AssistantThirdPartySipAccountWarningFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AssistantThirdPartySipAccountWarningFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.setBackClickListener {
            goBack()
        }

        binding.setContactClickListener {
            val url = getString(R.string.website_contact_url)
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

        binding.setCreateAccountClickListener {
            if (findNavController().currentDestination?.id == R.id.thirdPartySipAccountWarningFragment) {
                val action =
                    ThirdPartySipAccountWarningFragmentDirections.actionThirdPartySipAccountWarningFragmentToRegisterFragment()
                findNavController().navigate(action)
            }
        }

        binding.setLoginClickListener {
            if (findNavController().currentDestination?.id == R.id.thirdPartySipAccountWarningFragment) {
                val action =
                    ThirdPartySipAccountWarningFragmentDirections.actionThirdPartySipAccountWarningFragmentToThirdPartySipAccountLoginFragment()
                findNavController().navigate(action)
            }
        }
    }

    private fun goBack() {
        findNavController().popBackStack()
    }
}
