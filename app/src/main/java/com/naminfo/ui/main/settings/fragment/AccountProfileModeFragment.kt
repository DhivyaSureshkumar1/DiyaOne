package com.naminfo.ui.main.settings.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.navigation.navGraphViewModels
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.AccountProfileSecureModeFragmentBinding
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.ui.main.settings.viewmodel.AccountProfileViewModel

@UiThread
class AccountProfileModeFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[Account Profile Mode Fragment]"
    }

    private lateinit var binding: AccountProfileSecureModeFragmentBinding

    private val viewModel: AccountProfileViewModel by navGraphViewModels(
        R.id.main_nav_graph
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AccountProfileSecureModeFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            Log.i("$TAG Leaving without saving changes...")
            goBack()
        }
    }
}
