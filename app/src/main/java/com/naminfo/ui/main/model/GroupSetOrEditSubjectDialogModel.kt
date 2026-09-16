package com.naminfo.ui.main.model

import androidx.annotation.UiThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.naminfo.utils.Event

class GroupSetOrEditSubjectDialogModel
    @UiThread
    constructor(
    initialSubject: String,
    val isGroupConversation: Boolean
) {
    val isEdit = initialSubject.isNotEmpty()

    val subject = MutableLiveData<String>()

    val dismissEvent = MutableLiveData<Event<Boolean>>()

    val confirmEvent = MutableLiveData<Event<String>>()

    val emptySubject = MediatorLiveData<Boolean>()

    init {
        emptySubject.addSource(subject) { subject ->
            emptySubject.value = subject.isEmpty()
        }
        subject.value = initialSubject
    }

    @UiThread
    fun dismiss() {
        dismissEvent.value = Event(true)
    }

    @UiThread
    fun confirm() {
        val newSubject = subject.value.orEmpty()
        emptySubject.value = newSubject.isEmpty()
        confirmEvent.value = Event(newSubject)
    }
}
