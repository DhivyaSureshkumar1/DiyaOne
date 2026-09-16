package com.naminfo.ui.main.meetings.model

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.naminfo.DiyaOneApplication.Companion.coreContext
import org.linphone.core.ConferenceInfo
import org.linphone.core.Participant
import org.linphone.core.tools.Log
import com.naminfo.utils.TimestampUtils

class MeetingModel
    @WorkerThread
    constructor(val conferenceInfo: ConferenceInfo) {
    companion object {
        private const val TAG = "[Meeting Model]"
    }

    val id = conferenceInfo.uri?.asStringUriOnly() ?: ""

    val timestamp = conferenceInfo.dateTime

    val day = TimestampUtils.dayOfWeek(timestamp)

    val dayNumber = TimestampUtils.dayOfMonth(timestamp)

    val month = TimestampUtils.month(timestamp)

    val isToday = TimestampUtils.isToday(timestamp)

    val isAfterToday = TimestampUtils.isAfterToday(timestamp)

    val hasNotStartedYet = timestamp * 1000 > System.currentTimeMillis()

    private val startTime = TimestampUtils.timeToString(timestamp)

    private val endTime = TimestampUtils.timeToString(timestamp + (conferenceInfo.duration * 60))

    val time = "$startTime - $endTime"

    val isMyselfOrganizer = isOrganizer()

    val isBroadcast = MutableLiveData<Boolean>()

    val subject = MutableLiveData<String>()

    val firstMeetingOfTheDay = MutableLiveData<Boolean>()

    val weekLabel = TimestampUtils.firstAndLastDayOfWeek(timestamp)

    val firstMeetingOfTheWeek = MutableLiveData<Boolean>()

    val isCancelled = conferenceInfo.state == ConferenceInfo.State.Cancelled

    init {
        subject.postValue(conferenceInfo.subject)

        var allSpeaker = true
        for (participant in conferenceInfo.participantInfos) {
            if (participant.role == Participant.Role.Listener) {
                allSpeaker = false
                break
            }
        }

        isBroadcast.postValue(!allSpeaker)
    }

    @UiThread
    fun delete() {
        coreContext.postOnCoreThread { core ->
            Log.w("$TAG Deleting conference info [${conferenceInfo.uri?.asStringUriOnly()}]")
            core.deleteConferenceInformation(conferenceInfo)
        }
    }

    @WorkerThread
    private fun isOrganizer(): Boolean {
        return coreContext.core.accountList.find { account ->
            val address = account.params.identityAddress
            address != null && conferenceInfo.organizer?.weakEqual(address) == true
        } != null
    }
}
