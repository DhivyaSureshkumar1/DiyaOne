package com.naminfo.contacts

import androidx.lifecycle.MutableLiveData
import org.linphone.core.ConsolidatedPresence
import org.linphone.core.SecurityLevel

abstract class AbstractAvatarModel {
    val trust = MutableLiveData<SecurityLevel>()

    val showTrust = MutableLiveData<Boolean>()

    val initials = MutableLiveData<String>()

    val picturePath = MutableLiveData<String>()

    val forceConversationIcon = MutableLiveData<Boolean>()

    val forceConferenceIcon = MutableLiveData<Boolean>()

    val defaultToConversationIcon = MutableLiveData<Boolean>()

    val defaultToConferenceIcon = MutableLiveData<Boolean>()

    val skipInitials = MutableLiveData<Boolean>()

    val presenceStatus = MutableLiveData<ConsolidatedPresence>()
}
