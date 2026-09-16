package com.naminfo.ui

import androidx.annotation.UiThread
import androidx.fragment.app.Fragment

@UiThread
abstract class GenericFragment : Fragment() {
    protected fun observeToastEvents(viewModel: GenericViewModel) {
        viewModel.showRedToastEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val message = getString(pair.first)
                val icon = pair.second
                (requireActivity() as GenericActivity).showRedToast(message, icon)
            }
        }

        viewModel.showFormattedRedToastEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val message = pair.first
                val icon = pair.second
                (requireActivity() as GenericActivity).showRedToast(message, icon)
            }
        }

        viewModel.showGreenToastEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val message = getString(pair.first)
                val icon = pair.second
                (requireActivity() as GenericActivity).showGreenToast(message, icon)
            }
        }

        viewModel.showFormattedGreenToastEvent.observe(viewLifecycleOwner) {
            it.consume { pair ->
                val message = pair.first
                val icon = pair.second
                (requireActivity() as GenericActivity).showGreenToast(message, icon)
            }
        }
    }
}
