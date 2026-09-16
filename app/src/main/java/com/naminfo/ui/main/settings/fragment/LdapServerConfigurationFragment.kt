package com.naminfo.ui.main.settings.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import org.linphone.core.tools.Log
import com.naminfo.databinding.SettingsContactsLdapBinding
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.ui.main.settings.viewmodel.LdapViewModel

@UiThread
class LdapServerConfigurationFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[LDAP Server Configuration Fragment]"
    }

    private lateinit var binding: SettingsContactsLdapBinding

    private lateinit var viewModel: LdapViewModel

    private val args: LdapServerConfigurationFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsContactsLdapBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[LdapViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        val ldapServerUrl = args.serverUrl
        if (ldapServerUrl != null) {
            Log.i("$TAG Found server URL in arguments, loading values")
            viewModel.loadLdap(ldapServerUrl)
        } else {
            Log.i("$TAG No server URL found in arguments, starting from scratch")
        }

        binding.setBackClickListener {
            goBack()
        }

        viewModel.ldapServerOperationSuccessfulEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG LDAP server operation was successful, going back")
                goBack()
            }
        }
    }
}
