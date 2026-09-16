package com.naminfo.ui.main.chat.model

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import org.linphone.core.Address

class MessageBottomSheetParticipantModel
    @WorkerThread
    constructor(
    address: Address,
    val value: String,
    val timestamp: Long,
    val isOurOwnReaction: Boolean = false,
    val onClick: (() -> Unit)? = null
) {
    val sipUri = address.asStringUriOnly()

    val showSipUri = MutableLiveData<Boolean>()

    val avatarModel = coreContext.contactsManager.getContactAvatarModelForAddress(address)

    init {
        showSipUri.postValue(false)
    }

    @UiThread
    fun clicked() {
        if (!isOurOwnReaction && !corePreferences.onlyDisplaySipUriUsername && !corePreferences.hideSipAddresses) {
            showSipUri.postValue(showSipUri.value == false)
        } else {
            onClick?.invoke()
        }
    }
}
