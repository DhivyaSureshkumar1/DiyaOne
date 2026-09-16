package com.naminfo.utils

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData

class ConfirmationDialogModel
    @UiThread
    constructor(text: String = "") {
    val message = MutableLiveData<String>()

    val doNotShowAnymore = MutableLiveData<Boolean>()

    val dismissEvent = MutableLiveData<Event<Boolean>>()

    val alternativeChoiceEvent = MutableLiveData<Event<Boolean>>()

    val confirmEvent = MutableLiveData<Event<Boolean>>()

    init {
        message.value = text
    }

    @UiThread
    fun dismiss() {
        dismissEvent.value = Event(true)
    }

    @UiThread
    fun alternativeChoice() {
        alternativeChoiceEvent.value = Event(true)
    }

    @UiThread
    fun confirm() {
        confirmEvent.value = Event(true)
    }
}
