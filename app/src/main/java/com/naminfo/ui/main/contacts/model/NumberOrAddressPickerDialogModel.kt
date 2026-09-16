package com.naminfo.ui.main.contacts.model

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.utils.Event

class NumberOrAddressPickerDialogModel
    @UiThread
    constructor(
    list: List<ContactNumberOrAddressModel>
) {
    val sipAddressesAndPhoneNumbers = MutableLiveData<List<ContactNumberOrAddressModel>>()

    val dismissEvent = MutableLiveData<Event<Boolean>>()

    init {
        for (model in list) {
            model.setActionDoneCallback {
                dismiss()
            }
        }
        sipAddressesAndPhoneNumbers.value = list
    }

    @UiThread
    fun dismiss() {
        dismissEvent.value = Event(true)
    }
}
