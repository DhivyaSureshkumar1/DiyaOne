package com.naminfo.ui.call.conference.model

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.Participant
import org.linphone.core.tools.Log
import com.naminfo.ui.main.contacts.model.ContactAvatarModel

class ConferenceParticipantModel
    @WorkerThread
    constructor(
    val participant: Participant,
    val avatarModel: ContactAvatarModel,
    isMyselfAdmin: Boolean,
    val isMyself: Boolean,
    private val removeFromConference: ((participant: Participant) -> Unit)?,
    private val changeAdminStatus: ((participant: Participant, setAdmin: Boolean) -> Unit)?
) {
    companion object {
        private const val TAG = "[Conference Participant Model]"
    }

    val sipUri = participant.address.asStringUriOnly()

    val isAdmin = MutableLiveData<Boolean>()

    val isMeAdmin = MutableLiveData<Boolean>()

    init {
        isAdmin.postValue(participant.isAdmin)
        isMeAdmin.postValue(isMyselfAdmin)
    }

    @UiThread
    fun removeParticipant() {
        Log.w("$TAG Removing participant from conference")
        coreContext.postOnCoreThread {
            removeFromConference?.invoke(participant)
        }
    }

    @UiThread
    fun toggleAdminStatus() {
        val newStatus = isAdmin.value == false
        Log.w(
            "$TAG Changing participant admin status to ${if (newStatus) "admin" else "not admin"}"
        )
        isAdmin.postValue(newStatus)

        coreContext.postOnCoreThread {
            changeAdminStatus?.invoke(participant, newStatus)
        }
    }
}
