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
import com.naminfo.databinding.CallConferenceAudioOnlyFragmentBinding
import com.naminfo.ui.call.conference.viewmodel.ConferenceViewModel
import com.naminfo.ui.call.fragment.GenericCallFragment
import com.naminfo.ui.call.viewmodel.CurrentCallViewModel

@UiThread
class ConferenceAudioOnlyFragment : GenericCallFragment() {
    companion object {
        private const val TAG = "[Conference Audio Only Fragment]"
    }

    private lateinit var binding: CallConferenceAudioOnlyFragmentBinding

    private lateinit var callViewModel: CurrentCallViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CallConferenceAudioOnlyFragmentBinding.inflate(layoutInflater)
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
                    if (findNavController().currentDestination?.id == R.id.conferenceAudioOnlyFragment) {
                        findNavController().navigate(
                            R.id.action_conferenceAudioOnlyFragment_to_conferenceActiveSpeakerFragment
                        )
                    }
                }
                ConferenceViewModel.GRID_LAYOUT -> {
                    Log.i(
                        "$TAG Conference layout changed to mosaic, navigating to matching fragment"
                    )
                    if (findNavController().currentDestination?.id == R.id.conferenceAudioOnlyFragment) {
                        findNavController().navigate(
                            R.id.action_conferenceAudioOnlyFragment_to_conferenceGridFragment
                        )
                    }
                }
                else -> {
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Log.i("$TAG Making sure we are not in full-screen mode")
        callViewModel.fullScreenMode.value = false
    }
}
