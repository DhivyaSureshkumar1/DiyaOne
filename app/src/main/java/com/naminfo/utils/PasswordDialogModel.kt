package com.naminfo.utils

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData

class PasswordDialogModel
    @UiThread
    constructor(text: String = "") {
    val message = MutableLiveData<String>()

    val password = MutableLiveData<String>()

    val showPassword = MutableLiveData<Boolean>()

    val dismissEvent = MutableLiveData<Event<Boolean>>()

    val confirmEvent = MutableLiveData<Event<String>>()

    init {
        showPassword.value = false
        message.value = text
    }

    @UiThread
    fun toggleShowPassword() {
        showPassword.value = showPassword.value == false
    }

    @UiThread
    fun dismiss() {
        dismissEvent.value = Event(true)
    }

    @UiThread
    fun confirm() {
        confirmEvent.value = Event(password.value.orEmpty())
    }
}
