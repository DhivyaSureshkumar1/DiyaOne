package com.naminfo.ui.main.meetings.model

import androidx.annotation.WorkerThread
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.Address

class ParticipantModel
    @WorkerThread
    constructor(address: Address, val isOrganizer: Boolean) {
    val sipUri = address.asStringUriOnly()

    val avatarModel = coreContext.contactsManager.getContactAvatarModelForAddress(address)
}
