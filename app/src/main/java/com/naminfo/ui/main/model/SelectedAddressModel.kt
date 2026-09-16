package com.naminfo.ui.main.model

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import org.linphone.core.Address
import com.naminfo.ui.main.contacts.model.ContactAvatarModel

class SelectedAddressModel
    @WorkerThread
    constructor(
    val address: Address,
    val avatarModel: ContactAvatarModel?,
    private val onRemovedFromSelection: ((model: SelectedAddressModel) -> Unit)? = null
) {
    @UiThread
    fun removeFromSelection() {
        onRemovedFromSelection?.invoke(this)
    }
}
