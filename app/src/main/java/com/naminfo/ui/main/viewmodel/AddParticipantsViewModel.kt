package com.naminfo.ui.main.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.R
import org.linphone.core.Address
import org.linphone.core.Friend
import org.linphone.core.tools.Log
import com.naminfo.ui.main.model.SelectedAddressModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.Event

class AddParticipantsViewModel
    @UiThread
    constructor() : AddressSelectionViewModel() {
    companion object {
        private const val TAG = "[Add Participants ViewModel]"
    }

    val selectedSipUrisEvent = MutableLiveData<Event<ArrayList<String>>>()

    init {
        Log.i("$TAG Forcing multiple selection mode")
        switchToMultipleSelectionMode()
    }

    @WorkerThread
    override fun onSingleAddressSelected(address: Address, friend: Friend?) {
        Log.e("$TAG This shouldn't happen as we should always be in multiple selection mode here!")
    }

    @UiThread
    fun isSelectionEmpty(): Boolean {
        return selection.value.orEmpty().isEmpty()
    }

    @UiThread
    fun addSelectedParticipants(participants: Array<String>) {
        coreContext.postOnCoreThread { core ->
            Log.i("$TAG Adding [${participants.size}] pre-selected participants")
            val list = arrayListOf<SelectedAddressModel>()
            val addresses = arrayListOf<Address>()

            for (uri in participants) {
                val address = core.interpretUrl(uri, false)
                if (address == null) {
                    Log.e("$TAG Failed to parse participant URI [$uri] as address!")
                    continue
                }
                addresses.add(address)

                val avatarModel = coreContext.contactsManager.getContactAvatarModelForAddress(
                    address
                )
                val model = SelectedAddressModel(address, avatarModel) {
                    removeAddressModelFromSelection(it)
                }
                list.add(model)
            }

            selectionCount.postValue(
                AppUtils.getStringWithPlural(
                    R.plurals.selection_count_label,
                    list.size,
                    list.size.toString()
                )
            )
            selection.postValue(list)
            updateSelectedParticipants(addresses)
        }
    }

    @UiThread
    fun addParticipants() {
        val selected = selection.value.orEmpty()
        Log.i("$TAG [${selected.size}] participants selected")

        coreContext.postOnCoreThread {
            val list = arrayListOf<String>()
            for (model in selected) {
                list.add(model.address.asStringUriOnly())
            }

            selectedSipUrisEvent.postValue(Event(list))
        }
    }
}
