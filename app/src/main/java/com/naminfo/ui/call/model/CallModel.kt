package com.naminfo.ui.call.model

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.Call
import org.linphone.core.CallListenerStub
import org.linphone.core.tools.Log
import com.naminfo.ui.main.contacts.model.ContactAvatarModel
import com.naminfo.utils.LinphoneUtils

class CallModel
    @WorkerThread
    constructor(val call: Call) {
    companion object {
        private const val TAG = "[Call Model]"
    }

    val id = call.callLog.callId

    val displayName = MutableLiveData<String>()

    val state = MutableLiveData<String>()

    val isPaused = MutableLiveData<Boolean>()

    val friend = coreContext.contactsManager.findContactByAddress(call.callLog.remoteAddress)

    val contact = MutableLiveData<ContactAvatarModel>()

    private val callListener = object : CallListenerStub() {
        @WorkerThread
        override fun onStateChanged(call: Call, state: Call.State, message: String) {
            this@CallModel.state.postValue(LinphoneUtils.callStateToString(state))
            isPaused.postValue(LinphoneUtils.isCallPaused(state))
        }
    }

    init {
        call.addListener(callListener)

        val conferenceInfo = coreContext.core.findConferenceInformationFromUri(call.remoteAddress)
        val remoteAddress = call.callLog.remoteAddress
        val avatarModel = if (conferenceInfo != null) {
            coreContext.contactsManager.getContactAvatarModelForConferenceInfo(conferenceInfo)
        } else {
            coreContext.contactsManager.getContactAvatarModelForAddress(
                remoteAddress
            )
        }
        contact.postValue(avatarModel)
        displayName.postValue(
            avatarModel.friend.name ?: LinphoneUtils.getDisplayName(remoteAddress)
        )

        state.postValue(LinphoneUtils.callStateToString(call.state))
        isPaused.postValue(LinphoneUtils.isCallPaused(call.state))
    }

    @WorkerThread
    fun destroy() {
        call.removeListener(callListener)
    }

    @WorkerThread
    fun togglePauseResume() {
        when (call.state) {
            Call.State.Paused -> {
                Log.i("$TAG Trying to resume call [${call.remoteAddress.asStringUriOnly()}]")
                call.resume()
            }
            else -> {
                Log.i("$TAG Trying to resume call [${call.remoteAddress.asStringUriOnly()}]")
                call.pause()
            }
        }
    }

    @UiThread
    fun hangUp() {
        coreContext.postOnCoreThread {
            Log.i("$TAG Terminating call [${call.remoteAddress.asStringUriOnly()}]")
            coreContext.terminateCall(call)
        }
    }
}
