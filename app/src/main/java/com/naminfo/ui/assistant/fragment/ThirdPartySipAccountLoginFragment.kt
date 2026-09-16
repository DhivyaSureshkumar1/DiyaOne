package com.naminfo.ui.assistant.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import com.naminfo.compatibility.Compatibility
import org.linphone.core.tools.Log
import com.naminfo.databinding.AssistantThirdPartySipAccountLoginFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.GenericFragment
import com.naminfo.ui.assistant.viewmodel.ThirdPartySipAccountLoginViewModel
import com.naminfo.ui.sso.SingleSignOnActivity

@UiThread
class ThirdPartySipAccountLoginFragment : GenericFragment() {
    companion object {
        private const val TAG = "[Third Party SIP Account Login Fragment]"
    }

    private lateinit var binding: AssistantThirdPartySipAccountLoginFragmentBinding

    private val viewModel: ThirdPartySipAccountLoginViewModel by navGraphViewModels(
        R.id.assistant_nav_graph
    )

    private val dropdownListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val transport = viewModel.availableTransports[position]
            Log.i("$TAG Selected transport updated [$transport]")
            viewModel.transport.value = transport
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private lateinit var adapter: ArrayAdapter<String>

    private val accessLocalNetworkPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("$TAG ACCESS_LOCAL_NETWORK permission has been granted")
        } else {
            Log.w("$TAG ACCESS_LOCAL_NETWORK permission has been denied!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AssistantThirdPartySipAccountLoginFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        adapter = ArrayAdapter(
            requireContext(),
            R.layout.drop_down_item,
            viewModel.availableTransports
        )
        adapter.setDropDownViewResource(R.layout.generic_dropdown_cell)
        binding.transport.adapter = adapter
        binding.transport.onItemSelectedListener = dropdownListener

        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        viewModel.accountLoggedInEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Account successfully logged-in, requesting permissions")
                if (findNavController().currentDestination?.id == R.id.thirdPartySipAccountLoginFragment) {
                    findNavController().navigate(R.id.action_global_permissionsFragment)
                }
            }
        }

        viewModel.accountLoginErrorEvent.observe(viewLifecycleOwner) {
            it.consume { message ->
                (requireActivity() as GenericActivity).showRedToast(
                    message,
                    R.drawable.warning_circle
                )
                if (!Compatibility.isAccessLocalNetworkPermissionGranted(requireContext())) {
                    Log.w("$TAG Asking for ACCESS_LOCAL_NETWORK permission")
                    // accessLocalNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                }
            }
        }

        viewModel.defaultTransportIndexEvent.observe(viewLifecycleOwner) {
            it.consume { index ->
                binding.transport.setSelection(index)
            }
        }

        coreContext.bearerAuthenticationRequestedEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val serverUrl = pair.first
                val username = pair.second

                Log.i(
                    "$TAG Bearer auth request, navigating to Single Sign On Fragment with server URL [$serverUrl] and username [$username]"
                )
                val intent = Intent(requireContext(), SingleSignOnActivity::class.java)
                intent.putExtra(SingleSignOnActivity.INTENT_EXTRA_USERNAME, username)
                intent.putExtra(SingleSignOnActivity.INTENT_EXTRA_SERVER_URL, serverUrl)
                startActivity(intent)
            }
        }

        viewModel.countryDialPlans.observe(viewLifecycleOwner) { plans ->
            val labels = listOf("") + plans.map {
                "${it.flag} ${it.country} | +${it.countryCallingCode}"
            }
            val countryAdapter = object : ArrayAdapter<String>(requireContext(), R.layout.drop_down_item, labels) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val selectedView = super.getView(position, convertView, parent)
                    (selectedView as? android.widget.TextView)?.text =
                        plans.getOrNull(position - 1)?.let { "+${it.countryCallingCode}" }.orEmpty()
                    return selectedView
                }
            }
            countryAdapter.setDropDownViewResource(R.layout.assistant_country_picker_dropdown_cell)
            binding.countryCode.adapter = countryAdapter
            val selected = plans.indexOfFirst {
                it.isoCountryCode == viewModel.internationalPrefixIsoCountryCode.value
            } + 1
            binding.countryCode.setSelection(selected)
            binding.countryCode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val plan = plans.getOrNull(position - 1)
                    viewModel.internationalPrefix.value = plan?.countryCallingCode.orEmpty()
                    viewModel.internationalPrefixIsoCountryCode.value = plan?.isoCountryCode.orEmpty()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) { }
            }
        }
    }
}
