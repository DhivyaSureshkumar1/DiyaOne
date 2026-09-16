package com.naminfo.ui.main.chat.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.linphone.core.tools.Log
import com.naminfo.databinding.ChatEphemeralLifetimeFragmentBinding
import com.naminfo.ui.main.chat.viewmodel.ConversationEphemeralLifetimeViewModel
import com.naminfo.ui.main.fragment.SlidingPaneChildFragment
import com.naminfo.utils.Event

@UiThread
class ConversationEphemeralLifetimeFragment : SlidingPaneChildFragment() {
    companion object {
        private const val TAG = "[Conversation Ephemeral Lifetime Fragment]"
    }

    private lateinit var binding: ChatEphemeralLifetimeFragmentBinding

    private lateinit var viewModel: ConversationEphemeralLifetimeViewModel

    private val args: ConversationEphemeralLifetimeFragmentArgs by navArgs()

    override fun goBack(): Boolean {
        try {
            return findNavController().popBackStack()
        } catch (ise: IllegalStateException) {
            Log.e("$TAG Can't go back popping back stack: $ise")
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ChatEphemeralLifetimeFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        viewModel = ViewModelProvider(this)[ConversationEphemeralLifetimeViewModel::class.java]
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        val lifetime = args.currentEphemeralLifetime
        Log.i("$TAG Current lifetime for ephemeral messages is [$lifetime]")
        viewModel.currentlySelectedValue.value = lifetime

        binding.setBackClickListener {
            goBack()
        }
    }

    override fun onPause() {
        sharedViewModel.newChatMessageEphemeralLifetimeToSetEvent.value = Event(
            viewModel.currentlySelectedValue.value ?: 0L
        )
        super.onPause()
    }
}
