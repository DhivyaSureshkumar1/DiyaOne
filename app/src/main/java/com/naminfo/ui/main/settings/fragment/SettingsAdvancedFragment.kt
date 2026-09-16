package com.naminfo.ui.main.settings.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.UiThread
import androidx.lifecycle.ViewModelProvider
import com.naminfo.R
import com.naminfo.databinding.SettingsAdvancedFragmentBinding
import com.naminfo.ui.GenericActivity
import com.naminfo.ui.main.fragment.GenericMainFragment
import com.naminfo.ui.main.settings.viewmodel.SettingsViewModel
import com.naminfo.utils.Event

@UiThread
class SettingsAdvancedFragment : GenericMainFragment() {
    private lateinit var binding: SettingsAdvancedFragmentBinding

    private lateinit var viewModel: SettingsViewModel

    private val inputAudioDeviceDropdownListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            viewModel.setInputAudioDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    private val outputAudioDeviceDropdownListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            viewModel.setOutputAudioDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsAdvancedFragmentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
        observeToastEvents(viewModel)

        binding.setBackClickListener {
            goBack()
        }

        binding.setAndroidSettingsClickListener {
            (requireActivity() as GenericActivity).goToAndroidPermissionSettings()
        }

        viewModel.inputAudioDeviceIndex.observe(viewLifecycleOwner) {
            setupInputAudioDevicePicker()
        }

        viewModel.outputAudioDeviceIndex.observe(viewLifecycleOwner) {
            setupOutputAudioDevicePicker()
        }

        viewModel.keepAliveServiceSettingChangedEvent.observe(viewLifecycleOwner) {
            it.consume {
                sharedViewModel.refreshDrawerMenuQuitButtonEvent.postValue(Event(true))
            }
        }

        startPostponedEnterTransition()
    }

    override fun onPause() {
        viewModel.updateDeviceName()
        viewModel.updateRemoteProvisioningUrl()

        super.onPause()
    }

    private fun setupInputAudioDevicePicker() {
        val index = viewModel.inputAudioDeviceIndex.value ?: 0
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.drop_down_item,
            viewModel.inputAudioDeviceLabels
        )
        adapter.setDropDownViewResource(R.layout.generic_dropdown_cell)
        binding.inputAudioDevice.adapter = adapter
        binding.inputAudioDevice.onItemSelectedListener = inputAudioDeviceDropdownListener
        binding.inputAudioDevice.setSelection(index)
    }

    private fun setupOutputAudioDevicePicker() {
        val index = viewModel.outputAudioDeviceIndex.value ?: 0
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.drop_down_item,
            viewModel.outputAudioDeviceLabels
        )
        adapter.setDropDownViewResource(R.layout.generic_dropdown_cell)
        binding.outputAudioDevice.adapter = adapter
        binding.outputAudioDevice.onItemSelectedListener = outputAudioDeviceDropdownListener
        binding.outputAudioDevice.setSelection(index)
    }
}
