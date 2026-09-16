package com.naminfo.ui.main.settings.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import org.linphone.core.tools.Log
import com.naminfo.databinding.SettingsContactsCarddavBinding
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.ui.main.settings.viewmodel.CardDavViewModel

@UiThread
class CardDavAddressBookConfigurationFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[CardDAV Address Book Configuration Fragment]"
    }

    private lateinit var binding: SettingsContactsCarddavBinding

    private lateinit var viewModel: CardDavViewModel

    private val args: CardDavAddressBookConfigurationFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsContactsCarddavBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CardDavViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        val friendListDisplayName = args.displayName
        if (friendListDisplayName != null) {
            Log.i("$TAG Found display name in arguments, loading friends list values")
            viewModel.loadFriendList(friendListDisplayName)
        } else {
            Log.i("$TAG No display name found in arguments, starting from scratch")
        }

        binding.setBackClickListener {
            goBack()
        }

        viewModel.syncSuccessfulEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG Sync successful, going back")
                goBack()
            }
        }

        viewModel.friendListRemovedEvent.observe(viewLifecycleOwner) {
            it.consume {
                Log.i("$TAG CardDAV account removed, going back")
                goBack()
            }
        }
    }
}
