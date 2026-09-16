package com.naminfo.ui.call.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.core.view.doOnLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.Call
import org.linphone.core.tools.Log
import com.naminfo.databinding.CallOutgoingFragmentBinding
import com.naminfo.ui.call.viewmodel.CurrentCallViewModel
import com.naminfo.utils.LinphoneUtils

@UiThread
class OutgoingCallFragment : GenericCallFragment() {
    companion object {
        private const val TAG = "[Outgoing Call Fragment]"
    }

    private lateinit var binding: CallOutgoingFragmentBinding

    private lateinit var callViewModel: CurrentCallViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CallOutgoingFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callViewModel = requireActivity().run {
            ViewModelProvider(this)[CurrentCallViewModel::class.java]
        }
        observeToastEvents(callViewModel)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = callViewModel
        binding.numpadModel = callViewModel.numpadModel

        callViewModel.isOutgoingEarlyMedia.observe(viewLifecycleOwner) { earlyMedia ->
            if (earlyMedia) {
                coreContext.postOnCoreThread { core ->
                    val call = core.calls.find {
                        it.state == Call.State.OutgoingEarlyMedia
                    }
                    if (call != null && LinphoneUtils.isVideoEnabled(call)) {
                        Log.i("$TAG Outgoing early-media call with video, setting preview surface")
                        core.nativePreviewWindowId = binding.localPreviewVideoSurface
                    }
                }
            }
        }

        val numpadBottomSheetBehavior = BottomSheetBehavior.from(binding.callNumpad.root)
        numpadBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        numpadBottomSheetBehavior.skipCollapsed = true

        callViewModel.showNumpadBottomSheetEvent.observe(viewLifecycleOwner) {
            it.consume {
                numpadBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    override fun onResume() {
        super.onResume()

        (binding.root as? ViewGroup)?.doOnLayout {
            setupVideoPreview(binding.localPreviewVideoSurface)
        }
    }

    override fun onPause() {
        super.onPause()

        cleanVideoPreview(binding.localPreviewVideoSurface)
    }
}
