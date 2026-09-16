package com.naminfo.ui.call.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.linphone.core.tools.Log
import com.naminfo.databinding.CallEndedFragmentBinding
import com.naminfo.ui.call.CallActivity
import com.naminfo.ui.call.viewmodel.CurrentCallViewModel

@UiThread
class EndedCallFragment : GenericCallFragment() {
    companion object {
        private const val TAG = "[Ended Call Fragment]"

        private const val LOCALLY_TERMINATED_CALL_TIMEOUT: Long = 1000
        private const val REMOTELY_TERMINATED_CALL_TIMEOUT: Long = 2000
    }

    private lateinit var binding: CallEndedFragmentBinding

    private lateinit var callViewModel: CurrentCallViewModel

    private var finishScheduled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CallEndedFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Disable back gesture / button
        requireActivity().onBackPressedDispatcher.addCallback { }

        callViewModel = requireActivity().run {
            ViewModelProvider(this)[CurrentCallViewModel::class.java]
        }
        observeToastEvents(callViewModel)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = callViewModel

        Log.i("$TAG Showing ended call fragment")
    }

    override fun onResume() {
        super.onResume()

        if (finishScheduled) return
        finishScheduled = true

        lifecycleScope.launch {
            if (callViewModel.terminatedByUser) {
                Log.i(
                    "$TAG Call terminated by user, waiting 1 second before finishing activity"
                )
                delay(LOCALLY_TERMINATED_CALL_TIMEOUT)
            } else {
                Log.i(
                    "$TAG Call terminated by remote end, waiting 2 seconds before finishing activity"
                )
                delay(REMOTELY_TERMINATED_CALL_TIMEOUT)
            }

            Log.i("$TAG Returning to main activity")
            (activity as? CallActivity)?.returnToMainActivityAfterCall()
        }
    }
}
