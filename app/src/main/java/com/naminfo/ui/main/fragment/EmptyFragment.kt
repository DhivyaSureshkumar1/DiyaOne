package com.naminfo.ui.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.naminfo.databinding.EmptyFragmentBinding
import com.naminfo.ui.GenericFragment
import com.naminfo.ui.main.viewmodel.SharedMainViewModel
import com.naminfo.utils.Event

@UiThread
class EmptyFragment : GenericFragment() {
    private lateinit var binding: EmptyFragmentBinding

    private lateinit var sharedViewModel: SharedMainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = EmptyFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        sharedViewModel = requireActivity().run {
            ViewModelProvider(this)[SharedMainViewModel::class.java]
        }
    }

    override fun onResume() {
        super.onResume()

        findNavController().popBackStack()

        // This should prevent empty fragment from staying visible
        // after the device rotated if user touched the empty fragment on the right
        sharedViewModel.closeSlidingPaneEvent.postValue(Event(true))
    }
}
