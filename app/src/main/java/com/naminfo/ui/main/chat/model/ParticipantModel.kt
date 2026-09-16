package com.naminfo.ui.main.chat.model

import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import org.linphone.core.Address
import com.naminfo.ui.main.contacts.model.ContactAvatarModel

class ParticipantModel
    @WorkerThread
    constructor(
    val address: Address,
    val isMyselfAdmin: Boolean = false,
    val isParticipantAdmin: Boolean = false,
    val showMenu: Boolean = false,
    val isParticipantMyself: Boolean = false,
    private val onClicked: ((model: ParticipantModel) -> Unit)? = null,
    private val onMenuClicked: ((view: View, model: ParticipantModel) -> Unit)? = null
) {
    val sipUri = address.asStringUriOnly()

    val showSipUri = MutableLiveData<Boolean>()

    val avatarModel: ContactAvatarModel = coreContext.contactsManager.getContactAvatarModelForAddress(
        address
    )

    val refKey: String = avatarModel.friend.refKey.orEmpty()

    val friendAvailable: Boolean = coreContext.contactsManager.isContactAvailable(
        avatarModel.friend
    )

    init {
        showSipUri.postValue(false)
    }

    @UiThread
    fun onClicked() {
        if (onClicked == null && !corePreferences.onlyDisplaySipUriUsername && !corePreferences.hideSipAddresses) {
            showSipUri.postValue(showSipUri.value == false)
        } else {
            onClicked?.invoke(this)
        }
    }

    @UiThread
    fun openMenu(view: View) {
        onMenuClicked?.invoke(view, this)
    }
}
