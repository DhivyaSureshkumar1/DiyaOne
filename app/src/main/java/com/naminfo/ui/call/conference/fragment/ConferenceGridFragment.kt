package com.naminfo.ui.call.conference.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.CallConferenceGridFragmentBinding
import com.naminfo.ui.call.conference.viewmodel.ConferenceViewModel
import com.naminfo.ui.call.fragment.GenericCallFragment
import com.naminfo.ui.call.viewmodel.CurrentCallViewModel

@UiThread
class ConferenceGridFragment : GenericCallFragment() {
    companion object {
        private const val TAG = "[Conference Grid Fragment]"
    }

    private lateinit var binding: CallConferenceGridFragmentBinding

    private lateinit var callViewModel: CurrentCallViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CallConferenceGridFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callViewModel = requireActivity().run {
            ViewModelProvider(this)[CurrentCallViewModel::class.java]
        }

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = callViewModel
        binding.conferenceViewModel = callViewModel.conferenceModel

        callViewModel.conferenceModel.conferenceLayout.observe(viewLifecycleOwner) {
            when (it) {
                ConferenceViewModel.ACTIVE_SPEAKER_LAYOUT -> {
                    Log.i(
                        "$TAG Conference layout changed to active speaker, navigating to matching fragment"
                    )
                    if (findNavController().currentDestination?.id == R.id.conferenceGridFragment) {
                        findNavController().navigate(
                            R.id.action_conferenceGridFragment_to_conferenceActiveSpeakerFragment
                        )
                    }
                }
                ConferenceViewModel.AUDIO_ONLY_LAYOUT -> {
                    Log.i(
                        "$TAG Conference layout changed to audio only, navigating to matching fragment"
                    )
                    if (findNavController().currentDestination?.id == R.id.conferenceGridFragment) {
                        findNavController().navigate(
                            R.id.action_conferenceGridFragment_to_conferenceAudioOnlyFragment
                        )
                    }
                }
                else -> {
                }
            }
        }
    }
}
