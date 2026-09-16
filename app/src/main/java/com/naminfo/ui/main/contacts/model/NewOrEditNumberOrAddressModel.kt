package com.naminfo.ui.main.contacts.model

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData

class NewOrEditNumberOrAddressModel
    @WorkerThread
    constructor(
    defaultValue: String,
    val isSip: Boolean,
    val label: String? = "",
    private val onValueNoLongerEmpty: (() -> Unit)? = null,
    private val onRemove: ((model: NewOrEditNumberOrAddressModel) -> Unit)? = null
) {
    val value = MutableLiveData<String>()

    val showRemoveButton = MutableLiveData<Boolean>()

    init {
        value.postValue(defaultValue)
        showRemoveButton.postValue(defaultValue.isNotEmpty())
    }

    @UiThread
    fun onValueChanged(newValue: String) {
        if (newValue.isNotEmpty() && showRemoveButton.value == false) {
            onValueNoLongerEmpty?.invoke()
            showRemoveButton.value = true
        }
    }

    @UiThread
    fun remove() {
        onRemove?.invoke(this)
    }
}
