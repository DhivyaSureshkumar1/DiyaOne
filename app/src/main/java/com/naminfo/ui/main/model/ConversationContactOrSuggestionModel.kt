package com.naminfo.ui.main.model

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.corePreferences
import org.linphone.core.Address
import org.linphone.core.Friend
import com.naminfo.ui.main.contacts.model.ContactAvatarModel
import com.naminfo.utils.AppUtils
import com.naminfo.utils.LinphoneUtils

class ConversationContactOrSuggestionModel
    @WorkerThread
    constructor(
    val address: Address,
    val conversationId: String = "",
    conversationSubject: String? = null,
    val friend: Friend? = null,
    val defaultAccountDomain: String? = null,
    private val onClicked: ((Address) -> Unit)? = null
) {
    val id = friend?.refKey ?: address.asStringUriOnly().hashCode()

    val isFriend = friend != null

    val starred = friend?.starred == true

    val name = conversationSubject
        ?: if (friend != null) {
            friend.name ?: LinphoneUtils.getDisplayName(address)
        } else {
            address.username ?: address.domain.orEmpty()
        }

    val sipUri = if (!corePreferences.hideSipAddresses) {
        // Hide SIP address and only show username for suggestions
        // on the same domain as the currently selected account
        if (!defaultAccountDomain.isNullOrEmpty() && defaultAccountDomain == address.domain) {
            address.username
        } else {
            address.asStringUriOnly()
        }
    } else {
        address.username
    }

    val initials = AppUtils.getInitials(conversationSubject ?: name)

    val avatarModel = MutableLiveData<ContactAvatarModel>()

    val selected = MutableLiveData<Boolean>()

    @UiThread
    fun onClicked() {
        onClicked?.invoke(address)
    }
}
