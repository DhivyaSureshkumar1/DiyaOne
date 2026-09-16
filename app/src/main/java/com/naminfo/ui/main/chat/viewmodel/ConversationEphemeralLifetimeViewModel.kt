package com.naminfo.ui.main.chat.viewmodel

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.ui.GenericViewModel

class ConversationEphemeralLifetimeViewModel
    @UiThread
    constructor() : GenericViewModel() {
    val currentlySelectedValue = MutableLiveData<Long>()

    init {
        currentlySelectedValue.value = 0
    }

    @UiThread
    fun onValueSelected(value: Long) {
        currentlySelectedValue.value = value
    }
}
