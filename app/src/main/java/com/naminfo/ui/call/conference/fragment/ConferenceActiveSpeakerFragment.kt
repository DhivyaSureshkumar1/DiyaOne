package com.naminfo.ui.call.conference.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import org.linphone.core.tools.Log
import com.naminfo.databinding.CallConferenceActiveSpeakerFragmentBinding
import com.naminfo.ui.call.conference.viewmodel.ConferenceViewModel
import com.naminfo.ui.call.fragment.GenericCallFragment
import com.naminfo.ui.call.viewmodel.CurrentCallViewModel

@UiThread
class ConferenceActiveSpeakerFragment : GenericCallFragment() {
    companion object {
        private const val TAG = "[Conference Active Speaker Fragment]"
    }

    private lateinit var binding: CallConferenceActiveSpeakerFragmentBinding

    private lateinit var callViewModel: CurrentCallViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CallConferenceActiveSpeakerFragmentBinding.inflate(layoutInflater)
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
                ConferenceViewModel.GRID_LAYOUT -> {
                    Log.i(
                        "$TAG Conference layout changed to mosaic, navigating to matching fragment"
                    )
                    if (findNavController().currentDestination?.id == R.id.conferenceActiveSpeakerFragment) {
                        findNavController().navigate(
                            R.id.action_conferenceActiveSpeakerFragment_to_conferenceGridFragment
                        )
                    }
                }
                ConferenceViewModel.AUDIO_ONLY_LAYOUT -> {
                    Log.i(
                        "$TAG Conference layout changed to audio only, navigating to matching fragment"
                    )
                    if (findNavController().currentDestination?.id == R.id.conferenceActiveSpeakerFragment) {
                        findNavController().navigate(
                            R.id.action_conferenceActiveSpeakerFragment_to_conferenceAudioOnlyFragment
                        )
                    }
                }
                else -> {
                }
            }
        }

        coreContext.postOnCoreThread { core ->
            Log.i("$TAG Setting native video window ID")
            core.nativeVideoWindowId = binding.activeSpeakerSurface
        }
    }
}
