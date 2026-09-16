package com.naminfo.ui.main.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.annotation.UiThread
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naminfo.ui.main.adapter.ConversationsContactsAndSuggestionsListAdapter
import com.naminfo.ui.main.contacts.model.ContactNumberOrAddressModel
import com.naminfo.ui.main.contacts.model.NumberOrAddressPickerDialogModel
import com.naminfo.ui.main.viewmodel.AddressSelectionViewModel
import com.naminfo.utils.DialogUtils
import com.naminfo.utils.RecyclerViewHeaderDecoration

@UiThread
abstract class GenericAddressPickerFragment : GenericMainFragment() {
    companion object {
        private const val TAG = "[Generic Address Picker Fragment]"
    }

    private var numberOrAddressPickerDialog: Dialog? = null

    protected lateinit var adapter: ConversationsContactsAndSuggestionsListAdapter

    protected abstract val viewModel: AddressSelectionViewModel

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ConversationsContactsAndSuggestionsListAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.onClickedEvent.observe(viewLifecycleOwner) {
            it.consume { model ->
                viewModel.handleClickOnContactModel(model)
            }
        }

        viewModel.searchFilter.observe(viewLifecycleOwner) { filter ->
            val trimmed = filter.trim()
            viewModel.applyFilter(trimmed)
        }

        viewModel.showNumberOrAddressPickerDialogEvent.observe(viewLifecycleOwner) {
            it.consume { list ->
                showNumbersOrAddressesDialog(list)
            }
        }

        viewModel.dismissNumberOrAddressPickerDialogEvent.observe(viewLifecycleOwner) {
            it.consume {
                numberOrAddressPickerDialog?.dismiss()
                numberOrAddressPickerDialog = null
            }
        }
    }

    override fun onPause() {
        super.onPause()

        numberOrAddressPickerDialog?.dismiss()
        numberOrAddressPickerDialog = null
    }

    protected fun setupRecyclerView(view: RecyclerView) {
        recyclerView = view
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), adapter)
        recyclerView.addItemDecoration(headerItemDecoration)
    }

    protected fun attachAdapter() {
        if (::recyclerView.isInitialized) {
            if (recyclerView.adapter != adapter) {
                recyclerView.adapter = adapter
            }
        }
    }

    private fun showNumbersOrAddressesDialog(list: List<ContactNumberOrAddressModel>) {
        val numberOrAddressModel = NumberOrAddressPickerDialogModel(list)
        val dialog =
            DialogUtils.getNumberOrAddressPickerDialog(
                requireActivity(),
                numberOrAddressModel
            )
        numberOrAddressPickerDialog = dialog

        numberOrAddressModel.dismissEvent.observe(viewLifecycleOwner) { event ->
            event.consume {
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
